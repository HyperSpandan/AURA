package com.aura.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import com.aura.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Plays once per launch of the app, not each time the screen is rebuilt. */
object Opening {
    var played = false
}

/** The splash's black, matching the system splash screen and device bezels. */
val OPENING_FIELD = Color.Black

/**
 * Apple-grade AURA Opening Reveal with Calibrated Organic Illumination:
 *
 * 1. Poised Continuity:
 *    Seamlessly takes over from the OS splash screen on pure OLED black (#000000).
 *
 * 2. Ambient Sunrise (0 – 1100ms):
 *    The ambient caustic halo wakes up with a soft, gradual breath, blooming around the
 *    rounded glass corners with titanium silver (#DCE2EA, #818995) and champagne bronze (#E8D5C4).
 *
 * 3. Silky S-Curve Illumination Sweep (180 – 1280ms, 1100ms duration):
 *    Uses an organic S-curve [CubicBezierEasing(0.35f, 0.0f, 0.25f, 1.0f)].
 *    A wide, 84-unit feathered specular beam glides steadily across the 3D acoustic ribbon
 *    from bottom-left to top-right, creating a luxurious, continuous caress with ZERO flash/stutter.
 *
 * 4. Hold of Clarity (1280 – 1420ms, 140ms duration):
 *    Poised stillness where the illuminated mark is fully visible in all its glory.
 *
 * 5. Apple Fluid Spatial Unveil (1420 – 2100ms, 680ms duration):
 *    - Overlay dissolves with iOS quintic spring deceleration (cubic-bezier(0.16, 1, 0.3, 1)).
 *    - Logo elevates toward the camera (1.025 -> 1.085).
 *    - Destination app UI steps forward smoothly from 0.96 -> 1.00.
 */
@Composable
fun OpeningReveal(onDive: (Float) -> Unit, onDone: () -> Unit) {
    val logoBitmap = ImageBitmap.imageResource(id = R.drawable.splash_logo)
    val mark = remember { markPath() }
    val glow = remember { Animatable(0f) }
    val elevation = remember { Animatable(0f) }
    val shimmer = remember { Animatable(0f) }
    val dive = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    val report by rememberUpdatedState(onDive)

    // Pre-allocated static brush for the caustic halo (0 allocations during animation)
    val causticBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFDCE2EA).copy(alpha = 0.48f),
                Color(0xFF818995).copy(alpha = 0.30f),
                Color(0xFFE8D5C4).copy(alpha = 0.22f),
                Color.Transparent,
            )
        )
    }

    LaunchedEffect(Unit) {
        // 1. Ambient caustic halo breathes in smoothly (organic S-curve, 1100ms)
        launch {
            glow.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)))
        }
        // 2. Micro elevation begins gently (1300ms)
        launch {
            elevation.animateTo(1f, tween(1300, easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)))
        }
        // 3. Silky specular ribbon illumination starts after halo is softly established
        delay(180L)
        shimmer.animateTo(1f, tween(1100, easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)))

        // 4. Brief hold of poise where the mark is fully illuminated
        delay(140L)

        // 5. Apple fluid spatial unveil into the app
        dive.animateTo(1f, tween(680, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) { report(value) }
        report(1f)
        done()
    }

    // Root overlay: Pure OLED black, dissolving on GPU RenderNode
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OPENING_FIELD)
            .graphicsLayer {
                alpha = (1f - dive.value).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        // 1. Volumetric Dual-Tone Caustic Halo (Titanium Silver & Champagne Bronze)
        // Radiates naturally around the rounded squircle curves without square clipping
        Box(
            modifier = Modifier
                .size(380.dp)
                .graphicsLayer {
                    val gw = glow.value
                    val d = dive.value
                    val s = 0.88f + 0.26f * gw + 0.85f * d
                    scaleX = s
                    scaleY = s
                    alpha = ((gw * 0.58f) * (1f - d * 0.85f)).coerceIn(0f, 1f)
                }
                .background(causticBrush, shape = CircleShape),
        )

        // 2. The Photorealistic 32-bit Master Squircle Tile
        // GPU texture quad with bilinear filtering (0 CPU software scaling)
        Image(
            bitmap = logoBitmap,
            contentDescription = "AURA",
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer {
                    val el = elevation.value
                    val d = dive.value
                    // Apple physical elevation: 1.00 -> 1.025 during settle, then lifts to 1.085 during unveil
                    val s = 1.0f + 0.025f * el + 0.06f * d
                    scaleX = s
                    scaleY = s
                },
        )

        // 3. Silky Specular Ribbon Gleam (The Apple Caustic Shimmer)
        // Broad 84-unit feathered beam gliding across the 3D acoustic ribbon
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer {
                    val el = elevation.value
                    val d = dive.value
                    val s = 1.0f + 0.025f * el + 0.06f * d
                    scaleX = s
                    scaleY = s
                    alpha = if (d > 0.02f) (1f - d * 3.5f).coerceIn(0f, 1f) else 1f
                },
        ) {
            val sh = shimmer.value
            if (sh in 0.01f..0.99f) {
                val s = size.width / MARK_VIEWPORT
                translate(size.width / 2f - MARK_VIEWPORT / 2f * s, size.height / 2f - MARK_VIEWPORT / 2f * s) {
                    scale(s, s, pivot = Offset.Zero) {
                        val shimmerX = -50f + 210f * sh
                        drawPath(
                            mark,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFDCE2EA).copy(alpha = 0.45f),
                                    Color.White.copy(alpha = 0.85f),
                                    Color(0xFFE8D5C4).copy(alpha = 0.55f),
                                    Color.Transparent,
                                ),
                                start = Offset(shimmerX - 42f, 20f),
                                end = Offset(shimmerX + 42f, 85f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
