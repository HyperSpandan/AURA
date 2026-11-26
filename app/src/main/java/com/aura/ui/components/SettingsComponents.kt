package com.aura.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/** Small gray uppercase label above a group. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        com.aura.ui.theme.tr(text).uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp, bottom = 10.dp),
    )
}

/** A rounded card holding rows, with hairline dividers between them. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.75.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * One setting: optional icon, title, summary, and whatever sits on the
 * right (a switch, a value and chevron, nothing).
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable ColumnScope.() -> Unit)? = null,
    /** Shown where the icon would be; an avatar, say. Takes the icon's place when set. */
    leading: (@Composable () -> Unit)? = null,
    /** Still being worked on: marked with a small "Beta" tag after the title. */
    beta: Boolean = false,
) {
    val alpha = if (enabled) 1f else 0.45f
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Box(Modifier.width(26.dp))
            } else if (icon != null) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), modifier = Modifier.size(26.dp))
                Box(Modifier.width(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(com.aura.ui.theme.tr(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha), modifier = Modifier.weight(1f, fill = false))
                    if (beta) BetaTag(Modifier.padding(start = 8.dp))
                }
                if (!summary.isNullOrBlank()) {
                    Text(
                        com.aura.ui.theme.tr(summary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (trailing != null) {
                Box(Modifier.padding(start = 12.dp)) { trailing() }
            }
        }
        if (below != null) {
            Column(Modifier.padding(start = if (icon != null || leading != null) 52.dp else 0.dp, top = 12.dp), content = below)
        }
    }
}

@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    summary: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    beta: Boolean = false,
) {
    SettingRow(title, summary = summary, icon = icon, enabled = enabled, beta = beta, onClick = { onChange(!checked) }, trailing = {
        AppSwitch(checked, onChange, enabled)
    })
}

@Composable
fun NavRow(title: String, onClick: () -> Unit, summary: String? = null, icon: ImageVector? = null, value: String? = null, dot: Boolean = false, leading: (@Composable () -> Unit)? = null) {
    SettingRow(title, summary = summary, icon = icon, onClick = onClick, leading = leading, trailing = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot) UpdateDot(Modifier.padding(end = 6.dp))
            if (value != null) {
                Text(com.aura.ui.theme.tr(value), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    })
}

/** The small "Beta" tag on a setting that still has rough edges. */
@Composable
fun BetaTag(modifier: Modifier = Modifier) {
    Text(
        "Beta",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** Liquid-glass pill switch with spring slide and clear-glass press refraction like the navigation dock. */
@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val c = MaterialTheme.colorScheme
    val dark = c.surface.luminance() <= 0.5f
    val liquid = liquidGlassOn()
    val trackBackdrop = rememberLayerBackdrop()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val trackColor by animateColorAsState(
        targetValue = if (checked) c.primary else c.onSurface.copy(alpha = if (dark) 0.16f else 0.14f),
        animationSpec = spring(stiffness = 500f),
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "switchThumb",
    )
    val stretchX by animateFloatAsState(
        targetValue = if (pressed) 1.28f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 580f),
        label = "switchStretch",
    )
    val stretchY by animateFloatAsState(
        targetValue = if (pressed) 1.14f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 580f),
        label = "switchStretchY",
    )
    val thumbRim = remember(dark) {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = if (dark) 0.90f else 0.98f),
            0.5f to Color.White.copy(alpha = 0.25f),
            1f to Color.White.copy(alpha = if (dark) 0.65f else 0.85f),
        )
    }

    Box(
        Modifier
            .width(52.dp)
            .height(30.dp)
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .then(if (liquid) Modifier.layerBackdrop(trackBackdrop) else Modifier)
                .clip(CircleShape)
                .background(trackColor)
                .border(0.75.dp, Color.White.copy(alpha = if (dark) 0.18f else 0.45f), CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Switch,
                    onClick = { onChange(!checked) },
                ),
        )
        Box(
            Modifier
                .offset(x = thumbOffset)
                .size(width = 27.dp, height = 24.dp)
                .graphicsLayer {
                    scaleX = stretchX
                    scaleY = stretchY
                }
                .shadow(if (pressed) 6.dp else 3.dp, CircleShape, clip = false)
                .then(
                    if (pressed && liquid) {
                        Modifier.drawBackdrop(
                            backdrop = trackBackdrop,
                            shape = { CircleShape },
                            effects = {
                                val depth = lensDepth(12.dp.toPx(), size.minDimension)
                                lens(depth, 18.dp.toPx(), depthEffect = true, chromaticAberration = false)
                            },
                            highlight = { Highlight.Default },
                            shadow = { Shadow.Default },
                            innerShadow = { InnerShadow.Default },
                            onDrawSurface = { drawRect(Color.White.copy(alpha = 0.12f)) },
                        )
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (pressed) 0.28f else 1f))
                    },
                )
                .border(if (pressed) 1.1.dp else 0.75.dp, thumbRim, CircleShape),
        )
    }
}

/** Liquid-glass segmented tabs: matches the navigation dock's local LayerBackdrop + lens refraction. */
@Composable
fun <T> PillSegmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val dark = c.surface.luminance() <= 0.5f
    val liquid = liquidGlassOn()
    val itemsBackdrop = rememberLayerBackdrop()
    val rim = remember(dark) {
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (dark) 0.24f else 0.75f),
            1f to c.onSurface.copy(alpha = 0.08f),
        )
    }
    val thumbRim = remember(dark) {
        Brush.linearGradient(
            0f to Color.White.copy(alpha = if (dark) 0.55f else 0.90f),
            0.5f to Color.White.copy(alpha = 0.14f),
            1f to Color.White.copy(alpha = if (dark) 0.35f else 0.65f),
        )
    }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(c.onSurface.copy(alpha = if (dark) 0.08f else 0.07f))
            .border(0.75.dp, rim, CircleShape)
            .padding(4.dp),
    ) {
        val index = options.indexOf(selected).coerceAtLeast(0)
        val segment = maxWidth / options.size.coerceAtLeast(1)
        val x by animateDpAsState(segment * index, spring(dampingRatio = 0.74f, stiffness = 460f), label = "pill")
        val moving = kotlin.math.abs((x - segment * index).value) > 0.5f

        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(segment)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (dark) 0.14f else 0.78f),
                            Color.White.copy(alpha = if (dark) 0.08f else 0.56f),
                        ),
                    ),
                )
                .border(0.9.dp, thumbRim, CircleShape),
        )

        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (liquid) Modifier.layerBackdrop(itemsBackdrop) else Modifier),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            options.forEachIndexed { i, option ->
                val color by animateColorAsState(if (i == index) c.onSurface else c.onSurfaceVariant, label = "pillText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(CircleShape).clickable { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        com.aura.ui.theme.tr(label(option)),
                        color = color,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (i == index) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }

        if (liquid) {
            Box(
                Modifier
                    .offset { IntOffset(x.roundToPx(), 0) }
                    .width(segment)
                    .fillMaxHeight()
                    .graphicsLayer {
                        val s = if (moving) 1.05f else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .drawBackdrop(
                        backdrop = itemsBackdrop,
                        shape = { CircleShape },
                        effects = {
                            val depth = lensDepth(12.dp.toPx(), size.minDimension)
                            lens(depth, 20.dp.toPx(), depthEffect = true, chromaticAberration = false)
                        },
                        highlight = { Highlight.Default },
                        onDrawSurface = { drawRect(Color.White.copy(alpha = if (moving) 0.08f else 0.03f)) },
                    ),
            )
        }
    }
}
