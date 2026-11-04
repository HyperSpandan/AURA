package com.aura.ui.browse

import com.aura.ui.components.pinnedToTop
import com.aura.ui.components.GlassPage
import com.aura.ui.components.LocalHazeState
import com.aura.ui.player.LoopingVideo
import com.aura.data.covers.AppleMotionArt
import com.aura.data.settings.AppSettings
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawBehind
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.focusRequester
import com.aura.data.model.durationMillis
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.data.MusicRepository
import com.aura.data.model.BrowseType
import com.aura.data.model.HEADER_ART_PX
import com.aura.data.model.CARD_ART_PX
import com.aura.data.model.Song
import com.aura.data.model.UiState
import com.aura.data.model.artworkAt
import com.aura.ui.components.Artwork
import com.aura.ui.components.GlassBackButton
import com.aura.ui.components.glass
import com.aura.ui.components.ErrorState
import com.aura.ui.components.Placeholder
import com.aura.ui.components.SongListItem
import com.aura.ui.components.SongRowPlaceholder
import com.aura.ui.rememberLoader

/** Callbacks every track list needs. */
@androidx.compose.runtime.Immutable
class SongActions(
    val currentVideoId: String?,
    val isPlaying: Boolean,
    /** songs, start index, shuffle, "Playing from" label. */
    val playAll: (List<Song>, Int, Boolean, String?) -> Unit,
    val playNext: (Song) -> Unit,
    val addToQueue: (Song) -> Unit,
    /** Shuffle with similar songs mixed in: a long press on Shuffle. */
    val smartShuffle: (List<Song>, String?) -> Unit = { songs, source -> playAll(songs, 0, true, source) },
)

/** The same actions, with [onPlay] told each time something on the page starts playing. */
internal fun SongActions.alsoOnPlay(onPlay: () -> Unit): SongActions = SongActions(
    currentVideoId, isPlaying,
    playAll = { songs, index, shuffle, source -> onPlay(); playAll(songs, index, shuffle, source) },
    playNext = playNext,
    addToQueue = addToQueue,
    smartShuffle = { songs, source -> onPlay(); smartShuffle(songs, source) },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollapsingBar(
    title: String,
    listState: LazyListState,
    onBack: () -> Unit,
    forceCollapsed: Boolean = false,
    artworkBehind: Boolean = true,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    val scrolled by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val collapsed = forceCollapsed || scrolled
    // Once the header scrolls away the bar frosts in: the list under it is
    // blurred and washed with the page colour, and the frost thins out over
    // a short tail below the bar instead of stopping at a hard line.
    val frost by androidx.compose.animation.core.animateFloatAsState(if (collapsed) 1f else 0f, androidx.compose.animation.core.tween(260), label = "frost")
    val haze = LocalHazeState.current
    val page = MaterialTheme.colorScheme.background
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    var tall by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    Box {
        if (frost > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        // Drawn taller than the bar by the tail; the bar's own size is unchanged.
                        val h = constraints.maxHeight + FROST_TAIL.roundToPx()
                        val p = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                        layout(constraints.maxWidth, constraints.maxHeight) { p.place(0, 0) }
                    }
                    .onSizeChanged { tall = it.height }
                    .then(
                        if (haze == null) {
                            Modifier.graphicsLayer { alpha = frost }.drawBehind {
                                drawRect(Brush.verticalGradient(0f to page.copy(alpha = 0.96f), 0.7f to page.copy(alpha = 0.9f), 1f to page.copy(alpha = 0f)))
                            }
                        } else {
                            Modifier.hazeEffect(haze) {
                                alpha = frost
                                backgroundColor = page
                                tints = listOf(HazeTint(page.copy(alpha = 0.6f)))
                                blurRadius = 26.dp
                                noiseFactor = 0.03f
                                progressive = HazeProgressive.verticalGradient(
                                    easing = androidx.compose.animation.core.EaseIn,
                                    startY = tall - FROST_TAIL.value * density * 1.6f,
                                    startIntensity = 1f,
                                    endY = tall.toFloat(),
                                    endIntensity = 0f,
                                )
                            }
                        },
                    ),
            )
        }
        TopAppBar(
            title = {
                androidx.compose.animation.AnimatedVisibility(
                    collapsed,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
                    exit = androidx.compose.animation.fadeOut(),
                ) { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            },
            navigationIcon = { GlassBackButton(onBack, Modifier.padding(start = 8.dp).size(48.dp)) },
            actions = {
                actions()
                Spacer(Modifier.width(8.dp))
            },
            // White over the cover, where grey sank into its colours; the page's
            // own text colour once the bar has frosted over the list.
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                actionIconContentColor = androidx.compose.animation.animateColorAsState(
                    if (collapsed || !artworkBehind) MaterialTheme.colorScheme.onSurface else Color.White, androidx.compose.animation.core.tween(260), label = "barIcons",
                ).value,
            ),
        )
    }
}

