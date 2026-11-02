@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.aura.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.R
import kotlin.math.pow

/**
 * AURA Refracted Brand Name Component.
 * 100% Native Jetpack Compose implementation calibrated directly to the AURA Brand Identity:
 * - Seamless, continuous left-to-right light flow with organic breathing intervals (zero video-loop pop/jump)
 * - Liquid Titanium Silver, Polished Chrome, Cool Slate, and Pure Radiant White highlights
 * - Staggered character lift entrance (liftDuration = 1.75s, stagger = 0.55, power4.out easing, no opacity fade)
 * - 120 FPS native hardware acceleration with zero WebView dependencies or crashes
 */
@Composable
fun RefractedBrandText(
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    text: String = "AURA",
) {
    val density = LocalDensity.current
    val outfitFont = remember {
        FontFamily(
            Font(
                resId = R.font.outfit_variable,
                weight = FontWeight.Black,
                variationSettings = FontVariation.Settings(FontVariation.weight(900))
            )
        )
    }

    // High-resolution clock synchronized with Android display refresh rate (120Hz on S24)
    var startMillis by remember { mutableLongStateOf(0L) }
    var elapsedSec by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameTime ->
                if (startMillis == 0L) startMillis = frameTime
                elapsedSec = (frameTime - startMillis) / 1000f
            }
        }
    }

    // Character entrance lift parameters
    val liftDuration = 1.75f
    val stagger = 0.55f

    // Seamless, organic traveling glint loop parameters
    // Each cycle is 4.6 seconds:
    // 0.0s - 2.8s: Fluid light beam travels across the word from left to right
    // 2.8s - 4.6s: Calm, serene resting phase in resting Liquid Titanium (beam is fully off-screen)
    // Wrap at 4.6s is mathematically continuous (influence is 0.000 both before and after)
    val loopPeriod = 4.6f
    val travelDuration = 2.8f
    val cycleTime = elapsedSec % loopPeriod

    // Beam center X in normalized word space [0.0 = first letter, 1.0 = last letter]
    val beamCenterX = if (cycleTime < travelDuration) {
        val p = (cycleTime / travelDuration).coerceIn(0f, 1f)
        // Cubic smoothstep (smooth acceleration, cruising, and smooth deceleration)
        val smoothP = p * p * (3f - 2f * p)
        -0.35f + 1.70f * smoothP
    } else {
        2.0f // Fully parked off-screen during the resting breath
    }

    val characters = remember(text) { text.map { it.toString() } }
    val glyphCount = characters.size
    val totalHeightPx = with(density) { height.toPx() }
    val fontSize = (height.value * 0.92f).sp

    Row(
        modifier = modifier
            .height(height)
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        characters.forEachIndexed { index, char ->
            // Character lift physics: power4.out easing, stagger = 0.55, no opacity fade
            val staggerDelay = if (glyphCount > 1) {
                (index.toFloat() / (glyphCount - 1)) * (liftDuration * stagger)
            } else 0f
            val charDuration = liftDuration - (liftDuration * stagger)
            val charElapsed = (elapsedSec - staggerDelay).coerceAtLeast(0f)
            val liftProgress = (charElapsed / charDuration).coerceIn(0f, 1f)
            val easedLift = 1f - (1f - liftProgress).pow(5) // power4.out formula

            // Character starts offset downward and lifts cleanly into place
            val yOffset = with(density) { ((1f - easedLift) * totalHeightPx).toDp() }

            // Spatial beam coordination for each individual letter
            val charNormX = (index + 0.5f) / glyphCount.toFloat()
            val distToBeam = kotlin.math.abs(beamCenterX - charNormX)
            val beamRadius = 0.38f
            // Quadratic falloff: perfectly drops to 0 at boundary
            val beamIntensity = if (distToBeam < beamRadius) {
                (1f - (distToBeam / beamRadius)).pow(2f)
            } else 0f

            // Relative position of beam within this specific glyph [-1.0 = left of glyph, +1.0 = right of glyph]
            val localBeamNorm = ((beamCenterX - charNormX) / beamRadius).coerceIn(-1f, 1f)
            val localCoreX = (localBeamNorm * 0.5f + 0.5f).coerceIn(0.1f, 0.9f)

            // Dynamic Liquid Titanium & Polished Chrome gradient tailored to the incident refracted beam
            val dynamicBodyBrush = remember(beamIntensity, localCoreX) {
                if (beamIntensity > 0.001f) {
                    // Actively illuminated by the passing light beam: radiant chrome with diamond-white core
                    val core = localCoreX
                    val spread = 0.28f
                    val leftEdge = (core - spread).coerceAtLeast(0f)
                    val rightEdge = (core + spread).coerceAtMost(1f)

                    Brush.linearGradient(
                        0.0f to Color(0xFF64748B), // Liquid titanium substrate
                        leftEdge to Color(0xFFCBD5E1), // Approaching platinum
                        (core - 0.04f).coerceAtLeast(0f) to Color(0xFFFAF6F0), // Champagne glint
                        core to Color(0xFFFFFFFF), // Pure diamond-white radiant beam core
                        (core + 0.04f).coerceAtMost(1f) to Color(0xFFFFFFFF),
                        rightEdge to Color(0xFFD4D9E2), // Departing silver
                        1.0f to Color(0xFF475569), // Metallic titanium shadow
                        start = Offset(0f, 0f),
                        end = Offset(totalHeightPx * 0.85f, totalHeightPx)
                    )
                } else {
                    // Pristine resting state: sculpted Liquid Titanium Silver
                    Brush.verticalGradient(
                        0.00f to Color(0xFFFFFFFF), // Crisp upper edge reflection
                        0.18f to Color(0xFFE2E8F0), // Polished platinum
                        0.45f to Color(0xFFCBD5E1), // Liquid titanium silver
                        0.75f to Color(0xFF94A3B8), // Frosted slate
                        1.00f to Color(0xFF475569)  // Deep titanium shadow
                    )
                }
            }

            // Bold stroke expansion brush (gives extraBold = 2.25 weight and beveled silver contour)
            val strokeExpansionBrush = remember(beamIntensity) {
                Brush.verticalGradient(
                    0.0f to Color(0xFFFFFFFF).copy(alpha = 0.98f), // Crisp white top crest
                    0.25f to Color(0xFFE2E8F0).copy(alpha = 0.85f), // Polished platinum
                    0.65f to Color(0xFFA0AAB8).copy(alpha = 0.70f + 0.25f * beamIntensity), // Liquid silver
                    1.0f to Color(0xFF475569).copy(alpha = 0.75f)  // Slate metallic base
                )
            }

            // Specular glass crest reflection
            val crestBrush = remember(beamIntensity) {
                Brush.verticalGradient(
                    0.0f to Color(0xFFFFFFFF),
                    0.40f to Color(0xFFFFFFFF).copy(alpha = 0.40f + 0.50f * beamIntensity),
                    1.0f to Color.Transparent
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clipToBounds(), // Clean physical clipping during lift, exactly like WebGL scissor
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.offset(y = yOffset)
                ) {
                    // Layer 1: Ambient dispersed silver bloom behind glyph (pure luminous white & silver)
                    if (beamIntensity > 0.01f) {
                        Text(
                            text = char,
                            style = TextStyle(
                                fontFamily = outfitFont,
                                fontWeight = FontWeight.Black,
                                fontSize = fontSize,
                                color = Color(0xFFE2E8F0).copy(alpha = 0.35f * beamIntensity),
                                shadow = Shadow(
                                    color = Color(0xFFFFFFFF).copy(alpha = 0.60f * beamIntensity),
                                    offset = Offset(0f, 0f),
                                    blurRadius = with(density) { (14.dp * beamIntensity).toPx() }
                                )
                            )
                        )
                    }

                    // Layer 2: ExtraBold stroke expansion (thickness & solid structural weight)
                    Text(
                        text = char,
                        style = TextStyle(
                            fontFamily = outfitFont,
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            brush = strokeExpansionBrush,
                            drawStyle = Stroke(width = with(density) { 1.8.dp.toPx() })
                        )
                    )

                    // Layer 3: Main refracted liquid titanium glass body (solid fill)
                    Text(
                        text = char,
                        style = TextStyle(
                            fontFamily = outfitFont,
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            brush = dynamicBodyBrush,
                            drawStyle = Fill
                        )
                    )

                    // Layer 4: Glass contour bevel (razor-sharp crest highlight on upper shoulders)
                    Text(
                        text = char,
                        style = TextStyle(
                            fontFamily = outfitFont,
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            brush = crestBrush,
                            drawStyle = Stroke(width = with(density) { 0.75.dp.toPx() })
                        )
                    )
                }
            }
        }
    }
}
