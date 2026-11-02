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
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * AURA Refracted Brand Name Component.
 * 100% Native Jetpack Compose implementation calibrated directly to the AURA Brand Identity:
 * - Liquid Titanium Silver, Polished Chrome, Cool Slate, and Pure Radiant White highlights
 * - Zero discordant electric blues: pure metallic silver & frosted platinum harmony matching the 3D acoustic ribbon logo
 * - Staggered character lift entrance (liftDuration = 1.75s, stagger = 0.55, power4.out easing, no opacity fade)
 * - Left-to-right light flow with opening travel (6.9s) and continuous harmonic glint cycle (13.2s)
 * - Beveled glass contour highlights and razor-sharp specular crest reflections
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

    // ReactDrops configuration constants
    val liftDuration = 1.75f
    val stagger = 0.55f
    val openingDuration = 6.9f
    val duration = 13.2f

    // Shared beam optical flow across the entire word
    val beamProgress = (elapsedSec / openingDuration).coerceIn(0f, 1f)
    val openingTravel = 1f - (1f - beamProgress).pow(4.5f)
    val cycleTime = if (elapsedSec > openingDuration) (elapsedSec - openingDuration) % duration else 0f
    val cyclePhase = (cycleTime / duration) * 2f * PI.toFloat()
    val settledGlint = 0.5f + 0.35f * sin(cyclePhase)

    // Beam center X in normalized [0, 1] word space
    val beamCenterX = if (elapsedSec < openingDuration) {
        -0.25f + 1.5f * openingTravel
    } else {
        settledGlint
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

            // Character starts offset downward and lifts into place
            val yOffset = with(density) { ((1f - easedLift) * totalHeightPx).toDp() }

            // Spatial beam coordination for each individual letter
            val charNormX = (index + 0.5f) / glyphCount.toFloat()
            val distToBeam = kotlin.math.abs(beamCenterX - charNormX)
            val beamIntensity = (1f - (distToBeam / 0.5f).coerceIn(0f, 1f)).pow(1.6f)

            // Dynamic Liquid Titanium & Polished Chrome gradient matching the AURA brand ribbon
            val dynamicBodyBrush = remember(beamCenterX, distToBeam) {
                val shift = (beamCenterX - charNormX) * 140f
                Brush.linearGradient(
                    0.00f to Color(0xFF475569), // Deep metallic slate edge
                    0.18f to Color(0xFF64748B), // Liquid titanium substrate
                    0.35f to Color(0xFF94A3B8), // Metallic silver-grey
                    0.44f to Color(0xFFCBD5E1), // Polished platinum silver
                    0.50f to Color(0xFFFFFFFF), // Pure radiant white specular core
                    0.56f to Color(0xFFF1EBE4), // Champagne platinum glint
                    0.65f to Color(0xFFD4D9E2), // Lustrous silver
                    0.82f to Color(0xFF89909C), // Frosted titanium
                    1.00f to Color(0xFF334155), // Graphite titanium shadow
                    start = Offset(-shift, 0f),
                    end = Offset(totalHeightPx * 2.8f - shift, totalHeightPx)
                )
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
                    0.45f to Color(0xFFFFFFFF).copy(alpha = 0.45f * beamIntensity),
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
                    Text(
                        text = char,
                        style = TextStyle(
                            fontFamily = outfitFont,
                            fontWeight = FontWeight.Black,
                            fontSize = fontSize,
                            color = Color(0xFFCBD5E1).copy(alpha = 0.35f * beamIntensity),
                            shadow = Shadow(
                                color = Color(0xFFFFFFFF).copy(alpha = 0.50f * beamIntensity),
                                offset = Offset(0f, 0f),
                                blurRadius = with(density) { (12.dp * beamIntensity).toPx() }
                            )
                        )
                    )

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