/** How far below the collapsed bar its frost reaches as it fades out. */
private val FROST_TAIL = 22.dp

@Composable
private fun PlayShuffleButtons(enabled: Boolean, onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier, onSmartShuffle: (() -> Unit)? = null) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onPlay, enabled = enabled, modifier = Modifier.weight(1f).height(52.dp)) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(com.aura.ui.theme.tr("Play"))
        }
        ShuffleButton(onShuffle, onSmartShuffle, Modifier.weight(1f), enabled = enabled)
    }
}

/**
 * A list's Shuffle button; a long press starts smart shuffle, with similar
 * songs mixed in. The player's shuffle button cycles through a third state
 * instead, but this one starts a queue rather than holding a state, so a
 * second tap would have nothing to move on to. [compact] is the small round
 * glass button.
 */
@Composable
internal fun ShuffleButton(onShuffle: () -> Unit, onSmartShuffle: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true, compact: Boolean = false) {
    val smartAllowed = com.aura.data.settings.AppSettings.playback.collectAsState().value.smartShuffle
    val context = androidx.compose.ui.platform.LocalContext.current
    val onLong: (() -> Unit)? = onSmartShuffle?.takeIf { smartAllowed && enabled }?.let { smart ->
        {
            com.aura.data.Pills.info("Smart shuffle: similar songs mixed in", context)
            smart()
        }
    }
    val shape = if (compact) androidx.compose.foundation.shape.CircleShape else androidx.compose.material3.ButtonDefaults.filledTonalShape
    val press = Modifier.clip(shape).combinedClickable(
        enabled = enabled,
        role = androidx.compose.ui.semantics.Role.Button,
        onLongClickLabel = if (onLong != null) "Smart shuffle" else null,
        onLongClick = onLong,
        onClick = onShuffle,
    )
    if (compact) {
        Box(modifier.size(44.dp).glass(androidx.compose.foundation.shape.CircleShape).then(press), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Shuffle, "Shuffle", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        // Drawn like a FilledTonalButton, which has no long press of its own.
        val colors = MaterialTheme.colorScheme
        androidx.compose.material3.Surface(
            shape = shape,
            color = if (enabled) colors.secondaryContainer else colors.onSurface.copy(alpha = 0.12f),
            contentColor = if (enabled) colors.onSecondaryContainer else colors.onSurface.copy(alpha = 0.38f),
            modifier = modifier.height(52.dp).then(press),
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shuffle, null)
                Spacer(Modifier.width(8.dp))
                Text(com.aura.ui.theme.tr("Shuffle"), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * The cover blurred and stretched behind a page's header, fading into the
 * page below it. Before Android 12, which can't blur, a tiny copy of the
 * cover stretched to fill makes the same soft wash.
 */
@Composable
internal fun HeaderBackdrop(
    url: String?,
    modifier: Modifier = Modifier,
    /** What it fades into at its foot: the page under it, plain or tinted (see [rememberCoverTint]). */
    into: Color = MaterialTheme.colorScheme.background,
) {
    if (url.isNullOrBlank()) return
    val background = into
    Box(modifier.clipToBounds().fadeOutAtFoot()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Artwork(
                url,
                Modifier.matchParentSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f; alpha = 0.85f }.blur(70.dp),
                shape = androidx.compose.ui.graphics.RectangleShape,
            )
        } else {
            Artwork(
                url.artworkAt(24),
                Modifier.matchParentSize().graphicsLayer { scaleX = 1.4f; scaleY = 1.4f; alpha = 0.7f },
                shape = androidx.compose.ui.graphics.RectangleShape,
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(0f to background.copy(alpha = 0.15f), 0.55f to background.copy(alpha = 0.45f), 1f to background),
            ),
        )
    }
}

/**
 * Fades what it's on out to nothing over its last stretch, so a header melts
 * into the page's wash behind it. Ending on a solid colour drew a hard line
 * across the page wherever the header stopped, because the wash behind it is
 * pinned to the screen and is a different shade at every height.
 */
internal fun Modifier.fadeOutAtFoot(): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(0.55f to Color.Black, 1f to Color.Transparent),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
        )
    }

