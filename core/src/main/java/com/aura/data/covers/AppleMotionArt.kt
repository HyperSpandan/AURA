package com.aura.data.covers

import com.aura.data.DebugLog as Log
import com.aura.data.Http
import com.aura.data.metadata.AppleTrackMetadataRepository
import com.aura.data.metadata.AppleWebToken
import com.aura.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Apple Music's animated album covers: the looping clips Apple shows on
 * album pages, `editorialVideo` in its catalogue. The album comes from the
 * same strict match as the sharp cover ([AppleTrackMetadataRepository.release]:
 * same title, artist and length within 3 s), so a clip is only ever shown
 * for the album the song is really on. Each is an HLS stream.
 */
object AppleMotionArt {
    private const val TAG = "AppleMotionArt"
    private const val AMP = "https://amp-api.music.apple.com/v1/catalog"
    private const val MISS_TTL_MS = 6 * 60 * 60_000L

    /** An HLS playlist: the square clip first, the next one as the retry, plus tall format if available. */
    data class Motion(val url: String, val fallbackUrl: String?, val tallUrl: String? = null)

    private data class Entry(val motion: Motion?, val at: Long)

    private val json = Json { ignoreUnknownKeys = true }
    private val cache = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > 300
    }
    private val albumCache = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > 300
    }

    /** Cached motion for [browseId] if already queried. */
    fun cachedAlbum(browseId: String): Motion? = synchronized(albumCache) { albumCache[browseId]?.motion }

    /** The animated cover of the album [song] is on, or null when Apple has none or the match isn't certain. */
    suspend fun forSong(song: Song, durationMs: Long): Motion? {
        val singleRelease = runCatching { AppleTrackMetadataRepository.release(song, durationMs) }
            .onFailure { Log.w(TAG, "catalogue match failed", it) }.getOrNull()
        val candidates = if (singleRelease != null) {
            listOf(singleRelease)
        } else {
            runCatching { AppleTrackMetadataRepository.candidateReleases(song, durationMs) }
                .onFailure { Log.w(TAG, "candidate releases failed", it) }.getOrNull().orEmpty()
        }
        Log.i(TAG, "forSong: ${song.title} (${song.artist}) [album=${song.albumName}, len=$durationMs] -> single=$singleRelease, cands=${candidates.size}")
        if (candidates.isEmpty()) return null

        for (release in candidates) {
            val key = "${release.storefront}/${release.albumId}"
            val cached = synchronized(cache) { cache[key] }
            if (cached != null) {
                if (cached.motion != null) return cached.motion
                if (System.currentTimeMillis() - cached.at < MISS_TTL_MS) continue
            }
            val url = "$AMP/${release.storefront}/albums/${release.albumId}".toHttpUrl().newBuilder()
                .addQueryParameter("extend", "editorialVideo").build().toString()
            val body = runCatching { read(url) }.onFailure { Log.w(TAG, "album read failed for ${release.albumId}", it) }.getOrNull() ?: continue
            val motion = motionOf(json.parseToJsonElement(body).jsonObject)
            synchronized(cache) { cache[key] = Entry(motion, System.currentTimeMillis()) }
            if (motion != null) {
                Log.i(TAG, "forSong: ${song.title} motion found on ${release.album} (${release.albumId}): ${motion.url}")
                return motion
            }
        }
        return null
    }

    /** One catalogue read, with a fresh token once if Apple refuses the one held. */
    private suspend fun read(url: String): String? {
        repeat(2) {
            val bearer = AppleWebToken.get() ?: return null
            val (code, body) = runInterruptible(Dispatchers.IO) {
                Http.client.newCall(AppleWebToken.request(url, bearer)).execute().use { it.code to it.body?.string() }
            }
            when {
                code == 401 || code == 403 -> AppleWebToken.forget()
                code == 404 -> return null
                code in 200..299 -> return body
                else -> throw java.io.IOException("Apple answered $code")
            }
        }
        return null
    }

    /** The clip on an album read (`data[0].attributes.editorialVideo`). */
    internal fun motionOf(response: JsonObject): Motion? {
        val album = (response["data"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        val attributes = album["attributes"] as? JsonObject ?: return null
        return motionUrls(attributes["editorialVideo"] as? JsonObject)
    }

    /**
     * The square clip first: it fills the square cover without cropping. The
     * tall one is the same clip framed for a phone and only worth having as
     * the retry. Only Apple's own https hosts are taken.
     */
    internal fun motionUrls(video: JsonObject?): Motion? {
        video ?: return null
        fun link(key: String): String? {
            val asset = video[key] as? JsonObject ?: return null
            return listOf("video", "videoUrl", "hlsUrl", "url").firstNotNullOfOrNull { k ->
                (asset[k] as? JsonPrimitive)?.contentOrNull?.toHttpUrlOrNull()
                    ?.takeIf { it.isHttps && (it.host == "apple.com" || it.host.endsWith(".apple.com")) }?.toString()
            }
        }
        val square = link("motionDetailSquare") ?: link("motionSquareVideo1x1")
        val raw = link("motionDetailRaw")
        val tall = link("motionDetailTall") ?: link("motionTallVideo3x4")
        val url = square ?: raw ?: tall ?: return null
        val fallback = listOf(square, raw, tall).firstOrNull { it != null && it != url }
        return Motion(url, fallback, tallUrl = tall)
    }

    /** The animated motion cover for an album, or null when Apple has none. */
    suspend fun forAlbum(collection: com.aura.data.Collection): Motion? {
        if (collection.browseId.isNotBlank()) {
            val cached = synchronized(albumCache) { albumCache[collection.browseId] }
            if (cached != null) {
                if (cached.motion != null) return cached.motion
                if (System.currentTimeMillis() - cached.at < MISS_TTL_MS) return null
            }
        }
        val songs = collection.songs.filter { s ->
            val ms = com.aura.data.reco.Recommendations.durationMs(s.durationText)
            ms in 20_000..1800_000
        }.take(4)
        val artist = collection.subtitle.split("•").first().trim().ifBlank { collection.title }
        val foundMotion: Motion? = if (songs.isEmpty()) {
            val dummySong = Song(
                videoId = collection.browseId,
                title = collection.title,
                artist = artist,
                thumbnailUrl = collection.thumbnailUrl,
                albumName = collection.title,
            )
            forSong(dummySong, 0L)
        } else {
            var m: Motion? = null
            for (song in songs) {
                val songArtist = song.artist.ifBlank { artist }
                val candidate = song.copy(
                    albumName = collection.title,
                    artist = songArtist,
                )
                val durationMs = com.aura.data.reco.Recommendations.durationMs(song.durationText)
                val motion = forSong(candidate, durationMs)
                if (motion != null) {
                    Log.i(TAG, "forAlbum: matched motion for ${collection.title} using track ${song.title} (${motion.tallUrl ?: motion.url})")
                    m = motion
                    break
                }
            }
            m
        }
        if (collection.browseId.isNotBlank()) {
            synchronized(albumCache) { albumCache[collection.browseId] = Entry(foundMotion, System.currentTimeMillis()) }
        }
        return foundMotion
    }
}
