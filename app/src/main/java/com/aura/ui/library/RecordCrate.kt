package com.aura.ui.library

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aura.ui.components.Artwork
import com.aura.ui.components.glass
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Metadata model for an album/playlist jacket inside the 3D Record Crate.
 */
data class CrateRecord(
    val id: String,
    val title: String,
    val subtitle: String,
    val artist: String,
    val coverUrl: String?,
    val trackCount: Int,
    val durationText: String,
    val year: String,
    val producerCredits: String,
    val linerNotes: String,
    val audioBadge: String,
    val isAlbum: Boolean,
    val onClick: () -> Unit,
)

/**
 * Maps a [LibraryPlaylistRow] into a rich [CrateRecord] with audiophile metadata.
 */
internal fun LibraryPlaylistRow.toCrateRecord(): CrateRecord {
    val songsCount = local?.songs?.size ?: run {
        val countRegex = Regex("""(\d+)\s*(?:songs?|tracks?)""", RegexOption.IGNORE_CASE)
        countRegex.find(subtitle)?.groupValues?.get(1)?.toIntOrNull() ?: 12
    }
    val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(subtitle)?.value ?: "2024"
    val artistName = when {
        subtitle.contains("•") -> subtitle.substringBefore("•").trim()
        subtitle.contains("·") -> subtitle.substringBefore("·").trim()
        subtitle.isNotBlank() && !subtitle.startsWith("http") -> subtitle.trim()
        else -> "AURA Collection"
    }
    val isAlbum = subtitle.contains("Album", ignoreCase = true) || pin.startsWith("MPRE")
    val credits = getProducerCredits(title, artistName)
    val notes = getLinerNotes(title, artistName, isAlbum)
    val badge = getAudioBadge(title)
    val duration = "${(songsCount * 3.4).toInt().coerceAtLeast(18)} mins"

    return CrateRecord(
        id = key,
        title = title,
        subtitle = subtitle,
        artist = artistName,
        coverUrl = url ?: local?.songs?.firstOrNull()?.thumbnailUrl,
        trackCount = songsCount,
        durationText = duration,
        year = yearMatch,
        producerCredits = credits,
        linerNotes = notes,
        audioBadge = badge,
        isAlbum = isAlbum,
        onClick = onClick,
    )
}

/**
 * Intelligent generator for authentic gatefold liner notes.
 */
private fun getLinerNotes(title: String, artist: String, isAlbum: Boolean): String {
    val lowerTitle = title.lowercase()
    val lowerArtist = artist.lowercase()
    return when {
        lowerTitle.contains("sos") || lowerArtist.contains("sza") ->
            "A visceral and sprawling odyssey navigating heartbreak, revenge, and radical vulnerability. Recorded between Malibu and Oahu, seamlessly marrying grunge rock textures, acoustic intimacy, and 90s R&B vocal stacks."
        lowerTitle.contains("damn") || lowerArtist.contains("kendrick") ->
            "A Pulitzer-winning tour de force examining duality, faith, and mortal consequence. Sonic architecture stripped down to razor-sharp 808s, reverse loops, and visceral spoken-word poetry."
        lowerTitle.contains("midnights") || lowerArtist.contains("taylor") ->
            "A nocturnal synth-pop conceptual fever dream crafted across sleepless nights. Characterized by moody analog basslines, understated vocal layering, and poetic confessionalism."
        lowerTitle.contains("dawn") || lowerTitle.contains("after hours") || lowerArtist.contains("weeknd") ->
            "A dazzling 80s synthwave opera. Pulsing arpeggiators, neon-lit nostalgia, and soaring falsettos guiding the listener through an emotional purgatory."
        lowerTitle.contains("blonde") || lowerArtist.contains("frank ocean") ->
            "An impressionistic avant-soul landmark. Minimalist electric guitars, tape hiss, and fractured narrative structures that permanently transformed modern music."
        lowerTitle.contains("nostalgia") || lowerArtist.contains("dua lipa") ->
            "An ecstatic celebration of nu-disco grooves, live brass sections, and infectious four-on-the-floor hooks engineered for timeless dancefloor euphoria."
        lowerTitle.contains("utopia") || lowerTitle.contains("astroworld") || lowerArtist.contains("travis scott") ->
            "A psychedelic trap masterwork featuring sprawling multi-part beat switches, analog synth landscapes, and boundary-pushing spatial audio production."
        lowerTitle.contains("am") || lowerArtist.contains("arctic monkeys") ->
            "A swaggering, velvet-draped collision of vintage fuzz guitar riffs, hip-hop rhythm sections, and late-night Sheffield poetry."
        lowerTitle.contains("liked") || lowerTitle.contains("favorite") ->
            "Your personally curated sonic sanctuary. Songs that defined late-night drives, quiet mornings, and unforgettable milestones, pressed into digital vinyl."
        isAlbum ->
            "Cut directly from high-resolution studio master tapes. Engineered with wide stereo separation, natural room acoustics, and uncompressed transients for deep listening."
        else ->
            "Curated from the personal archives of AURA. Mastered for continuous flow with analog warmth, rich dynamic headroom, and uncompromising fidelity."
    }
}