/**
 * The page's colour taken from a cover: its own colour laid lightly over the
 * background. A dark cover's colour is lifted to a mid tone first, and a
 * black-and-white one (which has no colour to read) gets a soft grey, so
 * neither leaves the page plain black.
 */
@Composable
internal fun rememberCoverTint(url: String?): Color {
    val background = MaterialTheme.colorScheme.background
    val seed = com.aura.ui.theme.rememberArtworkSeed(url)
    val loaded = !url.isNullOrBlank()
    val tint by androidx.compose.animation.animateColorAsState(
        if (loaded) androidx.compose.ui.graphics.lerp(background, coverTone(seed, background.luminance() < 0.5f), 0.32f) else background,
        label = "coverTint",
    )
    return tint
}

/** [seed] at a tone that reads on the page: a mid tone on a dark page, a light one on a light page; grey without a seed. */
internal fun coverTone(seed: Color?, dark: Boolean): Color {
    val hct = com.materialkolor.hct.Hct.fromInt((seed ?: Color(0xFF808080)).toArgb())
    val tone = if (dark) hct.tone.coerceIn(42.0, 62.0) else hct.tone.coerceIn(70.0, 88.0)
    return Color(com.materialkolor.hct.Hct.from(hct.hue, if (seed == null) 0.0 else hct.chroma, tone).toInt())
}

/**
 * The cover's colour behind the whole page, not just its header: even down the
 * top half of the screen, where the header fades into it, then easing toward
 * the plain background at the foot.
 */
@Composable
internal fun Modifier.coverWash(tint: Color): Modifier {
    val background = MaterialTheme.colorScheme.background
    // Down to the foot it keeps most of the colour, rather than ending in the plain background.
    return this.background(Brush.verticalGradient(0f to tint, 0.5f to tint, 1f to androidx.compose.ui.graphics.lerp(tint, background, 0.3f)))
}

/** The orders a list's songs can be shown in. */
internal enum class ListOrder(val label: String) {
    DEFAULT("Default order"),
    A_TO_Z("Alphabetical (A to Z)"),
    Z_TO_A("Alphabetical (Z to A)"),
}

/** "Playlist · 146 Tracks · 9h 48m": the kind, how many songs, and how long they run where that's known. */
internal fun listMeta(kind: String, songs: List<com.aura.data.model.Song>): String {
    val count = "${songs.size} ${if (songs.size == 1) "Track" else "Tracks"}"
    val total = songs.sumOf { it.durationMillis() } / 1000
    val length = when {
        total <= 0 -> null
        total >= 3600 -> "${total / 3600}h ${(total % 3600) / 60}m"
        total >= 60 -> "${total / 60} min"
        else -> "${total}s"
    }
    return listOfNotNull(kind, count, length).joinToString(" · ")
}

private val ALBUM_HERO_FADE = Brush.verticalGradient(
    0f to Color.Black,
    0.55f to Color.Black,
    0.78f to Color.Black.copy(alpha = 0.65f),
    0.92f to Color.Black.copy(alpha = 0.20f),
    1f to Color.Transparent,
)

/**
 * Apple Music-style immersive animated header for albums: full-bleed motion art
 * looping edge-to-edge behind the status bar and top navigation, fading softly into
 * the page wash, with title, artist, and playback actions overlaid.
 */
