package com.aura.ui.components

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val LIQUID_LOGO_BADGE_AGSL = """
uniform shader uLogo;
uniform float2 uResolution;
uniform float2 uLogoRes;
uniform float uPhase;

float superRadius(float2 q, float n) {
    float2 a = abs(q);
    float m = max(a.x, a.y);
    if (m < 0.0001) return 0.0;
    float2 r = a / m;
    return m * pow(pow(r.x, n) + pow(r.y, n), 1.0 / n);
}

float2 superNormal(float2 q, float n) {
    float2 a = max(abs(q), float2(0.0001));
    float2 g = sign(q) * pow(a, float2(n - 1.0));
    return normalize(g + float2(0.0001));
}

half4 sampleLogo(float2 p, float boxSize) {
    float2 uv = float2(p.x / boxSize + 0.5, 0.5 - p.y / boxSize);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return half4(0.0);
    return uLogo.eval(uv * uLogoRes);
}

half4 main(float2 fragCoord) {
    float unit = min(uResolution.x, uResolution.y);
    float2 p = float2(fragCoord.x - 0.5 * uResolution.x, 0.5 * uResolution.y - fragCoord.y);
    float halfSize = unit * 0.46;
    float radius = superRadius(p / halfSize, 4.6);
    float sd = (radius - 1.0) * halfSize;
    float mask = 1.0 - smoothstep(-1.1, 1.1, sd);
    if (mask < 0.001) return half4(0.0);

    float2 outward = superNormal(p / halfSize, 4.6);
    float shoulder = smoothstep(0.62, 1.0, radius);
    float2 n = outward * (0.06 * radius + 0.82 * shoulder * shoulder);

    // Chromatic RGB refraction of the AURA mark inside the liquid glass tile
    float artBox = unit * 0.74;
    float disp = unit * 0.095;
    half4 tapR = sampleLogo(p + n * disp * 0.90, artBox);
    half4 tapG = sampleLogo(p + n * disp * 1.00, artBox);
    half4 tapB = sampleLogo(p + n * disp * 1.12, artBox);

    float3 ink = float3(float(tapR.r), float(tapG.g), float(tapB.b));
    float inkA = max(float(tapR.a), max(float(tapG.a), float(tapB.a)));

    // Deep liquid glass body + sweeping specular bevel light
    float3 tile = float3(0.03, 0.06, 0.11) * (0.65 + 0.35 * (1.0 - min(radius, 1.0)));
    float angle = atan(p.y, p.x);
    float sweep = uPhase * 6.2831853;
    float glintA = pow(max(0.0, cos(angle - sweep)), 5.0);
    float glintB = pow(max(0.0, cos(angle - sweep + 2.7)), 4.0);

    float rim = exp(-pow((sd + unit * 0.012) / (unit * 0.022), 2.0));
    float3 rimCol = float3(0.55 + 0.45 * glintA, 0.76 + 0.24 * glintA, 1.0) * rim * (0.28 + 0.65 * glintA + 0.35 * glintB);

    float3 col = mix(tile, ink, inkA) + rimCol;
    return half4(clamp(col, 0.0, 1.0) * mask, mask);
}
"""

/**
 * Compact living Liquid Glass AURA logo tile for headers and brand lockups.
 */
@Composable
fun LiquidAuraLogo(size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val mark = remember { markPath() }
    val infinite = rememberInfiniteTransition(label = "liquidLogo")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "liquidLogoPhase",
    )
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                val bmp = createAuraMarkBitmap(256)
                RuntimeShader(LIQUID_LOGO_BADGE_AGSL).apply {
                    setInputShader("uLogo", BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                    setFloatUniform("uLogoRes", bmp.width.toFloat(), bmp.height.toFloat())
                }
            }.getOrNull()
        } else {
            null
        }
    }

    if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val brush = remember(shader) { ShaderBrush(shader) }
        Canvas(modifier.size(size)) {
            if (drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                shader.setFloatUniform("uResolution", this.size.width, this.size.height)
                shader.setFloatUniform("uPhase", phase)
                drawRect(brush)
            } else {
                val s = this.size.width / MARK_VIEWPORT
                translate(this.size.width / 2f - MARK_VIEWPORT / 2f * s, this.size.height / 2f - MARK_VIEWPORT / 2f * s) {
                    scale(s, s, pivot = Offset.Zero) { drawPath(mark, Color.White) }
                }
            }
        }
    } else {
        Canvas(modifier.size(size)) {
            val s = this.size.width / MARK_VIEWPORT
            translate(this.size.width / 2f - MARK_VIEWPORT / 2f * s, this.size.height / 2f - MARK_VIEWPORT / 2f * s) {
                scale(s, s, pivot = Offset.Zero) { drawPath(mark, Color.White) }
            }
        }
    }
}
