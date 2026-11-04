package com.aura.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Individual luminous floating particle for Apple-style lyric vocal illumination.
 */
internal class LyricParticle {
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var size: Float = 0f
    var alpha: Float = 0f
    var lifeMs: Float = 0f
    var maxLifeMs: Float = 700f
    var active: Boolean = false
    var phase: Float = 0f

    fun spawn(originX: Float, originY: Float, width: Float, r: Random) {
        x = originX + (r.nextFloat() - 0.5f) * width * 0.9f
        y = originY + (r.nextFloat() - 0.5f) * 6f
        // Gentle upward drift with subtle horizontal waving
        vy = -18f - r.nextFloat() * 26f
        vx = (r.nextFloat() - 0.5f) * 14f
        size = 2.5f + r.nextFloat() * 2.5f
        alpha = 0f
        lifeMs = 0f
        maxLifeMs = 600f + r.nextFloat() * 400f
        phase = r.nextFloat() * (2f * PI.toFloat())
        active = true
    }

    fun update(deltaSec: Float): Boolean {
        if (!active) return false
        lifeMs += deltaSec * 1000f
        if (lifeMs >= maxLifeMs) {
            active = false
            return false
        }

        val progress = (lifeMs / maxLifeMs).coerceIn(0f, 1f)
        // Gentle organic sine wave drift
        x += (vx + sin(progress * 4f + phase) * 8f) * deltaSec
        y += vy * deltaSec

        // Fade in rapidly, then softly disperse
        alpha = if (progress < 0.25f) {
            progress / 0.25f
        } else {
            1f - ((progress - 0.25f) / 0.75f)
        }
        return true
    }
}

/**
 * Lightweight, allocation-free particle pool for floating lyric illumination.
 */
internal class LyricParticleSystem(private val poolSize: Int = 10) {
    private val particles = Array(poolSize) { LyricParticle() }
    private var lastEmitTimeMs: Long = 0L
    private var lastFrameNanos: Long = 0L
    private val rng = Random(42)

    fun onFrame(
        drawScope: DrawScope,
        isActive: Boolean,
        activeBox: Rect?,
        vocalEnergy: Float,
        currentPosMs: Long,
        glowColor: Color,
    ) {
        val nowNanos = System.nanoTime()
        val deltaSec = if (lastFrameNanos == 0L) 0.016f else ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.050f)
        lastFrameNanos = nowNanos

        // Emit new particles while vocal energy is active
        if (isActive && activeBox != null && vocalEnergy > 0.12f && currentPosMs - lastEmitTimeMs > 90L) {
            lastEmitTimeMs = currentPosMs
            val inactive = particles.firstOrNull { !it.active }
            inactive?.spawn(activeBox.center.x, activeBox.top + 2f, activeBox.width, rng)
        }

        // Draw active floating particles
        particles.forEach { p ->
            if (p.update(deltaSec)) {
                val effectiveAlpha = (p.alpha * 0.65f * (0.5f + vocalEnergy * 0.5f)).coerceIn(0f, 0.85f)
                if (effectiveAlpha > 0.02f) {
                    val center = Offset(p.x, p.y)
                    // Outer soft halo
                    drawScope.drawCircle(
                        color = glowColor.copy(alpha = effectiveAlpha * 0.35f),
                        radius = p.size * 2.2f,
                        center = center,
                        blendMode = BlendMode.Screen,
                    )
                    // Inner luminous spark
                    drawScope.drawCircle(
                        color = Color.White.copy(alpha = effectiveAlpha),
                        radius = p.size,
                        center = center,
                    )
                }
            }
        }
    }
}

/**
 * Draws the Apple-style word-by-word blooming illumination behind the actively sung word.
 */
internal fun DrawScope.drawAppleWordBloom(
    box: Rect,
    vocalPulse: Float,
    brightColor: Color,
    accentTint: Color = Color.White,
) {
    if (box.isEmpty || vocalPulse <= 0.02f) return

    val centerX = box.center.x
    val centerY = box.center.y
    val radius = maxOf(box.width * 0.65f, box.height * 1.15f) + 16.dp.toPx()

    // 1. Multi-stop radial specular bloom centered on the sung word
    val bloomBrush = Brush.radialGradient(
        0.0f to Color.White.copy(alpha = (0.55f * vocalPulse).coerceIn(0f, 0.65f)),
        0.35f to accentTint.copy(alpha = (0.35f * vocalPulse).coerceIn(0f, 0.45f)),
        0.70f to brightColor.copy(alpha = (0.15f * vocalPulse).coerceIn(0f, 0.25f)),
        1.0f to Color.Transparent,
        center = Offset(centerX, centerY),
        radius = radius,
    )

    drawCircle(
        brush = bloomBrush,
        radius = radius,
        center = Offset(centerX, centerY),
        blendMode = BlendMode.Screen,
    )

    // 2. High-intensity specular core directly behind word glyphs
    val coreWidth = box.width + 12.dp.toPx()
    val coreHeight = box.height + 6.dp.toPx()
    val coreLeft = box.left - 6.dp.toPx()
    val coreTop = box.top - 3.dp.toPx()

    drawRoundRect(
        color = Color.White.copy(alpha = (0.28f * vocalPulse).coerceIn(0f, 0.35f)),
        topLeft = Offset(coreLeft, coreTop),
        size = androidx.compose.ui.geometry.Size(coreWidth, coreHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        blendMode = BlendMode.Screen,
    )
}
