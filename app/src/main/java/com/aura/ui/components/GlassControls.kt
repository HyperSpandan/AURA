package com.aura.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.roundToInt

/**
 * Liquid-glass pill slider: a thin translucent track with an accent fill and a
 * pill thumb that swells into clear refracting liquid glass while dragged.
 */
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    val c = MaterialTheme.colorScheme
    val dark = c.surface.luminance() <= 0.5f
    val backdrop = LocalBackdrop.current
    val liquid = liquidGlassOn() && backdrop != null
    val latestOnChange by rememberUpdatedState(onValueChange)
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-5f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    var dragging by remember { mutableStateOf(false) }

    val thumbScale by animateFloatAsState(
        targetValue = if (dragging) 1.32f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "sliderThumbScale",
    )
    val glassAlpha by animateFloatAsState(
        targetValue = if (dragging) 0.22f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "sliderGlassAlpha",
    )
    val thumbRim = remember(dark) {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = if (dark) 0.90f else 0.98f),
            0.5f to Color.White.copy(alpha = 0.25f),
            1f to Color.White.copy(alpha = if (dark) 0.65f else 0.85f),
        )
    }

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(38.dp)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        val thumbW = 28.dp
        val thumbHalfPx = with(density) { (thumbW / 2).toPx() }
        val trackPx = with(density) { (maxWidth - thumbW).coerceAtLeast(1.dp).toPx() }
        val thumbOffsetPx = (fraction * trackPx).roundToInt()

        val gestureMod = if (enabled) {
            Modifier.pointerInput(valueRange, trackPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    dragging = true
                    fun emitAt(x: Float) {
                        val f = ((x - thumbHalfPx) / trackPx).coerceIn(0f, 1f)
                        latestOnChange(valueRange.start + f * span)
                    }
                    emitAt(down.position.x)
                    try {
                        horizontalDrag(down.id) { change ->
                            change.consume()
                            emitAt(change.position.x)
                        }
                    } finally {
                        dragging = false
                    }
                }
            }
        } else {
            Modifier
        }

        Box(Modifier.matchParentSize().then(gestureMod), contentAlignment = Alignment.CenterStart) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(c.onSurface.copy(alpha = if (dark) 0.16f else 0.14f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(c.primary),
                )
            }

            val thumbMod = Modifier
                .offset { IntOffset(thumbOffsetPx, 0) }
                .size(width = thumbW, height = 18.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(if (dragging) 8.dp else 4.dp, CircleShape, clip = false)
                .then(
                    if (dragging && liquid && backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx())
                                val depth = lensDepth(14.dp.toPx(), size.minDimension)
                                lens(depth, depth * 1.4f, depthEffect = true, chromaticAberration = true)
                            },
                            highlight = { Highlight.Default },
                            shadow = { Shadow.Default },
                            innerShadow = { InnerShadow.Default },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.16f)) },
                        )
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = glassAlpha))
                    },
                )
                .border(if (dragging) 1.1.dp else 0.75.dp, thumbRim, CircleShape)

            Box(thumbMod)
        }
    }
}

/**
 * Liquid-glass pill selector that opens a floating glass popup menu with spring
 * scale/fade entrance, matching the web `GlassSelect` component.
 */
@Composable
fun <T> GlassDropdownSelect(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MaterialTheme.colorScheme
    val dark = c.surface.luminance() <= 0.5f
    var expanded by remember { mutableStateOf(false) }
    val rim = remember(dark) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (dark) 0.25f else 0.75f),
            1f to c.onSurface.copy(alpha = 0.10f),
        )
    }

    Box(modifier) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(c.surfaceContainerHighest.copy(alpha = if (dark) 0.7f else 0.85f))
                .border(0.75.dp, rim, CircleShape)
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                com.aura.ui.theme.tr(label(selected)),
                style = MaterialTheme.typography.labelLarge,
                color = c.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.onSurfaceVariant,
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(spring(stiffness = 500f)) + scaleIn(
                        initialScale = 0.92f,
                        transformOrigin = TransformOrigin(0.9f, 0f),
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = 480f),
                    ),
                    exit = fadeOut() + scaleOut(targetScale = 0.94f),
                ) {
                    val shape = RoundedCornerShape(22.dp)
                    Column(
                        Modifier
                            .padding(top = 42.dp)
                            .widthIn(min = 165.dp, max = 260.dp)
                            .shadow(16.dp, shape, clip = false)
                            .glass(shape)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        options.forEach { option ->
                            val isSelected = option == selected
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) c.onSurface.copy(alpha = 0.10f) else Color.Transparent)
                                    .clickable {
                                        expanded = false
                                        onSelect(option)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    com.aura.ui.theme.tr(label(option)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = c.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = c.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
