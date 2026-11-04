package com.aura.data.metadata

import com.aura.data.lossless.LosslessHttp
import com.aura.data.model.Song
import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Catalog information only: Apple previews and catalog badges never describe our audio stream. */
data class AppleTrackMetadata(
    val id: Long,
    val artist: String,
    val album: String,
    val releaseDate: String?,
    val genre: String?,
    val explicitness: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val url: String,
)

/**
 * The album a song was matched to in Apple's catalogue: enough to show its
 * sharp cover and to ask for its animated cover. Clean and explicit copies
 * of one album are separate albums to Apple with the same art, so either
 * stands for both.
 */
data class AppleRelease(
    val albumId: Long,
    val album: String,
    /** The cover at Apple's 100px size; any size up to the 3000px master is a change of the last path part. */
    val artworkUrl: String?,
    /** The storefront (lower case) the match came from: the animated cover is asked for there. */
    val storefront: String,
)

object AppleTrackMetadataRepository {
    private data class Entry(val value: AppleTrackMetadata?, val release: AppleRelease?, val coverUrl: String?, val candidateReleases: List<AppleRelease>, val at: Long)
    internal data class Candidate(val track: AppleTrackMetadata, val albumId: Long?, val artworkUrl: String?)
    private val gate = Mutex()
    private var lastRequest = 0L
    private val cache = object : LinkedHashMap<String, Entry>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?) = size > 200
    }

    // Apple's documented Search API needs no developer token. Bound requests and cache both
    // matches and misses; transient failures propagate so the sheet can offer a retry.
    suspend fun lookup(song: Song, durationMs: Long, country: String = Locale.getDefault().country): AppleTrackMetadata? =
        search(song, durationMs, country)?.value

    /** The album [song] is on, for its cover and animated cover; null unless the match is certain. */
    suspend fun release(song: Song, durationMs: Long, country: String = Locale.getDefault().country): AppleRelease? =
        search(song, durationMs, country)?.release

    /**
     * All candidate albums this track matches in Apple's catalogue.
     * Includes studio albums, deluxe editions, and compilations where title, artist,
     * and duration match within 3s.
     */
    suspend fun candidateReleases(song: Song, durationMs: Long, country: String = Locale.getDefault().country): List<AppleRelease> =
        search(song, durationMs, country)?.candidateReleases.orEmpty()

    /** Direct studio album artwork from Apple Music. */
    suspend fun coverArt(song: Song, durationMs: Long, country: String = Locale.getDefault().country): String? =
        search(song, durationMs, country)?.coverUrl

    private suspend fun search(song: Song, durationMs: Long, country: String): Entry? = gate.withLock {
        if (durationMs <= 0 || song.title.isBlank() || song.artist.isBlank()) return@withLock null
        val storefront = country.uppercase(Locale.ROOT).takeIf { it in Locale.getISOCountries() } ?: "US"
        val key = "${song.title}|${song.artist}|${song.albumName}|$durationMs|$storefront"
        val now = System.nanoTime() / 1_000_000
        cache[key]?.takeIf { now - it.at < 86_400_000 }?.let { return@withLock it }
        var track: AppleTrackMetadata? = null
        var release: AppleRelease? = null
        var coverUrl: String? = null
        val candidatesList = mutableListOf<AppleRelease>()
        val cleaned = com.aura.data.lyrics.TrackNameCleaner.clean(song.title, song.artist)
        val query = if (cleaned.title != song.title) "${cleaned.title} ${cleaned.artist}" else "${song.title} ${song.artist}"
        for (store in listOf(storefront, "US").distinct()) {
            val elapsed = System.nanoTime() / 1_000_000 - lastRequest
            if (lastRequest != 0L && elapsed < 400) delay(400 - elapsed)
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("term", query)
                .addQueryParameter("country", store).addQueryParameter("media", "music")
                .addQueryParameter("entity", "song").addQueryParameter("limit", "25").build()
            val response = try { LosslessHttp.text(url.toString()) }
                finally { lastRequest = System.nanoTime() / 1_000_000 }
            val found = candidates(Json.parseToJsonElement(response).jsonObject, song, durationMs)
            track = track ?: single(found)
            release = release ?: releaseOf(found, store, song)
            coverUrl = coverUrl ?: found.firstOrNull { it.artworkUrl != null }?.artworkUrl
            found.filter { it.albumId != null }.forEach {
                candidatesList.add(AppleRelease(it.albumId!!, it.track.album, it.artworkUrl, store.lowercase(Locale.ROOT)))
            }
            // Clean and explicit copies leave the track ambiguous everywhere; the album is then enough.
            if (track != null || release != null || coverUrl != null) break
        }
        val sortedCandidates = candidatesList.distinctBy { it.albumId }.sortedWith(
            compareBy<AppleRelease> { rel ->
                val name = rel.album.lowercase(Locale.ROOT)
                when {
                    name.contains("remix") || name.contains("live") -> 3
                    name.contains("greatest") || name.contains("highlights") || name.contains("best of") -> 2
                    name.contains("deluxe") || name.contains("expanded") || name.contains("edition") -> 1
                    else -> 0
                }
            }.thenBy { it.album.length }
        )
        Entry(track, release, coverUrl, sortedCandidates, System.nanoTime() / 1_000_000).also { cache[key] = it }
    }

    internal fun select(response: JsonObject, song: Song, durationMs: Long): AppleTrackMetadata? =
        single(candidates(response, song, durationMs))

    internal fun selectRelease(response: JsonObject, song: Song, durationMs: Long, storefront: String = "US"): AppleRelease? =
        releaseOf(candidates(response, song, durationMs), storefront, song)

    // A search score alone is insufficient evidence for a different edition or recording.
    private fun single(found: List<Candidate>) = found.distinctBy { it.track.id }.singleOrNull()?.track

    /** Every match on one album (by name): its art and id. Resolves ambiguity between Singles and Studio albums. */
    private fun releaseOf(found: List<Candidate>, storefront: String, song: Song? = null): AppleRelease? {
        if (found.isEmpty()) return null
        val candidatesWithAlbum = found.filter { it.albumId != null }
        if (candidatesWithAlbum.isEmpty()) return null

        // If song explicitly specifies an album name, match it
        val songAlbum = song?.albumName?.takeIf { it.isNotBlank() }?.let { normalAlbum(it) }
        if (songAlbum != null) {
            val matched = candidatesWithAlbum.filter { normalAlbum(it.track.album) == songAlbum }
            if (matched.isNotEmpty()) {
                val pick = matched.first()
                return AppleRelease(pick.albumId!!, pick.track.album, pick.artworkUrl, storefront.lowercase(Locale.ROOT))
            }
            // Prefix match (e.g. songAlbum="sos" matching "sos deluxe lana")
            val prefixMatched = candidatesWithAlbum.filter {
                val candAlb = normalAlbum(it.track.album)
                candAlb.startsWith(songAlbum) || songAlbum.startsWith(candAlb)
            }
            if (prefixMatched.isNotEmpty()) {
                val pick = prefixMatched.minByOrNull { it.track.album.length } ?: prefixMatched.first()
                return AppleRelease(pick.albumId!!, pick.track.album, pick.artworkUrl, storefront.lowercase(Locale.ROOT))
            }
        }

        // If all candidates are on the same album, pick it
        if (candidatesWithAlbum.map { normalAlbum(it.track.album) }.distinct().size == 1) {
            val pick = candidatesWithAlbum.first()
            return AppleRelease(pick.albumId!!, pick.track.album, pick.artworkUrl, storefront.lowercase(Locale.ROOT))
        }

        // When a track appears on both a "- Single" and a studio album, pick the studio album
        // as Apple Music attaches motion art (editorialVideo) to the studio album.
        val withoutSingles = candidatesWithAlbum.filterNot { it.track.album.lowercase(Locale.ROOT).endsWith(" - single") }
        if (withoutSingles.isNotEmpty() && withoutSingles.map { normalAlbum(it.track.album) }.distinct().size == 1) {
            val pick = withoutSingles.first()
            return AppleRelease(pick.albumId!!, pick.track.album, pick.artworkUrl, storefront.lowercase(Locale.ROOT))
        }

        return null
    }

    internal fun candidates(response: JsonObject, song: Song, durationMs: Long): List<Candidate> {
        if (durationMs <= 0 || song.title.isBlank() || song.artist.isBlank()) return emptyList()
        val wantedArtists = artists(song.artist)
        return (response["results"] as? JsonArray).orEmpty().mapNotNull { item ->
            val row = item as? JsonObject ?: return@mapNotNull null
            if (row.text("kind") != "song") return@mapNotNull null
            val title = row.text("trackName") ?: return@mapNotNull null
            val artist = row.text("artistName") ?: return@mapNotNull null
            val album = row.text("collectionName") ?: return@mapNotNull null
            val duration = (row["trackTimeMillis"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            val cleaned = com.aura.data.lyrics.TrackNameCleaner.clean(song.title, song.artist)
            val cleanTitle = normal(cleaned.title)
            val rawTitle = normal(song.title)
            val normalCand = normal(title)
            val titleMatches = normalCand == cleanTitle || normalCand == rawTitle
            if (!titleMatches || duration <= 0 || kotlin.math.abs(duration - durationMs) > 3_000) return@mapNotNull null
            val candArtists = artists(artist)
            val artistMatches = wantedArtists.isEmpty() ||
                candArtists.containsAll(wantedArtists) ||
                wantedArtists.containsAll(candArtists) ||
                candArtists.intersect(wantedArtists).isNotEmpty()
            if (!artistMatches) return@mapNotNull null
            if (!song.albumName.isNullOrBlank() && normalAlbum(album) != normalAlbum(song.albumName)) return@mapNotNull null
            val id = (row["trackId"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 } ?: return@mapNotNull null
            val url = row.text("trackViewUrl")?.toHttpUrlOrNull()?.takeIf {
                it.isHttps && it.host in setOf("music.apple.com", "itunes.apple.com") && it.username.isEmpty() && it.password.isEmpty()
            } ?: return@mapNotNull null
            val track = AppleTrackMetadata(id, artist, album,
                row.text("releaseDate")?.let { runCatching { Instant.parse(it).toString().take(10) }.getOrNull() },
                row.text("primaryGenreName"), row.text("trackExplicitness")?.takeIf { it in setOf("explicit", "cleaned", "notExplicit") },
                row.positiveInt("trackNumber"), row.positiveInt("discNumber"), url.toString())
            Candidate(
                track,
                (row["collectionId"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 },
                row.text("artworkUrl100")?.toHttpUrlOrNull()?.takeIf { it.isHttps && it.host.endsWith(".mzstatic.com") }?.toString(),
            )
        }
    }

    private fun JsonObject.text(key: String) = (get(key) as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.positiveInt(key: String) = (get(key) as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 }
    private fun artists(value: String) = value.split(Regex("[,;&]|\\s+(?:feat\\.|ft\\.)\\s+", RegexOption.IGNORE_CASE)).map(::normal).filter(String::isNotEmpty).toSet()
    private fun normalAlbum(value: String) = normal(value.replace(Regex("(?:\\s*(?:\\(Original (?:Motion Picture )?Soundtrack\\)|- (?:Single|EP)))+$", RegexOption.IGNORE_CASE), ""))
    private fun normal(value: String) = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")
}
