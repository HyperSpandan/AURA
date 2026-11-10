package com.aura.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.aura.data.model.HEADER_ART_PX
import com.aura.data.model.artworkAt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Specification for an in-flight Shared Element Hero Morph transition
 * from a 3D Cover Flow card to the full-screen album header.
 */
data class HeroMorphSpec(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String?,
    val initialBounds: Rect,
    val initialCornerRadius: Dp = 26.dp,
    val onNavigate: () -> Unit,
)

/**
 * Coordinates the shared element hero expansion between the 3D Cover Flow
 * cards and the full-bleed destination collection screen.
 */
class HeroMorphState(val scope: CoroutineScope) {
    var activeSpec by mutableStateOf<HeroMorphSpec?>(null)
        private set

    val progress = Animatable(0f)
    var isMorphing by mutableStateOf(false)
        private set

    fun launch(
        browseId: String,
        title: String,
        subtitle: String,
        thumbnailUrl: String?,
        bounds: Rect,
        initialCornerRadius: Dp = 26.dp,
        onNavigate: () -> Unit,
    ) {
        if (isMorphing || bounds == Rect.Zero) {
            onNavigate()
            return
        }

        val spec = HeroMorphSpec(
            browseId = browseId,
            title = title,
            subtitle = subtitle,
            thumbnailUrl = thumbnailUrl,
            initialBounds = bounds,
            initialCornerRadius = initialCornerRadius,
            onNavigate = onNavigate,
        )
        activeSpec = spec
        isMorphing = true

        scope.launch {
            progress.snapTo(0f)
            // Fluid spring physics: fast responsive expansion with smooth, organic settle
            val animJob = launch {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 380f,
                    ),
                )
            }

            // Immediately trigger background navigation so destination screen loads behind the morph
            onNavigate()

            animJob.join()

            // Gracefully finish morph and clear overlay
            progress.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
            )
            activeSpec = null
            isMorphing = false
            progress.snapTo(0f)
        }
    }
}

val LocalHeroMorph = compositionLocalOf<HeroMorphState?> { null }

@Composable
fun rememberHeroMorphState(): HeroMorphState {
    val scope = rememberCoroutineScope()
    return remember(scope) { HeroMorphState(scope) }
}

/**
 * Overlay placed over the App root that smoothly expands the 3D Cover Flow card
 * into the full-bleed album header with realistic fluid physics.
 */
@Composable
fun HeroMorphOverlay(
    state: HeroMorphState,
    modifier: Modifier = Modifier,
) {
    val spec = state.activeSpec ?: return
    val progress = state.progress.value.coerceIn(0f, 1.05f)
    if (progress > 1.02f) return

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Calculate destination hero bounds (full bleed header dimensions)
    val destWidthPx = with(density) { screenWidth.toPx() }
    val destHeightPx = with(density) {
        val heroHeight = (screenHeight * 0.72f).coerceIn(600.dp, 760.dp)
        heroHeight.toPx()
    }

    val p = progress.coerceAtMost(1f)

    // Fluid interpolated geometry
    val currentLeft = lerp(spec.initialBounds.left, 0f, p)
    val currentTop = lerp(spec.initialBounds.top, 0f, p)
    val currentWidth = lerp(spec.initialBounds.width, destWidthPx, p)
    val currentHeight = lerp(spec.initialBounds.height, destHeightPx, p)
    val currentCornerRadius = lerp(spec.initialCornerRadius, 0.dp, p)
    val currentElevation = lerp(26.dp, 0.dp, p)

    // Alpha fade out when completing handoff to live screen
    val containerAlpha = if (progress > 0.95f) (1f - (progress - 0.95f) * 10f).coerceIn(0f, 1f) else 1f

    val currentWidthDp = with(density) { currentWidth.toDp() }
    val currentHeightDp = with(density) { currentHeight.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(999f)
            .graphicsLayer { this.alpha = containerAlpha },
    ) {
        // Backing scrim as card expands
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (p * 0.75f).coerceIn(0f, 0.75f))),
        )

        // The Morphing Hero Card
        Box(
            modifier = Modifier
                .offset { IntOffset(currentLeft.roundToInt(), currentTop.roundToInt()) }
                .size(currentWidthDp, currentHeightDp)
                .shadow(currentElevation, RoundedCornerShape(currentCornerRadius), clip = false)
                .clip(RoundedCornerShape(currentCornerRadius))
                .background(Color(0xFF141418)),
        ) {
            // Full-bleed artwork
            Artwork(
                spec.thumbnailUrl.artworkAt(HEADER_ART_PX),
                modifier = Modifier.fillMaxSize(),
                shape = androidx.compose.ui.graphics.RectangleShape,
                placeholder = Icons.Filled.Album,
            )

            // Top Status Bar Scrim (unfolds as card expands)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .alpha(p)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.65f),
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            1f to Color.Transparent,
                        ),
                    ),
            )

            // Dynamic Ambient Dark Gradient for bottom text contrast
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.40f to Color.Transparent,
                            0.70f to Color.Black.copy(alpha = lerp(0.40f, 0.50f, p)),
                            1f to Color.Black.copy(alpha = lerp(0.80f, 0.95f, p)),
                        ),
                    ),
            )

            // Unfolding Text & Action Controls
            val padHorizontal = lerp(14.dp, 24.dp, p)
            val padBottom = lerp(14.dp, 20.dp, p)
            val isUnfolded = p > 0.55f

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = padHorizontal, vertical = padBottom),
                horizontalAlignment = if (isUnfolded) Alignment.CenterHorizontally else Alignment.CenterHorizontally,
            ) {
                // If in initial card form, render frosted glass container; as it unfolds, dissolve container border
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (p < 0.7f) {
                                Modifier
                                    .clip(RoundedCornerShape(lerp(20.dp, 0.dp, p / 0.7f)))
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Black.copy(alpha = lerp(0.55f, 0f, p / 0.7f)),
                                            1f to Color.Black.copy(alpha = lerp(0.85f, 0f, p / 0.7f)),
                                        ),
                                    )
                                    .padding(vertical = lerp(12.dp, 4.dp, p / 0.7f), horizontal = lerp(10.dp, 0.dp, p / 0.7f))
                            } else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = spec.title,
                            style = if (isUnfolded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (spec.subtitle.isNotBlank()) {
                            val artist = spec.subtitle.split("•").firstOrNull()?.trim()?.ifBlank { spec.subtitle } ?: spec.subtitle
                            Text(
                                text = artist,
                                style = if (isUnfolded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = lerp(0.82f, 0.92f, p)),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Action buttons blossoming into view as hero finishes expanding (Play / Shuffle pills)
                if (p > 0.6f) {
                    val actionProgress = ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .graphicsLayer {
                                scaleX = actionProgress
                                scaleY = actionProgress
                                this.alpha = actionProgress
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Play Pill
                        Row(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Play",
                                color = Color.Black,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        // Shuffle Pill
                        Row(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.20f))
                                .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Shuffle",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}
