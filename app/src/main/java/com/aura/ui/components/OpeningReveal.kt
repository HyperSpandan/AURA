package com.aura.ui.components

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.asAndroidBitmap
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
private val CANVAS_SIZE = 380.dp

private const val LIQUID_GLASS_LOGO_AGSL = """
uniform shader uLogo;
uniform float2 uResolution;
uniform float2 uLogoRes;
uniform float uUnit;
uniform float uArtBox;
uniform float uTime;
uniform float uSettle;

const float PI = 3.14159265359;
const int BANDS = 12;

float sq(float x) { return x * x; }
float bell(float x, float width) { return exp(-sq(x / max(width, 0.001))); }
float ease(float a, float b, float x) { return smoothstep(a, b, x); }

float3 spectrum(float s) {
    float wavelength = 400.0 + 300.0 * s;
    float3 rgb = float3(0.0);
    if (wavelength < 440.0) rgb = float3((440.0 - wavelength) / 120.0, 0.0, 1.0);
    else if (wavelength < 490.0) rgb = float3(0.0, (wavelength - 440.0) / 50.0, 1.0);
    else if (wavelength < 510.0) rgb = float3(0.0, 1.0, (510.0 - wavelength) / 20.0);
    else if (wavelength < 580.0) rgb = float3((wavelength - 510.0) / 70.0, 1.0, 0.0);
    else if (wavelength < 645.0) rgb = float3(1.0, (645.0 - wavelength) / 65.0, 0.0);
    else rgb = float3(1.0, 0.0, 0.0);
    float edge = smoothstep(390.0, 440.0, wavelength) * (1.0 - smoothstep(645.0, 710.0, wavelength));
    return rgb * edge;
}

float glassIor(float s, float spread) {
    float shift = (0.5 - s) * 2.0;
    return 1.48 - 0.14 * spread * shift;
}

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

float contourNoise(float x) {
    return 0.5 + 0.25 * sin(x * 1.7 + 0.4) + 0.15 * sin(x * 3.9 + 1.9) + 0.10 * sin(x * 7.3 + 4.1);
}

float edgeWarp(float arc, float seed) {
    return uUnit * 0.0032 * (sin(arc / max(uUnit * 0.09, 1.0) + seed) * 0.6 + sin(arc / max(uUnit * 0.04, 1.0) * 1.3 + seed * 2.1) * 0.4);
}

half4 sampleLogo(float2 p, float boxSize) {
    float2 uv = float2(p.x / boxSize + 0.5, 0.5 - p.y / boxSize);
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) return half4(0.0);
    return uLogo.eval(uv * uLogoRes);
}

half4 main(float2 fragCoord) {
    // Center origin, Y-up to match optical light coordinates
    float2 p = float2(fragCoord.x - 0.5 * uResolution.x, 0.5 * uResolution.y - fragCoord.y);
    float t = uTime;
    float settle = ease(0.0, 1.0, uSettle);
    float enter = ease(0.025, 0.16, t);
    float artScale = mix(0.72, 1.0, ease(0.95, 1.48, t));

    half4 source = sampleLogo(p, uArtBox * artScale);
    if (t > 1.58) return source;
    if (enter < 0.001) return half4(0.0);

    float shape = 2.0 + 3.0 * ease(0.29, 0.36, t);
    float expand = enter * (0.64 + 0.41 * (1.0 - exp(-max(t - 0.16, 0.0) * 5.8)));
    float collapse = 1.0 - ease(1.33, 1.52, t);
    float lensScale = max(0.001, mix(expand * mix(0.48, 1.0, collapse), 1.0, settle));
    float halfSize = uUnit * 0.5 * lensScale;
    float radius = superRadius(p / halfSize, shape);
    float sd = (radius - 1.0) * halfSize;
    float aa = 1.25;
    float mask = 1.0 - smoothstep(-aa, aa, sd);
    float glass = (1.0 - ease(1.05, 1.40, t)) * collapse * (1.0 - settle);

    if (sd > uUnit * 0.10) return half4(0.0);

    float2 outward = superNormal(p / halfSize, shape);
    float2 radial = normalize(p + float2(0.001));
    float diagonal = max(0.0, 2.0 * radial.x * radial.y);
    float shoulderStart = 0.6 + 0.18 * ease(0.66, 1.15, t) * (1.0 - diagonal) + 0.16 * ease(1.0, 1.22, t) * diagonal;
    float shoulder = ease(shoulderStart, 1.0, radius);
    float slope = (0.04 * radius + (0.85 + 0.2 * (1.0 - ease(0.28, 0.75, t))) * shoulder * shoulder) * glass;
    slope += (1.0 - ease(0.18, 0.42, t)) * radius * 0.32;
    float2 opticalDirection = normalize(mix(outward, radial, 0.95));
    float astigmatism = 0.8 + 0.4 * radial.x * radial.y;
    float2 n = opticalDirection * slope * astigmatism * (1.0 - settle);

    // Wavelength-integrated refraction of the logo behind the glass lens
    float3 normal = normalize(float3(n, 1.0));
    float spread = mix(0.85, mix(2.2, 1.5, ease(0.28, 0.9, t)), ease(0.25, 0.33, t));
    float3 neutralRay = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / glassIor(0.5, spread));
    float2 neutralOffset = neutralRay.xy / max(-neutralRay.z, 0.25) * uUnit * 1.2 * 0.80 * lensScale;

    float3 inkColor = float3(0.0);
    float3 weights = float3(0.0);
    float3 refractedCov = float3(0.0);
    float inkAlpha = 0.0;
    float minCov = 1.0;
    float maxCov = 0.0;

    float sphereRadius = uUnit * lensScale * 0.5;
    float sphereFocus = 1.0 - ease(0.225, 0.335, t);
    float bubbleQ = min(length(p) / max(sphereRadius, 1.0), 1.0);
    float openingFocus = 1.0 - ease(0.19, 0.265, t);
    float focusProfile = mix(0.7 + 0.6 * sq(bubbleQ), 1.0 - 0.9 * sq(bubbleQ), openingFocus);

    for (int i = 0; i < BANDS; i++) {
        float s = float(i) / float(BANDS - 1);
        float ior = glassIor(s, spread);
        float3 ray = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / ior);
        float2 offset = ray.xy / max(-ray.z, 0.25) * uUnit * 1.2 * 0.80 * lensScale;
        float2 bubbleSample = p * focusProfile + offset * mix(0.35, 0.12, openingFocus);
        bubbleSample += float2(0.025, 0.10) * uUnit * openingFocus;
        bubbleSample += (offset - neutralOffset) * openingFocus * 1.6;
        float2 sourcePoint = mix(p + offset, bubbleSample, sphereFocus);
        half4 tap = sampleLogo(sourcePoint, uArtBox * artScale);
        tap *= mix(1.0, 1.0 - ease(0.72, 0.86, bubbleQ), openingFocus);
        float edgeShift = (s - 0.5) * uUnit * lensScale * 0.018;
        tap *= 1.0 - smoothstep(-0.8, 0.8, sd - edgeShift);
        minCov = min(minCov, float(tap.a));
        maxCov = max(maxCov, float(tap.a));
        float3 w = spectrum(s);
        inkColor += float3(tap.rgb) * w;
        inkAlpha += float(tap.a) * (w.r + w.g + w.b);
        refractedCov += float(tap.a) * w;
        weights += w;
    }
    inkColor /= max(weights, float3(0.0001));
    refractedCov /= max(weights, float3(0.0001));
    inkAlpha /= max(weights.r + weights.g + weights.b, 0.0001);

    float coating = ease(-uUnit * lensScale * 0.022, 0.0, sd);
    inkColor.g *= 1.0 - 0.65 * coating * (maxCov - minCov);
    float youngFilm = (maxCov - minCov) * (1.0 - ease(0.19, 0.30, t));
    youngFilm *= 1.0 - ease(-uUnit * lensScale * 0.08, -uUnit * lensScale * 0.025, sd);
    inkColor.r += inkColor.b * youngFilm * 0.80;
    inkColor.b += inkColor.r * youngFilm * 0.60;
    inkColor.g *= 1.0 - youngFilm * 0.55;

    float diffuse = max(dot(normal, normalize(float3(-0.4, 0.6, 1.0))), 0.0);
    float3 earlyWhite = mix(float3(0.90, 0.93, 0.95), float3(0.84, 0.86, 0.89), ease(0.21, 0.40, t));
    float3 whiteBalance = mix(earlyWhite, float3(0.87, 0.88, 0.885), ease(0.75, 1.22, t));
    inkColor *= whiteBalance * (0.90 + 0.10 * diffuse + glass * 0.04);

    // Dark glass tile body (#060c18) deepening toward the rim
    float3 tileColor = float3(0.024, 0.047, 0.094);
    float3 body = tileColor * (0.55 + 0.45 * (1.0 - min(radius, 1.0)));
    body += float3(0.004, 0.007, 0.01) * bell(radius - 0.75, 0.5);
    float3 color = inkColor + body * (1.0 - refractedCov) * glass;
    float plateFade = (1.0 - ease(1.08, 1.36, t)) * (1.0 - settle);
    float alpha = max(inkAlpha, plateFade);

    // Broad softbox studio light reflected off the curved front face of the sphere
    float2 spherePoint = p / halfSize;
    float3 sphereNormal = normalize(float3(spherePoint, sqrt(max(0.001, 1.0 - dot(spherePoint, spherePoint)))));
    float3 sphereRay = reflect(float3(0.0, 0.0, -1.0), sphereNormal);
    float lightTravel = mix(-0.12, 0.20, ease(0.13, 0.30, t));
    float softbox = bell(sphereRay.x - lightTravel, 1.05) * bell(sphereRay.y - 0.56, 0.52);
    float sphericalLight = softbox * (1.0 - ease(0.245, 0.36, t)) * glass * 0.70;
    color += float3(0.50, 0.61, 0.72) * sphericalLight * (1.0 - inkAlpha * 0.85);
    alpha = max(alpha, sphericalLight);

    // Shimmering liquid rim lines & chromatic edge streaks
    float arc = atan(p.y, p.x) * uUnit * 0.5;
    float grainScale = uUnit * 0.03;
    float developed = ease(0.23, 0.48, t);
    float cornerExposure = ease(0.12, 0.8, -2.0 * radial.x * radial.y);
    float reliefScale = lensScale * developed * (0.2 + 0.8 * cornerExposure);
    float sharedWarp = edgeWarp(arc, 0.0) * reliefScale;
    float rimSd = sd + sharedWarp;
    float around = arc / (uUnit * 0.5);
    float rimBob = uUnit * 0.0025 * developed * (0.6 * sin(7.0 * around + 0.4) * sin(t * 6.3 + 1.0) + 0.4 * sin(3.0 * around + 2.0) * sin(t * 4.1));
    float shimmer = mix(contourNoise(arc / grainScale), contourNoise(arc / grainScale + 211.0), 0.5 + 0.5 * sin(t * 5.0));
    float rim = bell(rimSd + rimBob + uUnit * 0.002, uUnit * 0.0022 + aa * 0.3) * (0.6 + 0.8 * shimmer);

    float angle = atan(p.y, p.x);
    float key = pow(0.5 + 0.5 * dot(outward, normalize(float2(-0.5, 0.9))), 5.0);
    float upperSliver = 0.12 + 1.15 * bell(angle - 2.18, 0.32);
    float rimVis = 1.0 - 0.55 * ease(0.75, 1.22, t);
    float3 rimHighlight = float3(0.82, 0.90, 1.0) * rim * (0.35 + 0.95 * key * upperSliver) * glass * rimVis;

    // Chromatic edge streaks around the squircle bevel
    float streakAngle = -2.2 + t * 3.8;
    float streakA = pow(max(0.0, cos(angle - streakAngle)), 6.0);
    float streakB = pow(max(0.0, cos(angle - streakAngle + 2.8)), 5.0);
    float bevelBand = bell(sd + uUnit * 0.012, uUnit * 0.018) * mask * glass;
    float3 chromaticStreak = float3(
        streakA * 0.55 + streakB * 0.25,
        streakA * 0.72 + streakB * 0.50,
        streakA * 1.00 + streakB * 0.85
    ) * bevelBand * 0.85;

    // Faint outer echo ring
    float echoSd = sd - uUnit * 0.014;
    float echoRing = bell(echoSd, uUnit * 0.003 + aa * 0.25) * glass * 0.35;

    color += (rimHighlight + chromaticStreak + float3(0.45, 0.65, 0.95) * echoRing) * mask;
    alpha = clamp(max(alpha, max(rim * glass, echoRing)), 0.0, 1.0);

    return half4(clamp(color, 0.0, 1.0), alpha);
}
"""