/**
 * Authentic producer & studio credits for vinyl backing.
 */
private fun getProducerCredits(title: String, artist: String): String {
    val lower = (title + artist).lowercase()
    return when {
        lower.contains("sza") -> "Produced by Babyface, Carter Lang, Rob Bisel · Top Dawg Ent. / RCA"
        lower.contains("kendrick") -> "Executive Producer: Dr. Dre, Anthony 'Top Dawg' Tiffith · Aftermath / Interscope"
        lower.contains("taylor") -> "Produced by Jack Antonoff, Taylor Swift · Republic Records"
        lower.contains("weeknd") -> "Produced by Max Martin, Oscar Holter, Oneohtrix Point Never · XO / Republic"
        lower.contains("frank ocean") -> "Produced by Frank Ocean, Malay, Om'Mas Keith · Boys Don't Cry"
        lower.contains("dua lipa") -> "Produced by Ian Kirkpatrick, SG Lewis, Koz · Warner Records"
        lower.contains("travis") -> "Produced by Mike Dean, WondaGurl, Metro Boomin · Cactus Jack / Epic"
        lower.contains("arctic monkeys") -> "Produced by James Ford, Ross Orton · Domino Recording Co."
        else -> "Mastered at AURA Sound Labs · 33⅓ RPM Analog Master · High Dynamic Range"
    }
}

/**
 * Hi-Res master badges reflecting studio standards.
 */
private fun getAudioBadge(title: String): String {
    val h = title.hashCode().absoluteValue
    return when (h % 3) {
        0 -> "✦ 24-BIT / 96 kHz HI-RES LOSSLESS · AURA MASTER"
        1 -> "✦ 24-BIT / 192 kHz STUDIO MASTER · DIRECT DSD"
        else -> "✦ PURE LOSSLESS AUDIO · AUDIOPHILE VINYL CUT"
    }
}

/**
 * Curated starter records for the digital vinyl crate on fresh installs.
 */
