package com.aura.ui.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aura.data.ExploreFeed
import com.aura.data.MusicRepository
import com.aura.data.Pills
import com.aura.data.library.LibraryStore
import com.aura.data.library.SavedList
import com.aura.data.model.ArtistPage
import com.aura.data.model.CARD_ART_PX
import com.aura.data.model.MoodGenre
import com.aura.data.model.ShelfItem
import com.aura.data.model.UiState
import com.aura.data.model.artworkAt
import androidx.compose.runtime.CompositionLocalProvider
import com.aura.ui.browse.ArtistNameOrSignature
import com.aura.ui.browse.SongActions
import com.aura.ui.components.Artwork
import com.aura.ui.components.ErrorState
import com.aura.ui.components.PageHeader
import com.aura.ui.components.Placeholder
import com.aura.ui.components.ShelfPlaceholder
import com.aura.ui.components.pressable
import com.aura.ui.home.HomeShelfView
import com.aura.ui.rememberLoader
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit

/**
 * Explore page redesigned: features a prominent spotlight artist hero with
 * a 3D Cover Flow release carousel, metadata, and quick action controls,
 * followed by curated shelves for clips, trending artists, new releases,
 * top charts, and moods/genres.
 */
@Composable
fun ExploreScreen(
    contentPadding: PaddingValues,
    onMoodClick: (MoodGenre) -> Unit,
    onItemClick: (ShelfItem) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    showHeader: Boolean = true,
    actions: SongActions? = null,
) {
    val header: (@Composable () -> Unit)? = if (!showHeader) null else ({
        PageHeader(
            com.aura.ui.theme.tr("Explore"),
            actions = onOpenSettings?.let { open -> { com.aura.ui.components.AccountButton(open) } },
        )
    })
    val loader = rememberLoader("explore:canvas") { ExploreFeed.load() }
    val state by loader.state.collectAsState()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    when (val s = state) {
        is UiState.Error -> Column(Modifier.fillMaxSize()) {
            header?.invoke()
            Box(Modifier.fillMaxSize().padding(contentPadding), Alignment.Center) {
                ErrorState(s.message, onRetry = { loader.reload() })
            }
        }
        else -> ExploreBoard(
            canvas = (s as? UiState.Success)?.data,
            contentPadding = contentPadding,
            filter = filter,
            onFilter = { filter = it },
            onMoodClick = onMoodClick,
            onItemClick = onItemClick,
            header = header,
            actions = actions,
            onOpenSettings = onOpenSettings,
            onBack = onBack,
            showHeader = showHeader,
        )
    }
}

