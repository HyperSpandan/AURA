package com.aura.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aura.R
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Plays once per launch of the app, not each time the screen is rebuilt. */
object Opening {
    var played = false
}

/** The splash's black, which the system splash screen and the icon use too. */
val OPENING_FIELD = Color.Black

/**
 * Apple-grade AURA Opening Reveal:
 * 1. Takes over from the system splash seamlessly on pure OLED black (#000000).
 * 2. Phase 1 (Luminous Poise): The 32-bit master obsidian glass tile elevates with
 *    subtle micro-depth (1.00 -> 1.025), wrapped in a multi-stop caustic halo
 *    matching the artwork's titanium silver (#DCE2EA) and champagne bronze (#E8D5C4).
 * 3. A soft, silky specular light sheen caresses the 3D acoustic ribbon contour.
 * 4. Phase 2 (Apple Fluid Spatial Unveil): The overlay dissolves with an authentic
 *    iOS quintic deceleration curve, the mark floats forward (1.025 -> 1.085),
 *    and the app underneath effortlessly steps forward into place at 120 FPS.
 */
@Composable
fun OpeningReveal(onDive: (Float) -> Unit, onDone: () -> Unit) {
    val logoBitmap = ImageBitmap.imageResource(id = R.drawable.splash_logo)
    val mark = remember { markPath() }
    val settle = remember { Animatable(0f) }
    val dive = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    val report by rememberUpdatedState(onDive)

    LaunchedEffect(Unit) {
        // Phase 1: Luminous poise, caustic aura blooming, and specular sheen sweep.
        settle.animateTo(1f, tween(SETTLE_MS, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        delay(HOLD_MS)
        // Phase 2: Apple fluid spatial unveil (spring deceleration).
        dive.animateTo(1f, tween(DIVE_MS, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) { report(value) }
        report(1f)
        done()
    }

    Canvas(Modifier.fillMaxSize()) {
        val d = dive.value
        val st = settle.value

        // Target size of the squircle tile (210dp: crisp golden ratio mobile scale)
        val baseSize = 210.dp.toPx()
        // Apple micro-elevation: gentle float (1.0 -> 1.025 during settle, then lifts to 1.085 during unveil)
        val scale = 1.0f + 0.025f * st + 0.06f * d
        val currentSize = baseSize * scale
        val fade = (1f - d).coerceIn(0f, 1f)

        // 1. OLED Pitch Black Field
        if (fade > 0.002f) {
            drawRect(OPENING_FIELD, alpha = fade)
        }

        // 2. Volumetric Dual-Tone Caustic Aura (Titanium Silver & Champagne Bronze)
        // Wrapped behind the squircle, radiating naturally around its antialiased edges
        val glowRadius = 160.dp.toPx() * (0.92f + 0.28f * st + 1.25f * d)
        val glowAlpha = ((st * 0.45f) * (1f - d * 0.88f)).coerceIn(0f, 1f)
        if (glowAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFDCE2EA).copy(alpha = 0.42f * glowAlpha),
                        Color(0xFF818995).copy(alpha = 0.26f * glowAlpha),
                        Color(0xFFE8D5C4).copy(alpha = 0.20f * glowAlpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )
        }

        // 3. The Photorealistic 32-bit Master Squircle Tile (with transparent subpixel corners)
        val imgLeft = (center.x - currentSize / 2f).roundToInt()
        val imgTop = (center.y - currentSize / 2f).roundToInt()
        val imgSize = currentSize.roundToInt()
        if (imgSize > 0 && fade > 0.005f) {
            drawImage(
                image = logoBitmap,
                dstOffset = IntOffset(imgLeft, imgTop),
                dstSize = IntSize(imgSize, imgSize),
                alpha = fade,
            )
        }

        // 4. Silky Specular Ribbon Gleam (The Apple Caustic Shimmer)
        // Sweeps across the 3D acoustic ribbon curve without any hard lines or polygon tears
        val s = currentSize / MARK_VIEWPORT
        translate(center.x - MARK_VIEWPORT / 2f * s, center.y - MARK_VIEWPORT / 2f * s) {
            scale(s, s, pivot = Offset.Zero) {
                if (st in 0.05f..0.98f && d == 0f) {
                    val shimmerX = -30f + 170f * st
                    drawPath(
                        mark,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFDCE2EA).copy(alpha = 0.55f * fade),
                                Color.White.copy(alpha = 0.75f * fade),
                                Color(0xFFE8D5C4).copy(alpha = 0.60f * fade),
                                Color.Transparent,
                            ),
                            start = Offset(shimmerX - 22f, 25f),
                            end = Offset(shimmerX + 22f, 80f),
                        ),
                    )
                }
            }
        }
    }
}

private const val SETTLE_MS = 560
private const val HOLD_MS = 60L
private const val DIVE_MS = 580