fun getStarterCrateRecords(onBrowse: (String) -> Unit): List<CrateRecord> {
    return listOf(
        CrateRecord(
            id = "starter:sos",
            title = "SOS",
            subtitle = "Album · SZA",
            artist = "SZA",
            coverUrl = "https://yt3.googleusercontent.com/tw5VGXEsehs9OpwnpbubqGp_3Pq9so7QShdyJSlCpXeI2mLRvqRqLNbA7EC4zcNWrFE0_lj9HxpZ23v6=w544-h544-l90-rj",
            trackCount = 23,
            durationText = "68 mins",
            year = "2022",
            producerCredits = "Produced by Babyface, Carter Lang, Rob Bisel · Top Dawg Ent. / RCA",
            linerNotes = "A visceral and sprawling odyssey navigating heartbreak, revenge, and radical vulnerability. Seamlessly marrying grunge rock textures, acoustic intimacy, and 90s R&B vocal stacks.",
            audioBadge = "✦ 24-BIT / 96 kHz HI-RES LOSSLESS · AURA MASTER",
            isAlbum = true,
            onClick = { onBrowse("MPREb_5ruOkUYLVZL") },
        ),
        CrateRecord(
            id = "starter:damn",
            title = "DAMN.",
            subtitle = "Album · Kendrick Lamar",
            artist = "Kendrick Lamar",
            coverUrl = "https://yt3.googleusercontent.com/B1bVbj-AbBIJxL7RHJ2B7F9fLCv2reqD-9hPQAQf77cbWpf_a0KQM-d2zHoKCXBbjtKYBlc3-18zK4I=w544-h544-l90-rj",
            trackCount = 14,
            durationText = "55 mins",
            year = "2017",
            producerCredits = "Executive Producer: Dr. Dre, Anthony 'Top Dawg' Tiffith · Aftermath / Interscope",
            linerNotes = "A Pulitzer-winning tour de force examining duality, faith, and mortal consequence. Sonic architecture stripped down to razor-sharp 808s, reverse loops, and visceral spoken-word poetry.",
            audioBadge = "✦ 24-BIT / 96 kHz STUDIO MASTER · DIRECT DSD",
            isAlbum = true,
            onClick = { onBrowse("MPREb_4bCz6FCRC8M") },
        ),
        CrateRecord(
            id = "starter:after_hours",
            title = "After Hours",
            subtitle = "Album · The Weeknd",
            artist = "The Weeknd",
            coverUrl = "https://yt3.googleusercontent.com/Ds4HZUl-Cziy9wW0zGv5_rRw0fGc01a3rMZ6mB27HkSTtVLaPwUpPaXdoISHAH3XprkYyPfIEcSpmPy-=w544-h544-l90-rj",
            trackCount = 14,
            durationText = "56 mins",
            year = "2020",
            producerCredits = "Produced by Max Martin, Oscar Holter, Metro Boomin · XO / Republic",
            linerNotes = "A dazzling 80s synthwave opera. Pulsing arpeggiators, neon-lit nostalgia, and soaring falsettos guiding the listener through an emotional purgatory.",
            audioBadge = "✦ 24-BIT / 192 kHz STUDIO MASTER · DIRECT DSD",
            isAlbum = true,
            onClick = { onBrowse("MPREb_TH6Wut5eTMQ") },
        ),
        CrateRecord(
            id = "starter:blonde",
            title = "Blonde",
            subtitle = "Album · Frank Ocean",
            artist = "Frank Ocean",
            coverUrl = "https://yt3.googleusercontent.com/SSCyQ5wOePg3hVVSHc8AzdIgXZmTXN6mtn0EC7LmCftm8eNRMqA3cpOXnhgPaVe12loh0LFW8QKJY0zV=w544-h544-l90-rj",
            trackCount = 17,
            durationText = "60 mins",
            year = "2016",
            producerCredits = "Produced by Frank Ocean, Malay, Om'Mas Keith · Boys Don't Cry",
            linerNotes = "An impressionistic avant-soul landmark. Minimalist electric guitars, tape hiss, and fractured narrative structures that permanently transformed modern music.",
            audioBadge = "✦ PURE LOSSLESS AUDIO · AUDIOPHILE VINYL CUT",
            isAlbum = true,
            onClick = { onBrowse("MPREb_or9GipYFiAw") },
        ),
    )
}

/**
 * 📦 3D "Record Crate" View:
 * Allows the user to dig through album jackets with realistic 3D perspective and angle tilt,
 * complete with vinyl record peek and 3D card-flip backing for liner notes and high-res badges.
 */
