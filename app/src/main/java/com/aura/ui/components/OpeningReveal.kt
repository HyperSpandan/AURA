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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.ln
import kotlinx.coroutines.delay

/** Plays once per launch of the app, not each time the screen is rebuilt. */
object Opening {
    var played = false
}

/** The splash's black, which the system splash screen and the icon use too. */
val OPENING_FIELD = Color.Black

/**
 * The AURA custom opening sequence:
 * 1. The Luminous Wave takes over from the system splash at the same 240dp scale.
 * 2. An ambient caustic radial aura blooms softly behind the mark, accompanied by
 *    an acoustic dispersion ripple radiating from the crest as light sweeps across
 *    the volumetric ribbon.
 * 3. The mark dilates fluidly as a sonic portal into the app, diving through its
 *    central aperture with fine specular rim lighting and seamless 120 FPS handoff.
 */
@Composable
fun OpeningReveal(onDive: (Float) -> Unit, onDone: () -> Unit) {
    val mark = remember { markPath() }
    val settle = remember { Animatable(0f) }
    val dive = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    val report by rememberUpdatedState(onDive)

    LaunchedEffect(Unit) {
        // Phase 1: Harmonic settle, caustic aura blooming, and acoustic light sweep.
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

        // The system splash draws the 108-unit icon 240dp across; start exactly there.
        val base = 240.dp.toPx() / MARK_VIEWPORT
        val breathe = 1f + 0.038f * kotlin.math.sin(Math.PI.toFloat() * st)
        val s = base * breathe * exp(ln(ZOOM) * d * d)
        val window = smooth(0.04f, 0.28f, d)
        val fade = 1f - smooth(0.78f, 1f, d)

        // 1. Deep black splash backdrop.
        drawRect(OPENING_FIELD, alpha = fade)

        // 2. Ambient Caustic Aura Bloom behind the mark (soft silver & warm bronze halo).
        val glowRadius = 145.dp.toPx() * (0.85f + 0.35f * st + 2.2f * d)
        val glowAlpha = ((st * 0.42f) * (1f - d)).coerceIn(0f, 1f)
        if (glowAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE2E8F0).copy(alpha = 0.38f * glowAlpha),
                        Color(0xFFCBD5E1).copy(alpha = 0.22f * glowAlpha),
                        Color(0xFFD4AF88).copy(alpha = 0.14f * glowAlpha),
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
            val rippleRadius = 55.dp.toPx() + 85.dp.toPx() * rippleProgress
            val rippleAlpha = (1f - rippleProgress) * 0.28f
            drawCircle(
                color = Color.White.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = center,
                style = Stroke(width = 1.3.dp.toPx()),
            )
        }

        // 4. The Luminous Ribbon Wave: light glint, cutout window, and specular rim.
        val rim = Stroke(width = RIM.toPx() / s, join = StrokeJoin.Round)
        translate(center.x - MARK_VIEWPORT / 2f * s, center.y - MARK_VIEWPORT / 2f * s) {
            scale(s, s, pivot = Offset.Zero) {
                // The solid luminous wave.
                drawPath(mark, Color.White, alpha = 1f - window)

                // Iridescent multi-tone light sweep coursing along the ribbon wave.
                if (st in 0.05f..0.98f && d == 0f) {
                    val x = -40f + 190f * st
                    drawPath(
                        mark,
                        Brush.linearGradient(
                            listOf(
                                Color(0x00FFFFFF),
                                Color(0xFFF1F5F9).copy(alpha = 0.95f),
                                Color(0xFFE2D4C5).copy(alpha = 0.90f),
                                Color(0x00FFFFFF),
                            ),
                            start = Offset(x - 24f, 25f),
                            end = Offset(x + 24f, 80f),
                        ),
                        blendMode = BlendMode.SrcAtop,
                    )
                }

                // The portal window cut through onto the app behind.
                if (window > 0f) {
                    drawPath(mark, Color.Black, alpha = window, blendMode = BlendMode.DstOut)
                }

                // Precision specular rim outlining the aperture as it expands.
                if (fade > 0f) {
                    drawPath(mark, Color.White, alpha = fade, style = rim)
                }
            }
        }
    }
}

private fun smooth(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** The width of the white specular edge around the mark. */
private val RIM = 2.4.dp
private const val SETTLE_MS = 580
private const val HOLD_MS = 90L
private const val DIVE_MS = 860
/** How many times its size the mark grows to by the end of the dive. */
private const val ZOOM = 85f
