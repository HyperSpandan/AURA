package com.aura.ui.components

import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
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

private val LOGO_SIZE = 240.dp
private val GLASS_TILE_SIZE = 320.dp

private const val LIQUID_GLASS_LOGO_AGSL = """
uniform float2 uSize;
uniform float uMorph;     // 0 = sphere, 1 = rounded tile
uniform float uGlassFade; // 1 = full glass optics, 0 = dissolved
uniform float uSweep;     // 0..1 chromatic sweep & ripple

float sdRoundedBox(float2 p, float2 b, float r) {
    float2 q = abs(p) - b + float2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord - 0.5 * uSize) / min(uSize.x, uSize.y);
    float halfSide = mix(0.24, 0.34, uMorph);
    float radius = mix(halfSide, halfSide * 0.44, uMorph);
    float d = sdRoundedBox(uv, float2(halfSide), radius);

    // Outward liquid caustic ripple ring
    float rippleRadius = mix(0.18, 0.52, uSweep);
    float ripple = exp(-pow((length(uv) - rippleRadius) * 28.0, 2.0)) * (1.0 - uSweep) * 0.35;

    // Bevel band inside the glass boundary
    float bevelWidth = 0.048;
    float edgeMask = smoothstep(0.008, -0.004, d) * smoothstep(-bevelWidth, -0.004, d);
    float innerMask = smoothstep(0.004, -0.006, d);

    // Normal estimate via central gradient of SDF
    float2 e = float2(0.002, 0.0);
    float nx = sdRoundedBox(uv + e.xy, float2(halfSide), radius) - sdRoundedBox(uv - e.xy, float2(halfSide), radius);
    float ny = sdRoundedBox(uv + e.yx, float2(halfSide), radius) - sdRoundedBox(uv - e.yx, float2(halfSide), radius);
    float2 n = normalize(float2(nx, ny) + float2(1e-5));

    // Specular light direction + sweeping chromatic edge streaks
    float angle = atan(n.y, n.x);
    float sweepAngle = -2.2 + uSweep * 4.8;
    float streakA = pow(max(0.0, cos(angle - sweepAngle)), 6.0);
    float streakB = pow(max(0.0, cos(angle - sweepAngle + 2.8)), 5.0);

    // Wavelength-dispersed chromatic rim (R, G, B slightly offset along normal)
    float rBand = smoothstep(0.012, -0.014, d + 0.004) * edgeMask;
    float gBand = smoothstep(0.012, -0.014, d) * edgeMask;
    float bBand = smoothstep(0.012, -0.014, d - 0.004) * edgeMask;

    float3 rimColor = float3(
        rBand * (0.65 + 0.65 * streakA + 0.35 * streakB),
        gBand * (0.78 + 0.55 * streakA + 0.45 * streakB),
        bBand * (0.98 + 0.45 * streakA + 0.70 * streakB)
    );

    // Subtle dark-blue tinted glass body + caustic rim
    float3 glassBody = float3(0.035, 0.065, 0.12) * innerMask * 0.55;
    float3 col = (glassBody + rimColor * 0.95) * uGlassFade + float3(0.45, 0.68, 1.0) * ripple * uGlassFade;
    float alpha = clamp((innerMask * 0.38 + edgeMask * 0.85) * uGlassFade + ripple * uGlassFade, 0.0, 1.0);
    return half4(col, alpha);
}
"""

/**
 * Liquid Glass Logo Opening Reveal:
 *
 * 1. Sphere-to-tile glass entrance with wavelength-dispersed bevel refraction and
 *    chromatic edge streaks (AGSL RuntimeShader on Android 13+; smooth fallback on older APIs).
 * 2. Glass and streaks dissolve cleanly, leaving AURA's original logo on pure OLED black (#000000).
 * 3. Fluid spatial unveil into the app.
 */
