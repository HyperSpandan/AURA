package com.aura.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.aura.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * AURA Refracted Brand Name Component.
 * 100% Native Jetpack Compose implementation calibrated directly to the AURA Brand Identity:
 * - Single continuous canvas-space light wave flowing seamlessly across the entire word (zero per-letter step lighting)
 * - Liquid Titanium Silver, Polished Chrome, Cool Slate, and Pure Radiant White highlights
 * - Staggered character lift entrance (liftDuration = 1.75s, stagger = 0.55, power4.out easing, no opacity fade)
 * - Buttery-smooth 120 FPS hardware-accelerated Skia rendering with zero WebView dependencies or crashes
 */
@Composable
fun RefractedBrandText(
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    text: String = "AURA",
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    // Load Outfit font with heavy architectural weight (900 Black)
    val outfitTypeface = remember {
        try {
            val baseFont = ResourcesCompat.getFont(context, R.font.outfit_variable)
            if (baseFont != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Typeface.create(baseFont, 900, false)
                } else {
                    Typeface.create(baseFont, Typeface.BOLD)
                }
            } else {
                Typeface.create(Typeface.SANS_SERIF, 900, false)
            }
        } catch (e: Exception) {
            Typeface.create(Typeface.SANS_SERIF, 900, false)
        }
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

    val heightPx = with(density) { height.toPx() }
    val fontSizePx = heightPx * 0.94f
    val letterSpacingPx = with(density) { 5.dp.toPx() }

    // Reusable Skia Paint objects
    val bodyPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            style = Paint.Style.FILL
            typeface = outfitTypeface
            textSize = fontSizePx
        }
    }

    val strokeExpansionPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            style = Paint.Style.STROKE
            strokeWidth = with(density) { 2.0.dp.toPx() }
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            typeface = outfitTypeface
            textSize = fontSizePx
        }
    }

    val crestPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            style = Paint.Style.STROKE
            strokeWidth = with(density) { 0.85.dp.toPx() }
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            typeface = outfitTypeface
            textSize = fontSizePx
        }
    }

    val bloomPaint = remember {
        Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            style = Paint.Style.FILL
            typeface = outfitTypeface
            textSize = fontSizePx
        }
    }

    // Measure character advances to position glyphs with letterSpacing
    val characters = remember(text) { text.map { it.toString() } }
    val glyphCount = characters.size
    val charWidths = remember(text, fontSizePx) {
        FloatArray(glyphCount) { i -> bodyPaint.measureText(characters[i]) }
    }
    val charPositions = remember(text, fontSizePx, letterSpacingPx) {
        var x = 0f
        FloatArray(glyphCount) { i ->
            val pos = x
            x += charWidths[i] + letterSpacingPx
            pos
        }
    }
    val totalWordWidth = remember(charPositions, charWidths) {
        if (glyphCount > 0) charPositions.last() + charWidths.last() else 0f
    }
    val totalWordWidthDp = with(density) { totalWordWidth.toDp() }

    // Vertical baseline calculation
    val fontMetrics = bodyPaint.fontMetrics
    val baselineY = (heightPx - (fontMetrics.descent + fontMetrics.ascent)) / 2f

    // Character entrance lift parameters
    val liftDuration = 1.75f
    val stagger = 0.55f

    // Seamless, organic traveling light flow across the ENTIRE word:
    // Continuous 4.5s cycle: 2.6s smooth sweep across the word + 1.9s resting breath
    val loopPeriod = 4.5f
    val sweepDuration = 2.6f
    val cycleTime = elapsedSec % loopPeriod

    // Width of the traveling light beam in word coordinates
    val beamWidth = totalWordWidth * 0.72f
    val startX = -beamWidth * 1.1f
    val endX = totalWordWidth + beamWidth * 1.1f

    // Single unified beam position in global word coordinates (pixels)
    val beamCenterPx = if (cycleTime < sweepDuration) {
        val p = (cycleTime / sweepDuration).coerceIn(0f, 1f)
        val smoothP = p * p * (3f - 2f * p) // Smoothstep easing
        startX + (endX - startX) * smoothP
    } else {
        endX + 200f // Completely parked off-screen during the serene rest
    }

    // Diagonal beam geometry (25 degree sheen)
    val angleRad = 25f * (PI.toFloat() / 180f)
    val dx = cos(angleRad) * (beamWidth / 2f)
    val dy = sin(angleRad) * (heightPx / 2f)

    // Palette: Liquid Titanium Silver, Polished Chrome, Cool Slate, Pure White
    val substrateColor = AndroidColor.rgb(0x64, 0x74, 0x8B) // Slate titanium resting body
    val chromeLight = AndroidColor.rgb(0xCB, 0xD5, 0xE1)    // Approaching platinum
    val pureWhite = AndroidColor.rgb(0xFF, 0xFF, 0xFF)      // Diamond radiant core
    val champagneGlint = AndroidColor.rgb(0xF5, 0xEF, 0xEB) // Warm champagne glint
    val platinumEdge = AndroidColor.rgb(0xD4, 0xD9, 0xE2)   // Departing silver
    val deepShadow = AndroidColor.rgb(0x47, 0x55, 0x69)     // Shadow base

    val gradientColors = intArrayOf(
        substrateColor,
        chromeLight,
        champagneGlint,
        pureWhite,
        pureWhite,
        platinumEdge,
        substrateColor
    )
    val gradientStops = floatArrayOf(
        0.00f,
        0.28f,
        0.44f,
        0.49f,
        0.53f,
        0.72f,
        1.00f
    )

    // Compute whether the beam is currently touching any part of the word
    val beamLeftEdge = beamCenterPx - dx
    val beamRightEdge = beamCenterPx + dx
    val beamTouchingWord = beamRightEdge > 0f && beamLeftEdge < totalWordWidth

    // One shared word-space LinearGradient for the entire text
    val unifiedBodyShader = if (beamTouchingWord) {
        LinearGradient(
            beamCenterPx - dx, -dy,
            beamCenterPx + dx, heightPx + dy,
            gradientColors,
            gradientStops,
            Shader.TileMode.CLAMP
        )
    } else {
        // Resting Liquid Titanium Silver vertical gradient
        LinearGradient(
            0f, 0f, 0f, heightPx,
            intArrayOf(
                AndroidColor.rgb(0xFF, 0xFF, 0xFF), // Upper crest reflection
                AndroidColor.rgb(0xE2, 0xE8, 0xF0), // Polished platinum
                AndroidColor.rgb(0xCB, 0xD5, 0xE1), // Liquid titanium silver
                AndroidColor.rgb(0x94, 0xA3, 0xB8), // Frosted slate
                AndroidColor.rgb(0x47, 0x55, 0x69)  // Deep titanium shadow
            ),
            floatArrayOf(0.00f, 0.20f, 0.50f, 0.78f, 1.00f),
            Shader.TileMode.CLAMP
        )
    }

    // Stroke expansion bevel gradient (vertical polished chrome)
    val strokeShader = LinearGradient(
        0f, 0f, 0f, heightPx,
        intArrayOf(
            AndroidColor.argb(0xFA, 0xFF, 0xFF, 0xFF), // Pure white top crest
            AndroidColor.argb(0xD8, 0xE2, 0xE8, 0xF0), // Bright platinum
            AndroidColor.argb(0xA0, 0xA0, 0xAA, 0xB8), // Liquid silver
            AndroidColor.argb(0xB0, 0x47, 0x55, 0x69)  // Slate metallic base
        ),
        floatArrayOf(0.00f, 0.25f, 0.65f, 1.00f),
        Shader.TileMode.CLAMP
    )

    // Specular crest highlight gradient (razor-sharp top rim)
    val crestShader = LinearGradient(
        0f, 0f, 0f, heightPx * 0.45f,
        intArrayOf(
            AndroidColor.argb(0xFF, 0xFF, 0xFF, 0xFF),
            AndroidColor.argb(0x00, 0xFF, 0xFF, 0xFF)
        ),
        floatArrayOf(0.00f, 1.00f),
        Shader.TileMode.CLAMP
    )

    bodyPaint.shader = unifiedBodyShader
    strokeExpansionPaint.shader = strokeShader
    crestPaint.shader = crestShader

    // Ambient bloom parameters
    if (beamTouchingWord) {
        val wordCenterPx = totalWordWidth / 2f
        val distToWordCenter = kotlin.math.abs(beamCenterPx - wordCenterPx)
        val maxDist = totalWordWidth / 2f + beamWidth / 2f
        val bloomAlpha = ((1f - (distToWordCenter / maxDist).coerceIn(0f, 1f)).pow(1.5f) * 110f).toInt()
        bloomPaint.color = AndroidColor.argb(bloomAlpha, 0xCB, 0xD5, 0xE1)
        bloomPaint.setShadowLayer(
            with(density) { 14.dp.toPx() },
            0f, 0f,
            AndroidColor.argb((bloomAlpha * 1.4f).toInt().coerceAtMost(255), 0xFF, 0xFF, 0xFF)
        )
    }

    Canvas(
        modifier = modifier
            .height(height)
            .width(totalWordWidthDp)
    ) {
        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas

            for (i in 0 until glyphCount) {
                // Character lift entrance physics
                val staggerDelay = if (glyphCount > 1) {
                    (i.toFloat() / (glyphCount - 1)) * (liftDuration * stagger)
                } else 0f
                val charDuration = liftDuration - (liftDuration * stagger)
                val charElapsed = (elapsedSec - staggerDelay).coerceAtLeast(0f)
                val liftProgress = (charElapsed / charDuration).coerceIn(0f, 1f)
                val easedLift = 1f - (1f - liftProgress).pow(5) // power4.out formula

                // Starts below baseline, lifts up into place
                val yOffset = (1f - easedLift) * heightPx

                val charX = charPositions[i]
                val charW = charWidths[i]

                canvas.save()
                // Physical clipping to this letter's slot so it emerges from below cleanly without opacity fade
                canvas.clipRect(charX - 4f, 0f, charX + charW + 4f, heightPx)

                // Layer 1: Ambient dispersed bloom (when beam passes near)
                if (beamTouchingWord) {
                    canvas.drawText(characters[i], charX, baselineY + yOffset, bloomPaint)
                }

                // Layer 2: ExtraBold stroke expansion (solid 3D contour weight)
                canvas.drawText(characters[i], charX, baselineY + yOffset, strokeExpansionPaint)

                // Layer 3: Main liquid titanium body (filled with ONE shared continuous word-space gradient!)
                canvas.drawText(characters[i], charX, baselineY + yOffset, bodyPaint)

                // Layer 4: Razor-sharp specular crest highlight
                canvas.drawText(characters[i], charX, baselineY + yOffset, crestPaint)

                canvas.restore()
            }
        }
    }
}