@Composable
private fun FullBleedAlbumHeader(
    collection: com.aura.data.Collection,
    motion: AppleMotionArt.Motion?,
    listState: LazyListState,
    band: com.aura.ui.components.RubberBandOverscroll? = null,
    saved: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSmartShuffle: () -> Unit,
    onSave: (() -> Unit)?,
) {
    val ui by AppSettings.ui.collectAsState()
    val still = ui.reduceAnimation
    val quality = ui.motionQuality

    // Apple Music parallax drift: the video drifts down at a fraction of the scroll
    val drift: androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit = {
        if (!still && listState.firstVisibleItemIndex == 0) {
            translationY = listState.firstVisibleItemScrollOffset * 0.35f
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val heroHeight = (screenHeight * 0.72f).coerceIn(600.dp, 760.dp)

        // 1. The Media Layer: edge-to-edge full-bleed video + static cover base
        Box(
            Modifier
                .pinnedToTop(band)
                .fillMaxWidth()
                .height(heroHeight)
                .clipToBounds()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(ALBUM_HERO_FADE, blendMode = BlendMode.DstIn)
                },
        ) {
            Artwork(
                collection.thumbnailUrl.artworkAt(HEADER_ART_PX),
                Modifier.fillMaxSize().graphicsLayer(drift),
                shape = RectangleShape,
                placeholder = Icons.Filled.Album,
            )
            if (motion != null && !still) {
                val videoUrl = motion.tallUrl ?: motion.url
                val fallback = motion.fallbackUrl ?: if (motion.tallUrl != null) motion.url else null
                LoopingVideo(
                    url = videoUrl,
                    playing = true,
                    modifier = Modifier.fillMaxSize().graphicsLayer(drift),
                    fallbackUrl = fallback,
                    maxPx = quality.maxPx,
                    maxBitrate = quality.maxBitrate,
                )
            }
        }

        // 2. Top Scrim for status bar and navigation buttons (back, search, sort, more)
        Box(
            Modifier
                .graphicsLayer { translationY = -(band?.pull ?: 0f) }
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        0.45f to Color.Black.copy(alpha = 0.22f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        // 3. Subtle dark gradient behind text & action buttons to ensure high contrast
        Box(
            Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.40f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.72f),
                    ),
                ),
        )

        // 4. Overlaid titles and buttons (Apple Music layout)
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val artist = collection.subtitle.split("•").firstOrNull()?.trim()?.ifBlank { collection.subtitle } ?: collection.subtitle
            if (artist.isNotBlank()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val meta = listMeta("Album", collection.songs)
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(18.dp))

            ListPlayRow(
                enabled = collection.songs.isNotEmpty(),
                saved = saved,
                onPlay = onPlay,
                onShuffle = onShuffle,
                onSmartShuffle = onSmartShuffle,
                onSave = onSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Shared artwork, typography and spacing for YouTube, Spotify and local playlists. */
@Composable
internal fun PlaylistHeading(title: String, subtitle: String, thumbnail: String?, songs: List<com.aura.data.model.Song>,
    isAlbum: Boolean = false, cover: (@Composable () -> Unit)? = null) {
    val coverSize = if (com.aura.ui.tv.LocalIsTv.current) Modifier.size(220.dp) else Modifier.fillMaxWidth(0.76f).aspectRatio(1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (cover == null) Artwork(
            thumbnail.artworkAt(HEADER_ART_PX),
            coverSize.shadow(24.dp, MaterialTheme.shapes.large),
            shape = MaterialTheme.shapes.large,
            placeholder = Icons.Filled.Album,
        )
        else Box(coverSize.shadow(24.dp, MaterialTheme.shapes.large)) { cover() }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        // Who it's by, then what it is: "Playlist · 146 Tracks · 9h 48m".
        if (subtitle.isNotBlank()) {
            com.aura.ui.components.SubtitleLine(
                subtitle,
                explicit = isAlbum && songs.any { it.isExplicit == true },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            listMeta(if (isAlbum) "Album" else "Playlist", songs),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** Shuffle in a round glass button, Play as a wide light pill, and a round button to save the list. */
@Composable
internal fun ListPlayRow(
    enabled: Boolean,
    saved: Boolean,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSmartShuffle: () -> Unit,
    onSave: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        ShuffleButton(onShuffle, onSmartShuffle, Modifier.size(56.dp), enabled = enabled, compact = true)
        androidx.compose.material3.Button(
            onClick = onPlay,
            enabled = enabled,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.onSurface, contentColor = colors.surface),
        ) {
            Icon(Icons.Filled.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(com.aura.ui.theme.tr("Play"), style = MaterialTheme.typography.titleMedium)
        }
        if (onSave != null) Box(
            Modifier.size(56.dp).glass(androidx.compose.foundation.shape.CircleShape).clip(androidx.compose.foundation.shape.CircleShape).clickable(enabled = enabled, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (saved) Icons.Filled.Check else Icons.Filled.Add, actionLabel ?: if (saved) "Remove from library" else "Save to library", tint = colors.onSurface)
        }
    }
}

/** "Search this list", in a rounded field under the buttons. */
@Composable
internal fun ListSearchField(query: String, onQuery: (String) -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier, autoFocus: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) runCatching { focus.requestFocus() } }
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(colors.surfaceContainerHigh.copy(alpha = 0.85f)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, null, tint = colors.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(com.aura.ui.theme.tr("Search this list"), style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            androidx.compose.foundation.text.BasicTextField(
                query, onQuery,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        androidx.compose.material3.IconButton(onClick = onClear) { Icon(Icons.Filled.Close, "Close search") }
    }
}

/** The page's own menu: the list's cover and name, then what can be done with the whole list. */
@Composable
private fun ListMoreSheet(c: com.aura.data.Collection, isAlbum: Boolean, actions: SongActions, onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val savedLists by com.aura.data.library.LibraryStore.savedLists.collectAsState()
    val saved = savedLists.any { it.browseId == c.browseId }
    com.aura.ui.components.FloatingCard(
        onDismiss = onDismiss,
        contentPadding = PaddingValues(0.dp),
        startExpanded = true,
        header = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(c.thumbnailUrl.artworkAt(com.aura.data.model.ROW_ART_PX), Modifier.size(56.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(c.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        },
    ) {
        val card = com.aura.ui.components.LocalFloatingCard.current
        val close: () -> Unit = { card?.close() ?: onDismiss() }
        Column {
            com.aura.ui.components.MenuRow(Icons.Filled.Refresh, "Refresh") { close(); onRefresh() }
            com.aura.ui.components.MenuRow(Icons.AutoMirrored.Filled.PlaylistPlay, "Play next") { close(); c.songs.asReversed().forEach(actions.playNext) }
            com.aura.ui.components.MenuRow(Icons.AutoMirrored.Filled.QueueMusic, "Add to queue") { close(); c.songs.forEach(actions.addToQueue) }
            com.aura.ui.components.MenuRow(Icons.Filled.Download, "Download all") {
                close()
                c.songs.forEach { com.aura.data.download.Downloads.enqueue(context, it) }
                com.aura.data.Pills.done("Downloading ${c.songs.size} songs", context)
            }
            com.aura.ui.components.MenuRow(Icons.Filled.Share, "Share") { close(); com.aura.ui.share.ListShare.shareList(context, c.title, c.browseId, com.aura.ui.share.ListShare.coversFor(c.thumbnailUrl, c.songs, album = c.browseId.startsWith("MPRE")), c.subtitle) }
            com.aura.ui.components.MenuRow(if (saved) Icons.Filled.BookmarkRemove else Icons.Filled.PushPin, if (saved) "Unpin" else "Pin") {
                close()
                com.aura.data.library.LibraryStore.setSaved(com.aura.data.library.SavedList(c.browseId, c.title, c.subtitle, c.thumbnailUrl), !saved)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun CollectionScreen(
    browseId: String,
    contentPadding: PaddingValues,
    pageActions: SongActions,
    onBack: () -> Unit,
) {
    val owner = com.aura.data.library.LibraryCache.owner()
    val loader = androidx.lifecycle.viewmodel.compose.viewModel<CollectionLoader>(key = "collection:$owner:$browseId") {
        CollectionLoader(browseId, owner)
    }
    val loadingState by loader.state.collectAsState()
    val firstPage by loader.preview.collectAsState()
    val state = firstPage?.let { UiState.Success(it) } ?: loadingState
    val listState = rememberLazyListState()
    val loaded = (state as? UiState.Success)?.data
    val isAlbum = MusicRepository.typeOf(browseId) == BrowseType.ALBUM || browseId.startsWith("MPRE") || (loaded?.subtitle?.contains("Album", ignoreCase = true) == true)
    val title = (state as? UiState.Success)?.data?.title.orEmpty()
    val uiSettings by AppSettings.ui.collectAsState()
    val canAnimate = uiSettings.animatedCovers && !uiSettings.reduceAnimation && !(uiSettings.animatedCoversWifiOnly && AppSettings.onMeteredNetwork())
    val isTv = com.aura.ui.tv.LocalIsTv.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var albumMotion by remember(loaded?.browseId) {
        mutableStateOf(loaded?.let { AppleMotionArt.cachedAlbum(it.browseId) })
    }
    LaunchedEffect(loaded?.browseId, canAnimate) {
        val col = loaded
        if (col != null && isAlbum && canAnimate && albumMotion == null) {
            albumMotion = AppleMotionArt.forAlbum(col)
        }
    }
    // Looking through the list, its order, and the page's own menu, as BitChord has them.
    val search = remember(browseId) { PlaylistSearch() }
    val searching = search.open
    val query = search.query
    LaunchedEffect(searching, query) {
        if (searching) listState.scrollToItem(0)
    }
    var order by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(ListOrder.DEFAULT) }
    var sortOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    // Played from here: the album or playlist counts toward Speed dial.
    val actions = remember(pageActions, loaded) {
        loaded?.let { c ->
            pageActions.alsoOnPlay { com.aura.data.history.ListenedCollections.played(browseId, c.title, c.subtitle, c.thumbnailUrl) }
        } ?: pageActions
    }
    val shown = remember(loaded, query, order) {
        val songs = loaded?.songs.orEmpty()
        val q = query.trim()
        val found = if (q.isEmpty()) songs else songs.filter { it.title.contains(q, ignoreCase = true) || it.artist.contains(q, ignoreCase = true) }
        when (order) {
            ListOrder.DEFAULT -> found
            ListOrder.A_TO_Z -> found.sortedBy { it.title.lowercase() }
            ListOrder.Z_TO_A -> found.sortedByDescending { it.title.lowercase() }
        }
    }
    if (moreOpen && loaded != null) ListMoreSheet(loaded, isAlbum, actions, onRefresh = { loader.reload() }, onDismiss = { moreOpen = false })

    GlassPage(Modifier.fillMaxSize().dismissPlaylistSearch(search), overlay = {
        PlaylistSearchHeader(search) {
        CollapsingBar(title, listState, onBack, forceCollapsed = searching, artworkBehind = loaded?.thumbnailUrl != null) {
            // Search, order and more in one glass pill.
            Row(
                Modifier.height(48.dp).glass(androidx.compose.foundation.shape.CircleShape).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.IconButton(onClick = { if (searching) search.close() else search.open = true }) {
                    Icon(if (searching) Icons.Filled.Close else Icons.Filled.Search, if (searching) "Stop searching" else "Search this list")
                }
                Box {
                    androidx.compose.material3.IconButton(onClick = { sortOpen = true }) { Icon(Icons.AutoMirrored.Filled.Sort, "Sort") }
                    androidx.compose.material3.DropdownMenu(sortOpen, onDismissRequest = { sortOpen = false }, shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
                        ListOrder.entries.forEach { o ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(o.label) },
                                trailingIcon = { if (o == order) Icon(Icons.Filled.Check, null) },
                                onClick = { order = o; sortOpen = false },
                            )
                        }
                    }
                }
                androidx.compose.material3.IconButton(onClick = { moreOpen = true }, enabled = loaded != null) { Icon(Icons.Filled.MoreHoriz, "More") }
            }
        }
        }
    }) {
        when (val s = state) {
            is UiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { ErrorState(s.message, { loader.reload() }) }
            else -> {
                val c = (s as? UiState.Success)?.data
                val tint = rememberCoverTint(c?.thumbnailUrl.artworkAt(CARD_ART_PX))
                val band = com.aura.ui.components.rememberRubberBand()
                LazyColumn(state = listState, contentPadding = if (searching) PaddingValues(bottom = 16.dp) else contentPadding, modifier = Modifier.fillMaxSize().coverWash(tint).playlistSearchResults(search), overscrollEffect = band) {
                    if (!searching) item(key = "header") {
                        val showFullBleed = isAlbum && !isTv && !isLandscape && loaded != null
                        val savedLists by com.aura.data.library.LibraryStore.savedLists.collectAsState()
                        val saved = c != null && savedLists.any { it.browseId == c.browseId }
                        val own = c?.let { rememberOwnPlaylist(it) }
                        val accountLists by com.aura.data.library.LibraryCache.playlists.collectAsState()
                        val inAccount = own != null || (c != null && accountLists.any { it.browseId?.removePrefix("VL") == c.browseId.removePrefix("VL") })
                        val onPlayAll = { c?.let { actions.playAll(shown.ifEmpty { it.songs }, 0, false, it.title) } ?: Unit }
                        val onShuffleAll = { c?.let { actions.playAll(shown.ifEmpty { it.songs }, 0, true, it.title) } ?: Unit }
                        val onSmartShuffleAll = { c?.let { actions.smartShuffle(shown.ifEmpty { it.songs }, it.title) } ?: Unit }
                        val onSaveList: (() -> Unit)? = if (inAccount && !saved) null else ({
                            c?.let {
                                com.aura.data.library.LibraryStore.setSaved(
                                    com.aura.data.library.SavedList(it.browseId, it.title, it.subtitle, it.thumbnailUrl), !saved,
                                )
                            }
                        })

                        if (showFullBleed && loaded != null) {
                            FullBleedAlbumHeader(
                                collection = loaded,
                                motion = albumMotion,
                                listState = listState,
                                band = band,
                                saved = saved,
                                onPlay = onPlayAll,
                                onShuffle = onShuffleAll,
                                onSmartShuffle = onSmartShuffleAll,
                                onSave = onSaveList,
                            )
                            if (own != null) {
                                com.aura.ui.library.ListTools(
                                    loaded.songs, onAddSongs = null, Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                                    list = com.aura.ui.library.DownloadedListInfo(loaded.browseId, loaded.title, loaded.subtitle, loaded.thumbnailUrl),
                                    onShare = { own.share() },
                                    privacy = own.privacy,
                                    onPrivacy = { own.choose() },
                                )
                            }
                        } else {
                            Box {
                                HeaderBackdrop(c?.thumbnailUrl.artworkAt(HEADER_ART_PX), Modifier.matchParentSize().pinnedToTop(band), into = tint)
                                Column(
                                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(top = 64.dp, start = 24.dp, end = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (c == null) {
                                        Placeholder(Modifier.size(260.dp), MaterialTheme.shapes.large)
                                        Placeholder(Modifier.padding(top = 20.dp).width(200.dp).height(24.dp))
                                        Placeholder(Modifier.padding(top = 8.dp).width(140.dp).height(16.dp))
                                    } else {
                                        PlaylistHeading(c.title, c.subtitle, c.thumbnailUrl, c.songs, isAlbum)
                                    }
                                    ListPlayRow(
                                        enabled = !c?.songs.isNullOrEmpty(),
                                        saved = saved,
                                        onPlay = onPlayAll,
                                        onShuffle = onShuffleAll,
                                        onSmartShuffle = onSmartShuffleAll,
                                        onSave = onSaveList,
                                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                                    )
                                    if (c != null && own != null) {
                                        com.aura.ui.library.ListTools(
                                            c.songs, onAddSongs = null, Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                            list = com.aura.ui.library.DownloadedListInfo(c.browseId, c.title, c.subtitle, c.thumbnailUrl),
                                            onShare = { own.share() },
                                            privacy = own.privacy,
                                            onPrivacy = { own.choose() },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (firstPage != null && loadingState is UiState.Error) item(key = "retry-pages") {
                        androidx.compose.material3.TextButton(onClick = { loader.reload() }) { Text("Some tracks couldn't load. Retry") }
                    }
                    if (c == null) {
                        items(8) { SongRowPlaceholder() }
                    } else {
                        itemsIndexed(shown, key = { i, song -> "$i:${song.videoId}" }) { i, song ->
                            SongListItem(
                                song = song,
                                onClick = { actions.playAll(shown, i, false, c.title) },
                                isCurrent = song.videoId == actions.currentVideoId,
                                isPlaying = actions.isPlaying,
                                leading = if (isAlbum) {
                                    {
                                        Box(Modifier.size(width = 28.dp, height = 52.dp), Alignment.Center) {
                                            Text(
                                                "${c.songs.indexOf(song).takeIf { it >= 0 }?.plus(1) ?: (i + 1)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (song.videoId == actions.currentVideoId) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                } else {
                                    null
                                },
                                onPlayNext = { actions.playNext(song) },
                                onAddToQueue = { actions.addToQueue(song) },
                            )
                        }
                    }

                }
            }
        }
    }
}
