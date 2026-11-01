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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aura.R
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Plays once per launch of the app, not each time the screen is rebuilt. */
object Opening {
    var played = false
}

/** The splash's black, which the system splash screen and the icon use too. */
val OPENING_FIELD = Color.Black

/**
 * The AURA custom opening sequence:
 * 1. Takes over from the system splash at the same 240dp scale, displaying the exact
 *    photorealistic master logo in full 32-bit color (smoked obsidian glass tile,
 *    caustic translucent ribbon, warm bronze and cool silver rim refractions).
 * 2. An ambient dual-tone caustic aura blooms softly behind the mark, matching the
 *    warm amber (#E8D5C4) and cool silver-blue (#DCE2EA) hues of the artwork.
 * 3. An iridescent multi-tone light sweep courses along the wave's 3D curvature.
 * 4. The mark dilates fluidly as a sonic portal into the app, diving through its
 *    central aperture with dual-tone specular rim lighting and seamless 120 FPS handoff.
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
        // Phase 1: Harmonic settle, dual-tone aura blooming, and prismatic light shimmer.
        settle.animateTo(1f, tween(SETTLE_MS, easing = CubicBezierEasing(0.18f, 0f, 0f, 1f)))
        delay(HOLD_MS)
        // Phase 2: Fluid sonic portal expansion.
        dive.animateTo(1f, tween(DIVE_MS, easing = CubicBezierEasing(0.55f, 0f, 0.8f, 0.35f))) { report(value) }
        report(1f)
        done()
    }

    Canvas(
        Modifier
            .fillMaxSize()
            // Own offscreen layer so the window cut through it reveals the app below.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
    ) {
        val d = dive.value
        val st = settle.value

        // Target size of the icon on screen
        val baseSize = 240.dp.toPx()
        val breathe = 1f + 0.038f * kotlin.math.sin(Math.PI.toFloat() * st)
        val currentSize = baseSize * breathe * exp(ln(ZOOM) * d * d)
        val window = smooth(0.04f, 0.28f, d)
        val fade = 1f - smooth(0.78f, 1f, d)

        // 1. Deep black splash backdrop.
        drawRect(OPENING_FIELD, alpha = fade)

        // 2. Ambient Dual-Tone Caustic Aura Bloom matching the logo's real colors
        // (cool silver #DCE2EA, cool slate #818995, and warm amber-bronze #E8D5C4).
        val glowRadius = 150.dp.toPx() * (0.85f + 0.35f * st + 2.2f * d)
        val glowAlpha = ((st * 0.45f) * (1f - d)).coerceIn(0f, 1f)
        if (glowAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFDCE2EA).copy(alpha = 0.42f * glowAlpha),
                        Color(0xFF818995).copy(alpha = 0.26f * glowAlpha),
                        Color(0xFFE8D5C4).copy(alpha = 0.18f * glowAlpha),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = center,
            )
        }

        // 3. Acoustic dispersion ripple radiating outward from the wave crest.
        if (st > 0.15f && d == 0f) {
            val rippleProgress = ((st - 0.15f) / 0.85f).coerceIn(0f, 1f)
            val rippleRadius = 60.dp.toPx() + 85.dp.toPx() * rippleProgress
            val rippleAlpha = (1f - rippleProgress) * 0.25f
            drawCircle(
                color = Color(0xFFDCE2EA).copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = center,
                style = Stroke(width = 1.3.dp.toPx()),
            )
        }

        // 4. The Master Logo (Photorealistic 32-bit render with full volumetric depth & rim lights).
        val imgLeft = (center.x - currentSize / 2f).roundToInt()
        val imgTop = (center.y - currentSize / 2f).roundToInt()
        val imgSize = currentSize.roundToInt()
        if (imgSize > 0 && (1f - window) > 0.005f) {
            drawImage(
                image = logoBitmap,
                dstOffset = IntOffset(imgLeft, imgTop),
                dstSize = IntSize(imgSize, imgSize),
                alpha = 1f - window,
            )
        }

        // 5. Iridescent dual-tone light sweep coursing along the ribbon wave.
        val s = currentSize / MARK_VIEWPORT
        val rim = Stroke(width = RIM.toPx() / s, join = StrokeJoin.Round)
        translate(center.x - MARK_VIEWPORT / 2f * s, center.y - MARK_VIEWPORT / 2f * s) {
            scale(s, s, pivot = Offset.Zero) {
                if (st in 0.05f..0.98f && d == 0f) {
                    val x = -40f + 190f * st
                    drawPath(
                        mark,
                        Brush.linearGradient(
                            listOf(
                                Color(0x00FFFFFF),
                                Color(0xFFDCE2EA).copy(alpha = 0.85f),
                                Color(0xFFE8D5C4).copy(alpha = 0.90f),
                                Color(0x00FFFFFF),
                            ),
                            start = Offset(x - 24f, 25f),
                            end = Offset(x + 24f, 80f),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                }

                // 6. The portal window cut through onto the app behind.
                if (window > 0f) {
                    drawPath(mark, Color.Black, alpha = window, blendMode = BlendMode.DstOut)
                }

                // 7. Dual-tone specular rim outlining the aperture as it expands.
                if (fade > 0f) {
                    drawPath(
                        mark,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFDCE2EA).copy(alpha = fade),
                                Color(0xFFE8D5C4).copy(alpha = fade),
                            ),
                            start = Offset(20f, 70f),
                            end = Offset(85f, 35f),
                        ),
                        style = rim,
                    )
                }
            }
        }
    }
}

private fun smooth(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** The width of the specular edge around the mark. */
private val RIM = 2.4.dp
private const val SETTLE_MS = 580
private const val HOLD_MS = 90L
private const val DIVE_MS = 860
/** How many times its size the mark grows to by the end of the dive. */
private const val ZOOM = 85f