@Composable
fun OpeningReveal(onDive: (Float) -> Unit, onDone: () -> Unit) {
    val logoBitmap = ImageBitmap.imageResource(id = R.drawable.splash_logo)
    val mark = remember { markPath() }
    val morph = remember { Animatable(0f) }
    val glassFade = remember { Animatable(1f) }
    val sweep = remember { Animatable(0f) }
    val elevation = remember { Animatable(0f) }
    val dive = remember { Animatable(0f) }
    val done by rememberUpdatedState(onDone)
    val report by rememberUpdatedState(onDive)

    val causticBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFFDCE2EA).copy(alpha = 0.42f),
                Color(0xFF818995).copy(alpha = 0.24f),
                Color(0xFFE8D5C4).copy(alpha = 0.16f),
                Color.Transparent,
            )
        )
    }

    LaunchedEffect(Unit) {
        // 1. Sphere-to-tile glass morph + chromatic sweep
        launch {
            morph.animateTo(1f, tween(950, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
        }
        launch {
            sweep.animateTo(1f, tween(1350, easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)))
        }
        launch {
            elevation.animateTo(1f, tween(1150, easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)))
        }
        // 2. Glass and streaks dissolve, leaving the original logo alone on black
        delay(980L)
        glassFade.animateTo(0f, tween(480, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
        // 3. Brief poise on the settled logo, then fluid unveil into the app
        delay(160L)
        dive.animateTo(1f, tween(640, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) { report(value) }
        report(1f)
        done()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OPENING_FIELD)
            .graphicsLayer {
                alpha = (1f - dive.value).coerceIn(0f, 1f)
            },
        contentAlignment = Alignment.Center,
    ) {
        // Ambient caustic halo behind the logo
        Box(
            modifier = Modifier
                .size(380.dp)
                .graphicsLayer {
                    val gw = elevation.value
                    val gf = glassFade.value
                    val d = dive.value
                    val s = 0.88f + 0.24f * gw + 0.80f * d
                    scaleX = s
                    scaleY = s
                    alpha = (gw * gf * 0.55f * (1f - d)).coerceIn(0f, 1f)
                }
                .background(causticBrush, shape = CircleShape),
        )

        // Original AURA logo mark
        Image(
            bitmap = logoBitmap,
            contentDescription = "AURA",
            modifier = Modifier
                .size(LOGO_SIZE)
                .graphicsLayer {
                    val el = elevation.value
                    val d = dive.value
                    val s = (0.86f + 0.14f * el) + 0.08f * d
                    scaleX = s
                    scaleY = s
                },
        )

        // Liquid Glass sphere-to-tile shader overlay (Android 13+) or specular sweep fallback
        val shader = remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { RuntimeShader(LIQUID_GLASS_LOGO_AGSL) }.getOrNull()
            } else {
                null
            }
        }
        if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shaderBrush = remember(shader) { ShaderBrush(shader) }
            Canvas(
                modifier = Modifier
                    .size(GLASS_TILE_SIZE)
                    .graphicsLayer {
                        val d = dive.value
                        val s = 1f + 0.06f * d
                        scaleX = s
                        scaleY = s
                    },
            ) {
                val gf = glassFade.value
                if (gf > 0.01f) {
                    if (drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                        shader.setFloatUniform("uSize", size.width, size.height)
                        shader.setFloatUniform("uMorph", morph.value)
                        shader.setFloatUniform("uGlassFade", gf)
                        shader.setFloatUniform("uSweep", sweep.value)
                        drawRect(shaderBrush)
                    } else {
                        // Software-rendering fallback (e.g., bitmap capture in tests)
                        val m = morph.value
                        val half = size.minDimension * (0.24f + 0.10f * m)
                        val r = half * (1f - 0.56f * m)
                        drawRoundRect(
                            color = Color(0xFF8FB4FF).copy(alpha = 0.28f * gf),
                            topLeft = Offset(size.width / 2f - half, size.height / 2f - half),
                            size = androidx.compose.ui.geometry.Size(half * 2f, half * 2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()),
                        )
                    }
                }
            }
        } else {
            Canvas(
                modifier = Modifier
                    .size(LOGO_SIZE)
                    .graphicsLayer {
                        val el = elevation.value
                        val d = dive.value
                        val s = (0.86f + 0.14f * el) + 0.08f * d
                        scaleX = s
                        scaleY = s
                        alpha = glassFade.value
                    },
            ) {
                val sh = sweep.value
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
}