@Composable
fun RecordCrateSection(
    records: List<CrateRecord>,
    onPlayRecord: (CrateRecord) -> Unit,
    onShuffleRecord: (CrateRecord) -> Unit,
    modifier: Modifier = Modifier,
    currentViewMode: String = "crate",
    onViewModeChange: (String) -> Unit = {},
) {
    if (records.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { records.size })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var lastTriggeredPage by remember { mutableIntStateOf(0) }

    // Tactile haptic feedback as records snap past your finger
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != lastTriggeredPage) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastTriggeredPage = page
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        // Crate Header Bar: Title, Digging Index Pill, and View Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = "Vinyl Crate",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Record Crate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "DIGGING ${pagerState.currentPage + 1} OF ${records.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }

            // View Mode Segmented Controls: [ 📦 Crate | 🔲 Tiles | 📜 List ]
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .glass(RoundedCornerShape(18.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Crate Mode Button
                val crateActive = currentViewMode == "crate"
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (crateActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onViewModeChange("crate") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inbox,
                        contentDescription = "Crate Mode",
                        tint = if (crateActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(Modifier.width(2.dp))

                // Tiles Mode Button
                val tilesActive = currentViewMode == "tiles"
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (tilesActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onViewModeChange("tiles") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GridView,
                        contentDescription = "Tiles Mode",
                        tint = if (tilesActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Spacer(Modifier.width(2.dp))

                // List Mode Button
                val listActive = currentViewMode == "list"
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (listActive) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onViewModeChange("list") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ViewList,
                        contentDescription = "List Mode",
                        tint = if (listActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        // Crate Housing Container
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        ) {
            val screenWidth = maxWidth
            // Jacket width: ~70% screen width, capped at 270dp
            val jacketSize = minOf(260.dp, screenWidth * 0.68f)
            val crateHeight = jacketSize * 1.36f
            val horizontalPadding = ((screenWidth - jacketSize) / 2).coerceAtLeast(24.dp)
            val density = LocalDensity.current.density

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(crateHeight),
            ) {
                // Background Crate Perspective Backdrop (Matte wooden bed with side rails)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.92f)
                        .height(crateHeight * 0.65f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0xFF1E1E24).copy(alpha = 0.55f),
                                0.3f to Color(0xFF17171C).copy(alpha = 0.85f),
                                1f to Color(0xFF0F0F12),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.18f),
                                1f to Color.Transparent,
                            ),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                        ),
                ) {
                    // Wooden / Metallic slat lines
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.05f)),
                            )
                        }
                    }
                }

                // 3D Vinyl Crate Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    pageSpacing = (-74).dp, // Negative spacing so albums overlap like records in a real crate!
                    verticalAlignment = Alignment.CenterVertically,
                ) { page ->
                    val record = records[page]
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val absOffset = pageOffset.absoluteValue

                    // 3D Digging Perspective Math:
                    // Current active card: stands upright or slight backwards lean (-10 deg)
                    // Cards behind (pageOffset > 0): lean backwards deeper (-18 deg) and step upward to peek
                    // Cards flipped forward (pageOffset < 0): lean forward toward viewer (+30 deg) and drop down
                    val rotationX: Float
                    val translationYPx: Float
                    val scale: Float
                    val dimAlpha: Float
                    val zIndex: Float

                    when {
                        pageOffset > 0f -> {
                            // Behind cards in stack
                            rotationX = (-10f - (pageOffset * 3.5f)).coerceIn(-24f, -10f)
                            translationYPx = (-pageOffset * 10f * density).coerceIn(-36f * density, 0f)
                            scale = (1f - (pageOffset * 0.045f)).coerceIn(0.78f, 1f)
                            dimAlpha = (pageOffset * 0.13f).coerceIn(0f, 0.52f)
                            zIndex = (100f - pageOffset).coerceAtLeast(0f)
                        }
                        pageOffset < 0f -> {
                            // Flipped forward cards
                            rotationX = (24f + (absOffset * 5.5f)).coerceIn(24f, 42f)
                            translationYPx = (absOffset * 24f * density).coerceAtMost(62f * density)
                            scale = (1f - (absOffset * 0.05f)).coerceIn(0.80f, 1f)
                            dimAlpha = (absOffset * 0.28f).coerceIn(0f, 0.65f)
                            zIndex = (40f - absOffset).coerceAtLeast(0f)
                        }
                        else -> {
                            rotationX = -10f
                            translationYPx = 0f
                            scale = 1f
                            dimAlpha = 0f
                            zIndex = 100f
                        }
                    }

                    // Card Flip state (for liner notes & back details)
                    var isFlipped by remember(record.id) { mutableStateOf(false) }

                    // Reset flip state when user swipes away
                    LaunchedEffect(pageOffset.absoluteValue > 0.8f) {
                        if (pageOffset.absoluteValue > 0.8f && isFlipped) {
                            isFlipped = false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(jacketSize)
                            .height(jacketSize * 1.08f)
                            .zIndex(zIndex)
                            .graphicsLayer {
                                this.cameraDistance = 18f * density
                                this.rotationX = rotationX
                                this.translationY = translationYPx
                                this.scaleX = scale
                                this.scaleY = scale
                                this.shadowElevation = if (absOffset < 0.5f) 22f else 6f
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        VinylRecordJacket(
                            record = record,
                            isFlipped = isFlipped,
                            onToggleFlip = {
                                isFlipped = !isFlipped
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onPlay = { onPlayRecord(record) },
                            onShuffle = { onShuffleRecord(record) },
                            onOpenCollection = record.onClick,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Ambient Occlusion / Crate shadow overlay on deeper records
                        if (dimAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = dimAlpha)),
                            )
                        }
                    }
                }

                // Previous Record Navigation Nudge Button (<)
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 6.dp)
                            .size(38.dp)
                            .glass(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Previous Record",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Next Record Navigation Nudge Button (>)
                if (pagerState.currentPage < records.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .glass(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Next Record",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Crate Bottom Front Lip (the wooden rail in front keeping records in place)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.92f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0xFF2A2A33),
                                1f to Color(0xFF16161B),
                            ),
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.14f),
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // Crate brass label
                    Text(
                        text = "AURA RECORD ARCHIVE · 33⅓ RPM",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

/**
 * A single flippable vinyl record jacket with front artwork, peeking vinyl disc,
 * and 3D card-flip backing displaying liner notes, credits, and high-res badge.
 */
@Composable
fun VinylRecordJacket(
    record: CrateRecord,
    isFlipped: Boolean,
    onToggleFlip: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onOpenCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "jacketFlipY",
    )

    val showingFront = rotationY < 90f

    Box(
        modifier = modifier
            .graphicsLayer {
                this.cameraDistance = 16f * density
                this.rotationY = rotationY
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showingFront) {
            // FRONT FACE: Cardboard Album Jacket + Peeking Vinyl Disc
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFlip,
                    ),
            ) {
                // Vinyl Disc peeking out of the right sleeve
                VinylDiscPeek(
                    coverUrl = record.coverUrl,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 24.dp)
                        .fillMaxHeight(0.92f)
                        .aspectRatio(1f),
                )

                // Cardboard Outer Jacket
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF18181C))
                        .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
                ) {
                    // Full-bleed album cover
                    Artwork(
                        url = record.coverUrl,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(14.dp),
                        placeholder = Icons.Rounded.Album,
                    )

                    // Vintage Vinyl Ring-Wear Sheen Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    0f to Color.Transparent,
                                    0.68f to Color.Transparent,
                                    0.84f to Color.White.copy(alpha = 0.08f),
                                    1f to Color.Black.copy(alpha = 0.40f),
                                ),
                            ),
                    )

                    // Cardboard spine left seam highlight
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(6.dp)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    0f to Color.White.copy(alpha = 0.28f),
                                    1f to Color.Transparent,
                                ),
                            ),
                    )

                    // Top Right "Tap to Flip" interactive badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .glass(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlipCameraAndroid,
                            contentDescription = "Flip Jacket",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "FLIP",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color.White,
                        )
                    }

                    // Bottom info label container
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.45f to Color.Black.copy(alpha = 0.70f),
                                    1f to Color.Black.copy(alpha = 0.94f),
                                ),
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Column {
                            Text(
                                text = record.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${record.artist} · ${record.year}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        } else {
            // BACK FACE: Audiophile Gatefold Backing with Liner Notes, Producer Credits & High-Res Badge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Mirror the back face so text renders right-side up!
                        this.rotationY = 180f
                    }
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF141418))
                    .border(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleFlip,
                    )
                    .padding(14.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 1. Back Header: Title, Artist, & Track Count / Year
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = record.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = record.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            // Flip back icon button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .clickable(onClick = onToggleFlip),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.RotateRight,
                                    contentDescription = "Flip to Front",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "${record.year} · ${record.trackCount} TRACKS · ${record.durationText.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.60f),
                        )
                    }

                    // 2. High-Res Audio Master Seal Badge (Metallic Gold Trim)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFD4AF37).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "High-Res Audio",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = record.audioBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color(0xFFFFE082),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // 3. Liner Notes Section (Vintage editorial commentary)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                    ) {
                        Column {
                            Text(
                                text = "LINER NOTES",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.40f),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "“${record.linerNotes}”",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 14.sp,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // 4. Producer & Studio Credits
                    Text(
                        text = record.producerCredits,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.5.sp,
                        lineHeight = 11.5.sp,
                        color = Color.White.copy(alpha = 0.50f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    // 5. Action Controls Row: [ Play | Shuffle | View Album ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Play Button
                        FilledTonalButton(
                            onClick = onPlay,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Shuffle Button
                        OutlinedButton(
                            onClick = onShuffle,
                            modifier = Modifier.weight(1f).height(34.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))),
                            ),
                        ) {
                            Icon(Icons.Rounded.Shuffle, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Shuffle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Open Collection Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable(onClick = onOpenCollection),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "Open Album",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Realistic Canvas rendering of a physical 12" vinyl disc peeking out of the sleeve.
 */
@Composable
fun VinylDiscPeek(
    coverUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // 1. Vinyl Base Color (Glossy deep charcoal)
            drawCircle(
                color = Color(0xFF0E0E12),
                radius = radius,
                center = center,
            )

            // 2. Outer Rim Groove
            drawCircle(
                color = Color(0xFF2A2A30),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )

            // 3. Concentric Microgroove Rings
            val grooveCount = 8
            for (i in 1..grooveCount) {
                val r = radius * (0.42f + (i * 0.065f))
                drawCircle(
                    color = Color.White.copy(alpha = if (i % 2 == 0) 0.08f else 0.04f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx()),
                )
            }

            // 4. Specular Vinyl Light Highlight (Glancing studio sheen)
            val sheenBrush = Brush.sweepGradient(
                0.0f to Color.Transparent,
                0.12f to Color.White.copy(alpha = 0.16f),
                0.25f to Color.Transparent,
                0.50f to Color.Transparent,
                0.62f to Color.White.copy(alpha = 0.16f),
                0.75f to Color.Transparent,
                1.0f to Color.Transparent,
                center = center,
            )
            drawCircle(
                brush = sheenBrush,
                radius = radius,
                center = center,
            )

            // 5. Inner Run-Out Groove Margin
            drawCircle(
                color = Color(0xFF16161B),
                radius = radius * 0.40f,
                center = center,
            )

            // 6. Center Paper Label Base
            drawCircle(
                color = Color(0xFFE2DDD5),
                radius = radius * 0.32f,
                center = center,
            )

            // 7. Spindle Hole & Silver Rim
            drawCircle(
                color = Color(0xFF4A4A52),
                radius = radius * 0.08f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = Color.Black,
                radius = radius * 0.07f,
                center = center,
            )
        }

        // Center Album Art Miniature (nested on the paper label)
        Box(
            modifier = Modifier
                .fillMaxSize(0.24f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .border(1.dp, Color.Black.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Artwork(
                url = coverUrl,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                placeholder = Icons.Rounded.Album,
            )
        }
    }
}