/**
 * Liquid Glass Logo Opening Reveal:
 *
 * 1. On Android 13+, runs the full AGSL wavelength-dispersed sphere-to-tile glass shader,
 *    genuinely refracting the AURA logo bitmap behind a morphing superellipse lens with
 *    spectral chromatic dispersion, softbox front reflection, and shimmering liquid rim streaks.
 * 2. Dissolves the glass cleanly so the original AURA logo remains alone on pure OLED black (#000000).
 * 3. Fluid spatial unveil into the app.
 */
@Composable
fun OpeningReveal(onDive: (Float) -> Unit, onDone: () -> Unit) {
    val logoBitmap = ImageBitmap.imageResource(id = R.drawable.splash_logo)
    val mark = remember { markPath() }
    val time = remember { Animatable(0f) }
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
        // 1. Run the sphere-to-tile Liquid Glass timeline (0 -> 1.62)
        launch {
            elevation.animateTo(1f, tween(1400, easing = CubicBezierEasing(0.35f, 0f, 0.25f, 1f)))
        }
        time.animateTo(1.62f, tween(2100, easing = LinearEasing))

        // 2. Brief poise on the settled original logo on pure black
        delay(140L)

        // 3. Fluid spatial unveil into the app
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
        val shader = remember(logoBitmap) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    RuntimeShader(LIQUID_GLASS_LOGO_AGSL).apply {
                        val bmp = logoBitmap.asAndroidBitmap()
                        setInputShader("uLogo", BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                        setFloatUniform("uLogoRes", bmp.width.toFloat(), bmp.height.toFloat())
                    }
                }.getOrNull()
            } else {
                null
            }
        }

        if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shaderBrush = remember(shader) { ShaderBrush(shader) }
            Canvas(
                modifier = Modifier
                    .size(CANVAS_SIZE)
                    .graphicsLayer {
                        val d = dive.value
                        val s = 1f + 0.08f * d
                        scaleX = s
                        scaleY = s
                    },
            ) {
                if (drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
                    val unit = size.minDimension * 0.68f
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    shader.setFloatUniform("uUnit", unit)
                    shader.setFloatUniform("uArtBox", unit)
                    shader.setFloatUniform("uTime", time.value)
                    shader.setFloatUniform("uSettle", 0f)
                    drawRect(shaderBrush)
                } else {
                    // Software-rendering fallback (e.g., bitmap capture in host tests)
                    val s = size.width / MARK_VIEWPORT
                    translate(size.width / 2f - MARK_VIEWPORT / 2f * s, size.height / 2f - MARK_VIEWPORT / 2f * s) {
                        scale(s, s, pivot = Offset.Zero) {
                            drawPath(mark, Color.White)
                        }
                    }
                }
            }
        } else {
            // Android < 13 fallback
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .graphicsLayer {
                        val gw = elevation.value
                        val d = dive.value
                        val s = 0.88f + 0.24f * gw + 0.80f * d
                        scaleX = s
                        scaleY = s
                        alpha = (gw * 0.55f * (1f - d)).coerceIn(0f, 1f)
                    }
                    .background(causticBrush, shape = CircleShape),
            )
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
        }
    }
}