/** The modern Explore board. */
@Composable
internal fun ExploreBoard(
    canvas: ExploreFeed.Canvas?,
    contentPadding: PaddingValues,
    filter: String?,
    onFilter: (String?) -> Unit,
    onMoodClick: (MoodGenre) -> Unit,
    onItemClick: (ShelfItem) -> Unit,
    header: (@Composable () -> Unit)? = { PageHeader("Explore") },
    actions: SongActions? = null,
    onOpenSettings: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    showHeader: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeArtist by remember(canvas) {
        mutableStateOf(canvas?.spotlightArtist ?: canvas?.artists?.firstOrNull())
    }
    var activePage by remember(canvas) {
        mutableStateOf(canvas?.spotlightPage)
    }
    var activeHeroUrl by remember(canvas) {
        mutableStateOf(canvas?.spotlightHeroUrl)
    }
    var activeReleases by remember(canvas) {
        val releases = canvas?.spotlightReleases.orEmpty()
        mutableStateOf(
            if (releases.isNotEmpty()) releases
            else canvas?.tiles?.filterIsInstance<ExploreFeed.Tile.Item>()?.map { it.item }?.take(10).orEmpty(),
        )
    }

    val savedLists by LibraryStore.savedLists.collectAsState()

    // Prefetch top trending artists and their signatures in background for instant switching
    LaunchedEffect(canvas) {
        val list = canvas?.artists?.take(8).orEmpty()
        if (list.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                list.forEach { a ->
                    launch {
                        ExploreFeed.loadArtistSpotlight(a)
                        com.aura.data.covers.ArtistSignatures.signatureFor(a.name, com.aura.data.settings.AppSettings.ui.value.signatureApi)
                    }
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 28.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090C)),
    ) {
        // 1. Spotlight Artist Hero with 3D Cover Flow Carousel & Controls
        item(key = "spotlight") {
            if (canvas == null) {
                SpotlightHeroPlaceholder()
            } else {
                ExploreSpotlightHero(
                    artist = activeArtist,
                    page = activePage,
                    heroUrl = activeHeroUrl,
                    releases = activeReleases,
                    savedLists = savedLists,
                    onReleaseClick = { release -> onItemClick(release) },
                    onPlayAll = { release ->
                        scope.launch {
                            val id = release.browseId
                            if (id != null) {
                                val songs = runCatching { MusicRepository.collection(id).songs }.getOrNull().orEmpty()
                                if (songs.isNotEmpty() && actions != null) {
                                    actions.playAll(songs, 0, false, release.title)
                                } else {
                                    onItemClick(release)
                                }
                            } else {
                                onItemClick(release)
                            }
                        }
                    },
                    onShuffle = { release ->
                        scope.launch {
                            val id = release.browseId
                            if (id != null) {
                                val songs = runCatching { MusicRepository.collection(id).songs }.getOrNull().orEmpty()
                                if (songs.isNotEmpty() && actions != null) {
                                    actions.playAll(songs, 0, true, release.title)
                                } else {
                                    onItemClick(release)
                                }
                            } else {
                                onItemClick(release)
                            }
                        }
                    },
                    onSave = { release ->
                        val id = release.browseId ?: release.videoId
                        if (!id.isNullOrBlank()) {
                            val isSaved = savedLists.any { it.browseId == id }
                            LibraryStore.setSaved(
                                SavedList(id, release.title, release.subtitle, release.thumbnailUrl),
                                !isSaved,
                            )
                            if (!isSaved) Pills.done("Saved to Library") else Pills.done("Removed from Library")
                        }
                    },
                    onOpenSettings = onOpenSettings,
                    onBack = onBack,
                    showHeader = showHeader,
                )
            }
        }

        if (canvas == null) {
            item(key = "loading-shelves") {
                Column(Modifier.padding(top = 16.dp)) {
                    repeat(3) { ShelfPlaceholder() }
                }
            }
            return@LazyColumn
        }


        // 3. Trending Artists Section
        if (canvas.artists.isNotEmpty()) {
            item(key = "artists-section") {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        text = "Trending Artists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        items(canvas.artists, key = { it.browseId }) { artist ->
                            ArtistCircleItem(
                                artist = artist,
                                isSelected = activeArtist?.browseId == artist.browseId,
                                onClick = {
                                    activeArtist = artist
                                    val cached = ExploreFeed.cachedSpotlight(artist.browseId)
                                    if (cached != null) {
                                        activePage = cached.first
                                        activeHeroUrl = cached.second
                                        if (cached.third.isNotEmpty()) activeReleases = cached.third
                                    } else {
                                        activeHeroUrl = artist.thumbnailUrl
                                        scope.launch {
                                            val (p, h, r) = ExploreFeed.loadArtistSpotlight(artist)
                                            if (activeArtist?.browseId == artist.browseId) {
                                                activePage = p
                                                activeHeroUrl = h
                                                if (r.isNotEmpty()) activeReleases = r
                                            }
                                        }
                                    }
                                },
                                onLongClick = {
                                    onItemClick(ShelfItem(artist.name, "", artist.thumbnailUrl, null, artist.browseId))
                                },
                            )
                        }
                    }
                }
            }
        }

        // 4. New Releases Section
        val releases = canvas.newReleases.ifEmpty {
            canvas.tiles.filterIsInstance<ExploreFeed.Tile.Item>()
                .filter { it.kind == ExploreFeed.Kind.NEW }
                .map { it.item }
        }
        if (releases.isNotEmpty()) {
            item(key = "new-releases-section") {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        text = "New Releases",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        items(releases, key = { it.browseId ?: it.title }) { item ->
                            ExploreCard(item = item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }

        // 5. Top Charts Section
        val charts = canvas.charts.ifEmpty {
            canvas.tiles.filterIsInstance<ExploreFeed.Tile.Item>()
                .filter { it.kind == ExploreFeed.Kind.CHART }
                .map { it.item }
        }
        if (charts.isNotEmpty()) {
            item(key = "top-charts-section") {
                Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(
                        text = "Top Charts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
                        items(charts, key = { it.browseId ?: it.title }) { item ->
                            ExploreCard(item = item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }

        // 6. Moods & Genres Section
        val moods = canvas.moods.ifEmpty {
            canvas.tiles.filterIsInstance<ExploreFeed.Tile.Mood>().map { it.mood }
        }
        if (moods.isNotEmpty()) {
            item(key = "moods-header") {
                Text(
                    text = "Moods & Genres",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 12.dp),
                )
            }
            val moodPairs = moods.chunked(2)
            items(moodPairs, key = { it.joinToString("-") { m -> m.browseId } }) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    pair.forEach { mood ->
                        Box(Modifier.weight(1f)) {
                            MoodTile(mood = mood, height = 96.dp, onClick = { onMoodClick(mood) })
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Spotlight Artist Hero:
 * - High-res artist backdrop portrait
 * - Bold stylized artist title typography
 * - Top navigation bar with scrim
 * - Smooth vertical bottom fade
 * - 3D Cover Flow release carousel with glowing purple active border & elevation
 * - Release title and subtitle metadata
 * - Action controls: [🎥 Clips], [▶ Play All], [📄 Save]
 */
@Composable
private fun ExploreSpotlightHero(
    artist: ExploreFeed.Artist?,
    page: ArtistPage?,
    heroUrl: String?,
    releases: List<ShelfItem>,
    savedLists: List<SavedList>,
    onReleaseClick: (ShelfItem) -> Unit,
    onPlayAll: (ShelfItem) -> Unit,
    onShuffle: (ShelfItem) -> Unit,
    onSave: (ShelfItem) -> Unit,
    onOpenSettings: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    showHeader: Boolean,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = if (releases.isNotEmpty()) (releases.size / 2).coerceAtLeast(0) else 0,
        pageCount = { releases.size },
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val heroHeight = (screenHeight * 0.78f).coerceIn(660.dp, 780.dp)
    val backdropHeight = (screenHeight * 0.58f).coerceIn(490.dp, 590.dp)
    val fadeHeight = (screenHeight * 0.42f).coerceIn(340.dp, 420.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        val screenWidth = maxWidth
        val cardSize = 126.dp
        val centerPadding = ((screenWidth - cardSize) / 2).coerceAtLeast(0.dp)

        // 1. Artist Backdrop Photo
        val displayHero = heroUrl ?: page?.thumbnailUrl ?: artist?.thumbnailUrl
        if (displayHero != null) {
            Artwork(
                url = displayHero.artworkAt(ARTIST_PHOTO_PX),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backdropHeight),
                shape = RoundedCornerShape(0.dp),
            )
            // Mid-tone soft vignette so text pops against any background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backdropHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.65f),
                            ),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(backdropHeight)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF2E1065), Color(0xFF09090C)),
                        ),
                    ),
            )
        }

        // 2. Top Scrim for Status Bar and Navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent),
                    ),
                ),
        )

        // 3. Smooth Bottom Gradient Fade into Dark Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fadeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x6609090C),
                            Color(0xCC09090C),
                            Color(0xFF09090C),
                            Color(0xFF09090C),
                        ),
                    ),
                ),
        )

        // 4. Top Navigation Bar (Status Bar Scrim + Action Buttons)
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back Button (Chevron)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onBack?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // More / Settings Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onOpenSettings?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // 5. Hero Content: Artist Signature, 3D Cover Flow Carousel, Release Metadata, and Action Controls
        val rawName = artist?.name ?: page?.name ?: "EXPLORE"
        val heroMorph = com.aura.ui.components.LocalHeroMorph.current
        var selectedCardBounds by remember { mutableStateOf(Rect.Zero) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // A. Artist Signature or Name (positioned cleanly below the artist's face)
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides Color.White,
            ) {
                ArtistNameOrSignature(rawName, compact = true)
            }

            Spacer(Modifier.height(14.dp))

            // B. 3D Cover Flow Horizontal Pager
            if (releases.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = centerPadding),
                    pageSpacing = (-26).dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(138.dp),
                ) { pageIdx ->
                    val release = releases.getOrNull(pageIdx) ?: return@HorizontalPager
                    val pageOffset = ((pagerState.currentPage - pageIdx) + pagerState.currentPageOffsetFraction)
                    val isSelected = pagerState.currentPage == pageIdx

                    val scale = (1f - pageOffset.absoluteValue * 0.16f).coerceIn(0.78f, 1f)
                    val rotationZ = (pageOffset * 5.5f).coerceIn(-12f, 12f)
                    val rotationY = (pageOffset * -14f).coerceIn(-28f, 28f)
                    val alpha = (1f - pageOffset.absoluteValue * 0.22f).coerceIn(0.55f, 1f)

                    Box(
                        modifier = Modifier
                            .size(cardSize)
                            .zIndex(1f - pageOffset.absoluteValue.coerceAtMost(1f))
                            .then(if (isSelected) Modifier.onGloballyPositioned { selectedCardBounds = it.boundsInWindow() } else Modifier)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.rotationZ = rotationZ
                                this.rotationY = rotationY
                                this.alpha = alpha
                                cameraDistance = 14f * density
                            }
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .shadow(
                                            elevation = 26.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            ambientColor = Color(0xFFA855F7),
                                            spotColor = Color(0xFF8B5CF6),
                                            clip = false,
                                        )
                                        .border(
                                            width = 2.5.dp,
                                            brush = Brush.linearGradient(
                                                listOf(Color(0xFFC084FC), Color(0xFF818CF8), Color(0xFFA855F7)),
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                        )
                                } else Modifier,
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E1E26))
                            .clickable {
                                if (isSelected) {
                                    val bId = release.browseId
                                    if (heroMorph != null && bId != null && selectedCardBounds != Rect.Zero) {
                                        heroMorph.launch(
                                            browseId = bId,
                                            title = release.title,
                                            subtitle = release.subtitle,
                                            thumbnailUrl = release.thumbnailUrl,
                                            bounds = selectedCardBounds,
                                            initialCornerRadius = 20.dp,
                                            onNavigate = { onReleaseClick(release) },
                                        )
                                    } else {
                                        onReleaseClick(release)
                                    }
                                } else {
                                    scope.launch { pagerState.animateScrollToPage(pageIdx) }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Artwork(
                            url = release.thumbnailUrl.artworkAt(CARD_ART_PX),
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }
            }

            // B. Release Metadata Row
            val currentRelease = releases.getOrNull(pagerState.currentPage)
            if (currentRelease != null) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = currentRelease.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    val rawSub = currentRelease.subtitle.trim()
                    val sub = if (rawSub.isNotBlank()) {
                        if (rawSub.contains("album", ignoreCase = true) || rawSub.contains("single", ignoreCase = true) || rawSub.contains("ep", ignoreCase = true)) {
                            rawSub
                        } else {
                            "Studio album by ${artist?.name ?: "Artist"} • $rawSub"
                        }
                    } else {
                        "Studio album by ${artist?.name ?: "Artist"}"
                    }
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // C. Action Controls Row: [🔀 Shuffle], [▶ Play All], [📄 Save]
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // [🔀 Shuffle]
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            currentRelease?.let { onShuffle(it) }
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                // [▶ Play All]
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color.White)
                        .clickable {
                            currentRelease?.let { onPlayAll(it) }
                        }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play All",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "Play All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        softWrap = false,
                    )
                }

                // [📄 Save]
                val isSaved = remember(currentRelease, savedLists) {
                    val id = currentRelease?.browseId ?: currentRelease?.videoId
                    id != null && savedLists.any { it.browseId == id }
                }
                Row(
                    modifier = Modifier
                        .height(42.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            currentRelease?.let { onSave(it) }
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (isSaved) "Saved" else "Save",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}

/** Placeholder for spotlight hero while loading. */
@Composable
private fun SpotlightHeroPlaceholder() {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val heroHeight = (screenHeight * 0.78f).coerceIn(660.dp, 780.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(Color(0xFF14141B)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Placeholder(Modifier.width(180.dp).height(32.dp), RoundedCornerShape(16.dp))
            Placeholder(Modifier.size(122.dp), RoundedCornerShape(20.dp))
            Placeholder(Modifier.width(220.dp).height(18.dp), RoundedCornerShape(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Placeholder(Modifier.width(90.dp).height(42.dp), RoundedCornerShape(21.dp))
                Placeholder(Modifier.width(110.dp).height(42.dp), RoundedCornerShape(21.dp))
                Placeholder(Modifier.width(90.dp).height(42.dp), RoundedCornerShape(21.dp))
            }
        }
    }
}

/** Video / Music clip card with 16:9 ratio and center play badge. */
@Composable
private fun ClipCard(clip: ShelfItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .pressable(onClick, pressedScale = 0.96f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF18181F)),
        ) {
            Artwork(
                url = clip.thumbnailUrl.artworkAt(CARD_ART_PX),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = clip.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        if (clip.subtitle.isNotBlank()) {
            Text(
                text = clip.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** Circular artist avatar item with switching and open on long-press. */
@Composable
private fun ArtistCircleItem(
    artist: ExploreFeed.Artist,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            2.dp,
                            Brush.linearGradient(listOf(Color(0xFFC084FC), Color(0xFF6366F1))),
                            CircleShape,
                        )
                    } else Modifier,
                )
                .padding(if (isSelected) 3.dp else 0.dp)
                .clip(CircleShape)
                .background(Color(0xFF26262F)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (artist.thumbnailUrl != null) {
                Artwork(
                    url = artist.thumbnailUrl.artworkAt(150),
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFFC084FC) else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Standard square release/playlist card for explore rows. */
@Composable
private fun ExploreCard(item: ShelfItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .pressable(onClick, pressedScale = 0.96f),
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E1E26)),
        ) {
            Artwork(
                url = item.thumbnailUrl.artworkAt(CARD_ART_PX),
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

/** Covers cache for mood tiles. */
private object MoodCovers {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val gate = kotlinx.coroutines.sync.Semaphore(3)

    fun cached(mood: MoodGenre): String? = mood.thumbnailUrl ?: cache[mood.browseId + mood.params]

    suspend fun load(mood: MoodGenre): String? {
        cached(mood)?.let { return it }
        return gate.withPermit {
            runCatching {
                MusicRepository.shelves(mood.browseId, mood.params)
                    .firstNotNullOfOrNull { shelf -> shelf.items.firstNotNullOfOrNull { it.thumbnailUrl } }
            }.getOrNull()?.also { cache[mood.browseId + mood.params] = it }
        }
    }
}

/** A mood card with rich gradient overlay. */
@Composable
private fun MoodTile(mood: MoodGenre, height: androidx.compose.ui.unit.Dp = 104.dp, onClick: () -> Unit) {
    val hue = (mood.title.hashCode().absoluteValue % 360).toFloat()
    val colour = Color.hsv(hue, 0.65f, 0.55f)
    var cover by remember(mood) { mutableStateOf(MoodCovers.cached(mood)) }
    LaunchedEffect(mood) { if (cover == null) cover = MoodCovers.load(mood) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .pressable(onClick, pressedScale = 0.97f)
            .clip(RoundedCornerShape(20.dp))
            .background(colour),
    ) {
        AnimatedVisibility(cover != null, enter = fadeIn(tween(500)), modifier = Modifier.matchParentSize()) {
            Artwork(cover.artworkAt(CARD_ART_PX), Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(listOf(colour.copy(alpha = 0.05f), colour.copy(alpha = 0.55f), colour.copy(alpha = 0.95f))),
            ),
        )
        Text(
            mood.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 12.dp, end = 14.dp),
        )
    }
}

/** A mood or genre's own page: shelves of playlists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    title: String,
    browseId: String,
    params: String?,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onItemClick: (ShelfItem) -> Unit,
) {
    val loader = rememberLoader("mood:$browseId:$params") { MusicRepository.shelves(browseId, params) }
    val state by loader.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        PageHeader(title, onBack = onBack)
        when (val s = state) {
            is UiState.Loading -> LazyColumn { items(3) { ShelfPlaceholder() } }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(contentPadding), Alignment.Center) {
                ErrorState(s.message, onRetry = { loader.reload() })
            }
            is UiState.Success -> LazyColumn(contentPadding = contentPadding) {
                items(s.data, key = { it.title + (it.items.firstOrNull()?.browseId ?: it.items.firstOrNull()?.videoId ?: "") }) { HomeShelfView(it, onItemClick) }
            }
        }
    }
}

private const val ARTIST_PHOTO_PX = 1080
