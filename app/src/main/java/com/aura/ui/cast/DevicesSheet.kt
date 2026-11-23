package com.aura.ui.cast

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.cast.Cast
import com.aura.cast.CastTarget
import com.aura.data.remote.FoundTv
import com.aura.data.remote.PairedComputer
import com.aura.data.remote.PairedComputers
import com.aura.data.remote.PairedTv
import com.aura.data.remote.PairedTvs
import com.aura.data.remote.PrefsStorage
import com.aura.data.remote.TvDiscovery
import com.aura.ui.components.FloatingCard
import com.aura.ui.components.rememberHaptics
import com.aura.ui.player.OutputRoute
import com.aura.ui.player.VolumeBar
import com.aura.ui.player.icon
import com.aura.ui.player.rememberOutputDeviceName
import com.aura.ui.player.rememberOutputRoute

/**
 * Spotify Connect / YouTube Music style Devices sheet:
 * Shows the current active playback device with volume control,
 * available Google Cast speakers and smart TVs, paired AURA TVs with quick
 * remote control, the AURA Web Player, and pairing shortcuts.
 */
@Composable
fun DevicesSheet(
    onDismiss: () -> Unit,
    onCast: () -> Unit = {},
    onLinkDevice: (target: String?) -> Unit = {},
    onRemote: () -> Unit = {},
    onTogether: () -> Unit = {},
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme

    val route = rememberOutputRoute()
    val deviceName = rememberOutputDeviceName()
        .removePrefix("Playing on ").substringBefore("…").ifBlank { "This phone" }
    val session by Cast.session.collectAsState()
    val castDevices by Cast.devices.collectAsState()
    val scanningCast by Cast.scanning.collectAsState()

    // Scan for Cast devices on open
    LaunchedEffect(Unit) { Cast.scan(context) }

    // Paired AURA TVs and local discovery
    val pairedTvs by remember { PairedTvs(PrefsStorage(context, "remote_tvs")).tvs }.collectAsState(initial = emptyList())
    val pairedComputers by remember { PairedComputers(PrefsStorage(context, "remote_computers")).computers }.collectAsState(initial = emptyList())
    val tvDiscovery = remember { TvDiscovery(context) }
    val foundTvs by tvDiscovery.found.collectAsState(initial = emptyList())
    DisposableEffect(Unit) {
        tvDiscovery.start()
        onDispose { tvDiscovery.stop() }
    }

    // Live Web Player & Connect Session state
    val connectView by com.aura.data.connect.ConnectManager.view.collectAsState()
    val remoteDevices = connectView.devices.filter { it.id != connectView.myId }

    FloatingCard(onDismiss = onDismiss, title = "Connect to a device", icon = Icons.Rounded.Devices) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .animateContentSize()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ─── Current Device Section ───
            Text(
                "CURRENT DEVICE",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp, top = 2.dp),
            )

            val isLocalPlaying by com.aura.data.connect.ConnectManager.isLocalPlaying.collectAsState()
            val activeRemote = if (!isLocalPlaying) {
                remoteDevices.firstOrNull { it.state?.playing == true }
                    ?: remoteDevices.firstOrNull { it.id == connectView.activeId && it.state?.track != null && connectView.activeId != connectView.myId }
            } else null

            if (session != null) {
                val s = session!!
                ActiveDeviceCard(
                    icon = Icons.Rounded.CastConnected,
                    title = s.target.name,
                    subtitle = if (s.receiver == null) "Connecting…" else "Google Cast · Playing here",
                    actionLabel = "Disconnect",
                    onAction = { haptics.press(); Cast.disconnect() },
                )
            } else if (activeRemote != null) {
                val trackTitle = activeRemote.state?.track?.title ?: "Music"
                val artistName = activeRemote.state?.track?.artist
                val detail = if (!artistName.isNullOrBlank()) "$trackTitle · $artistName" else trackTitle
                ActiveDeviceCard(
                    icon = when (activeRemote.form) {
                        "tv" -> Icons.Rounded.Tv
                        else -> Icons.Rounded.Laptop
                    },
                    title = activeRemote.name.ifBlank { "AURA Web Player" },
                    subtitle = "$detail · Playing here",
                    actionLabel = "Play on phone",
                    onAction = {
                        haptics.press()
                        com.aura.data.connect.ConnectManager.pullToPhone()
                    },
                    showVolume = false,
                )
            } else {
                ActiveDeviceCard(
                    icon = if (route == OutputRoute.CAST) Icons.Rounded.Smartphone else route.icon,
                    title = if (route == OutputRoute.CAST) "This phone" else deviceName,
                    subtitle = "This device · Playing here",
                    onAction = null,
                )
            }

            // ─── Available Devices Section ───
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "SELECT A DEVICE",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (scanningCast) {
                    CircularProgressIndicator(
                        Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = scheme.primary,
                    )
                }
            }

            // If currently casting or playing on another connected device, option to switch back to phone
            if (session != null || activeRemote != null) {
                DeviceItemRow(
                    icon = route.icon,
                    title = deviceName,
                    subtitle = "Switch playback to this phone",
                    onClick = {
                        haptics.tick()
                        if (session != null) Cast.disconnect()
                        if (activeRemote != null) {
                            com.aura.data.connect.ConnectManager.pullToPhone()
                        }
                    },
                )
            }

            // Discovered Cast devices
            castDevices.forEach { target ->
                if (session?.target?.id != target.id) {
                    val tv = target.kind == CastTarget.Kind.DLNA &&
                        Regex("tv|bravia|samsung|lg|webos|tizen|android", RegexOption.IGNORE_CASE)
                            .containsMatchIn(target.name + " " + target.model.orEmpty())
                    val icon = when {
                        target.kind == CastTarget.Kind.CHROMECAST &&
                            target.model?.contains("speaker", true) != true &&
                            target.model?.contains("home", true) != true &&
                            target.model?.contains("nest audio", true) != true -> Icons.Rounded.Cast
                        tv -> Icons.Rounded.Tv
                        else -> Icons.Rounded.Speaker
                    }
                    DeviceItemRow(
                        icon = icon,
                        title = target.name,
                        subtitle = listOfNotNull(target.model, if (target.kind == CastTarget.Kind.CHROMECAST) "Google Cast" else "Smart Speaker / TV").distinct().joinToString(" · "),
                        onClick = {
                            haptics.press()
                            Cast.connect(context, target)
                            onDismiss()
                        },
                    )
                }
            }

            // Paired AURA TVs
            if (pairedTvs.isNotEmpty()) {
                pairedTvs.forEach { tv ->
                    val isOnline = foundTvs.any { it.tvId == tv.tvId }
                    TvDeviceRow(
                        tv = tv,
                        online = isOnline,
                        onRemoteClick = {
                            haptics.tick()
                            onRemote()
                            onDismiss()
                        },
                    )
                }
            } else {
                // TV discovery or pairing prompt
                DeviceItemRow(
                    icon = Icons.Rounded.Tv,
                    title = "AURA for TV",
                    subtitle = "Control your TV or sign in from your phone",
                    chevron = true,
                    onClick = {
                        haptics.tick()
                        onLinkDevice("tv")
                        onDismiss()
                    },
                )
            }

            // Live AURA Web Players connected via Nostr Connect
            if (remoteDevices.isNotEmpty()) {
                remoteDevices.forEach { dev ->
                    val isPlaying = dev.state?.playing == true
                    val songTitle = dev.state?.track?.title
                    val subtitle = when {
                        isPlaying && !songTitle.isNullOrBlank() -> "Playing: $songTitle · Tap to bring to phone"
                        isPlaying -> "Playing · Tap to bring to phone"
                        else -> "Connected · Tap to play music here"
                    }
                    DeviceItemRow(
                        icon = when (dev.form) {
                            "tv" -> Icons.Rounded.Tv
                            else -> Icons.Rounded.Laptop
                        },
                        title = dev.name.ifBlank { "AURA Web Player" },
                        subtitle = subtitle,
                        chevron = !isPlaying,
                        trailing = if (isPlaying) {
                            {
                                FilledTonalButton(
                                    onClick = {
                                        haptics.tick()
                                        com.aura.data.connect.ConnectManager.sendCommand(dev.id, "toggle")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Pause")
                                }
                            }
                        } else null,
                        onClick = {
                            haptics.press()
                            if (isPlaying) {
                                com.aura.data.connect.ConnectManager.pullToPhone()
                            } else {
                                val ok = com.aura.data.connect.ConnectManager.transferTo(dev.id)
                                if (ok) {
                                    com.aura.data.Pills.info("Music moved to ${dev.name}", context)
                                }
                            }
                            onDismiss()
                        },
                    )
                }
            } else if (pairedComputers.isNotEmpty()) {
                pairedComputers.forEach { pc ->
                    DeviceItemRow(
                        icon = Icons.Rounded.Laptop,
                        title = pc.name,
                        subtitle = "AURA Web Player · Tap to connect & play",
                        chevron = true,
                        onClick = {
                            haptics.tick()
                            com.aura.data.connect.ConnectManager.start(context, pc.secret)
                            val ok = com.aura.data.connect.ConnectManager.transferTo(pc.id)
                            if (!ok) {
                                onLinkDevice("computer")
                            }
                            onDismiss()
                        },
                    )
                }
            } else {
                DeviceItemRow(
                    icon = Icons.Rounded.Laptop,
                    title = "AURA Web Player",
                    subtitle = "Play on computer or scan code",
                    chevron = true,
                    onClick = {
                        haptics.tick()
                        onLinkDevice("computer")
                        onDismiss()
                    },
                )
            }

            // Listen Together
            DeviceItemRow(
                icon = Icons.Rounded.Groups,
                title = "Listen together",
                subtitle = "Host or join a synchronized room with friends",
                chevron = true,
                onClick = {
                    haptics.tick()
                    onTogether()
                    onDismiss()
                },
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = scheme.outlineVariant.copy(alpha = 0.35f),
            )

            // ─── Quick Actions & Shortcuts ───
            DeviceActionRow(
                icon = Icons.Rounded.QrCode2,
                title = "Link a new device",
                subtitle = "Scan TV QR code, web player, or friend code",
                onClick = {
                    haptics.tick()
                    onLinkDevice(null)
                    onDismiss()
                },
            )

            DeviceActionRow(
                icon = Icons.Rounded.Bluetooth,
                title = "Bluetooth settings",
                subtitle = "Pair or switch headphones & speakers",
                isExternal = true,
                onClick = {
                    haptics.tick()
                    runCatching {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ActiveDeviceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String = "Stop",
    onAction: (() -> Unit)? = null,
    showVolume: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.primary.copy(alpha = 0.12f))
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onAction != null) {
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(actionLabel)
                }
            } else {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Active",
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        if (showVolume) {
            VolumeBar(Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun DeviceItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = scheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            trailing()
        } else if (chevron) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TvDeviceRow(
    tv: PairedTv,
    online: Boolean,
    onRemoteClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surfaceContainerHigh)
            .clickable(onClick = onRemoteClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (online) scheme.primary.copy(alpha = 0.15f) else scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Tv,
                contentDescription = null,
                tint = if (online) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tv.name.ifBlank { "AURA TV" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (online) "AURA TV · Ready on Wi-Fi" else "AURA TV · Offline or standby",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        FilledTonalButton(
            onClick = onRemoteClick,
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("Remote")
        }
    }
}

@Composable
private fun DeviceActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExternal: Boolean = false,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Icon(
            if (isExternal) Icons.AutoMirrored.Rounded.OpenInNew else Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
