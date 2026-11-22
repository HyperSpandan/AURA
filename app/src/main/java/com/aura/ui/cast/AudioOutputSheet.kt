package com.aura.ui.cast

import androidx.compose.runtime.Composable

/**
 * Backward compatibility wrapper delegating to [DevicesSheet].
 */
@Composable
fun AudioOutputSheet(
    onDismiss: () -> Unit,
    onCast: () -> Unit,
    onPipeline: () -> Unit = {},
) {
    DevicesSheet(
        onDismiss = onDismiss,
        onCast = onCast,
    )
}
