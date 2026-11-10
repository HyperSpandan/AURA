package com.aura.data

import com.aura.data.covers.AppleArtistArtRepository
import com.aura.data.history.History
import com.aura.data.model.ArtistPage
import com.aura.data.model.MoodGenre
import com.aura.data.model.ShelfItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Explore as one canvas: artists to jump to, then moods, genres, new
 * albums and chart playlists mixed together in a two-column board.
 */
object ExploreFeed {
    enum class Kind(val label: String) { MOOD("Moods"), GENRE("Genres"), NEW("New releases"), CHART("Charts") }

    sealed interface Tile {
        val kind: Kind
        val key: String

        data class Mood(val mood: MoodGenre, override val kind: Kind) : Tile {
            override val key get() = "m:${mood.browseId}:${mood.params}"
        }

        data class Item(val item: ShelfItem, val from: String, override val kind: Kind) : Tile {
            override val key get() = "i:${item.browseId ?: item.videoId}"
        }
    }

    /** An artist pill: a page to open, a name and maybe a photo. */
    data class Artist(val browseId: String, val name: String, val thumbnailUrl: String?)

    data class Canvas(
        val artists: List<Artist>,
        val tiles: List<Tile>,
        val spotlightArtist: Artist? = null,
        val spotlightPage: ArtistPage? = null,
        val spotlightHeroUrl: String? = null,
        val spotlightReleases: List<ShelfItem> = emptyList(),
        val newReleases: List<ShelfItem> = emptyList(),
        val charts: List<ShelfItem> = emptyList(),
        val moods: List<MoodGenre> = emptyList(),
        val clips: List<ShelfItem> = emptyList(),
    )

    suspend fun load(): Canvas = coroutineScope {
        val moodsDeferred = async { runCatching { MusicRepository.moodsAndGenres() }.getOrDefault(emptyList()) }
        val releasesDeferred = async { runCatching { MusicRepository.shelves("FEmusic_new_releases", null) }.getOrDefault(emptyList()) }
        val chartsDeferred = async { runCatching { MusicRepository.shelves("FEmusic_charts", null) }.getOrDefault(emptyList()) }

        val chartShelves = chartsDeferred.await()
        val chartArtists = artistsOf(chartShelves)
        val mine = MusicRepository.personalArtists()
        val artists = mine.ifEmpty { chartArtists }.distinctBy { it.browseId }.take(24)

        val moodSections = moodsDeferred.await()
        val allMoods = moodSections.flatMap { it.items }
        val moodTiles = moodSections.flatMap { section ->
            val kind = if (section.title.contains("genre", ignoreCase = true)) Kind.GENRE else Kind.MOOD
            section.items.map { Tile.Mood(it, kind) }
        }

        val releaseShelves = releasesDeferred.await()
        val releaseItems = releaseShelves.flatMap { it.items }
        val newTiles = releaseShelves.flatMap { shelf ->
            shelf.items.filter { it.browseId != null }.map { Tile.Item(it, shelf.title, Kind.NEW) }
        }

        val chartItems = chartShelves.flatMap { it.items }
        val chartTiles = chartShelves.flatMap { shelf ->
            shelf.items.filter { it.browseId != null && !it.browseId.startsWith("UC") }.map { Tile.Item(it, shelf.title, Kind.CHART) }
        }

        // Determine Spotlight Artist: prefer Kanye West or user's top artist / first chart artist
        val featuredArtist = artists.firstOrNull { it.name.contains("Kanye", ignoreCase = true) }
            ?: artists.firstOrNull()

        var spotlightPage: ArtistPage? = null
        var spotlightHero: String? = null
        var spotlightReleases: List<ShelfItem> = emptyList()
        var clips: List<ShelfItem> = emptyList()

        if (featuredArtist != null) {
            val (page, hero, releases) = loadArtistSpotlight(featuredArtist)
            spotlightPage = page
            spotlightHero = hero
            spotlightReleases = releases
            clips = spotlightPage?.songs?.map { song ->
                ShelfItem(
                    title = song.title,
                    subtitle = song.artist,
                    thumbnailUrl = song.thumbnailUrl,
                    videoId = song.videoId,
                    browseId = null,
                )
            }?.take(10).orEmpty()
        }

        if (spotlightReleases.isEmpty()) {
            spotlightReleases = releaseItems.filter { it.browseId != null }.distinctBy { it.browseId }.take(10)
        }

        Canvas(
            artists = artists,
            tiles = weave(listOf(moodTiles, newTiles, chartTiles)).distinctBy { it.key },
            spotlightArtist = featuredArtist,
            spotlightPage = spotlightPage,
            spotlightHeroUrl = spotlightHero,
            spotlightReleases = spotlightReleases,
            newReleases = releaseItems.filter { it.browseId != null }.distinctBy { it.browseId ?: it.title }.take(12),
            charts = chartItems.filter { it.browseId != null && !it.browseId.startsWith("UC") }.distinctBy { it.browseId }.take(12),
            moods = allMoods.distinctBy { it.browseId + it.params }.take(16),
            clips = clips,
        )
    }

    private val spotlightCache = java.util.concurrent.ConcurrentHashMap<String, Triple<ArtistPage?, String?, List<ShelfItem>>>()

    fun cachedSpotlight(browseId: String): Triple<ArtistPage?, String?, List<ShelfItem>>? = spotlightCache[browseId]

    /** Loads detailed page, hero art, and discography releases for a spotlight artist, caching results for instant navigation. */
    suspend fun loadArtistSpotlight(artist: Artist): Triple<ArtistPage?, String?, List<ShelfItem>> {
        spotlightCache[artist.browseId]?.let { return it }
        val res = coroutineScope {
            val pageDef = async { runCatching { MusicRepository.artist(artist.browseId) }.getOrNull() }
            val heroDef = async { runCatching { AppleArtistArtRepository.artFor(artist.name)?.heroUrl }.getOrNull() }
            val page = pageDef.await()
            val hero = heroDef.await() ?: page?.thumbnailUrl
            val albums = page?.sections?.firstOrNull { it.title.contains("album", ignoreCase = true) }?.items.orEmpty()
            val singles = page?.sections?.firstOrNull { it.title.contains("single", ignoreCase = true) }?.items.orEmpty()
            val releases = (albums + singles).ifEmpty {
                page?.sections?.flatMap { it.items }.orEmpty()
            }.distinctBy { it.browseId ?: it.title }.take(10)
            Triple(page, hero, releases)
        }
        spotlightCache[artist.browseId] = res
        return res
    }

    /** The artists on a page's shelves (the charts' "Top artists"), each once, in order. */
    fun artistsOf(shelves: List<com.aura.data.model.HomeShelf>): List<Artist> =
        shelves.flatMap { it.items }.filter { it.browseId?.startsWith("UC") == true }
            .map { Artist(it.browseId!!, it.title, it.thumbnailUrl) }
            .distinctBy { it.browseId }

    /**
     * Takes from each list in turn, so the board mixes them instead of
     * showing every mood before the first album.
     */
    fun <T> weave(lists: List<List<T>>): List<T> {
        val out = ArrayList<T>(lists.sumOf { it.size })
        val iterators = lists.map { it.iterator() }
        while (iterators.any { it.hasNext() }) iterators.forEach { if (it.hasNext()) out += it.next() }
        return out
    }
}
