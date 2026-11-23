package com.aura.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.aura.data.Pills
import com.aura.data.account.AccountStore
import com.aura.data.remote.PairedComputer
import com.aura.data.remote.PairedComputers
import com.aura.data.remote.PrefsStorage
import com.aura.ui.components.PageHeader
import com.aura.ui.components.SheetButton
import com.aura.ui.components.SheetTone
import com.aura.ui.components.rememberHaptics
import com.aura.ui.theme.Icon
import com.aura.ui.theme.Text
import com.aura.ui.together.QrCode

/** The other screens AURA can reach from this one. */
enum class LinkTarget(val label: String, val icon: ImageVector) {
    TV("TV", Icons.Rounded.Tv),
    COMPUTER("Computer", Icons.Rounded.Laptop),
    SPEAKER("Speaker", Icons.Rounded.Speaker),
    FRIEND("Friend", Icons.Rounded.Groups),
}

/**
 * Link a device: one place for everything that reaches past this phone.
 * A row of devices at the top picks one; under it, that device's steps in
 * order, each with the button that does it. Signing a TV in, linking a computer
 * web player, casting, and listening together.
 */
@Composable
fun LinkDeviceScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onTogether: () -> Unit,
    onRemote: () -> Unit = {},
    initialTarget: LinkTarget = LinkTarget.TV,
) {
    val context = LocalContext.current
    var target by rememberSaveable { mutableStateOf(initialTarget) }
    var casting by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }

    val pairedComputers = remember { PairedComputers(PrefsStorage(context, "remote_computers")) }
    val computers by pairedComputers.computers.collectAsState()

    val phoneSecret = remember {
        val s = PrefsStorage(context, "connect_phone_secret")
        s.read()?.takeIf { PairedComputers.parse(it) != null }
            ?: PairedComputers.generateSecret().also { s.write(it) }
    }

    LaunchedEffect(target, phoneSecret) {
        if (target == LinkTarget.COMPUTER) {
            com.aura.data.connect.ConnectManager.start(context, phoneSecret)
        }
    }

    // QR scanner handles TV pairing codes, computer connect codes, and general AURA links.
    if (scanning) com.aura.ui.account.QrScannerDialog(onDismiss = { scanning = false }) { text ->
        scanning = false
        val computerSecret = PairedComputers.parse(text)
        if (computerSecret != null) {
            val computer = PairedComputer(
                id = java.util.UUID.randomUUID().toString().take(12),
                name = "Web Browser",
                secret = computerSecret,
            )
            pairedComputers.add(computer)
            com.aura.data.connect.ConnectManager.start(context, computerSecret)
            Pills.info("Connected to AURA Web Player", context)
            return@QrScannerDialog
        }

        val view = Intent(Intent.ACTION_VIEW, text.toUri())
        if (!com.aura.ui.account.PairingLinks.receive(view)) {
            if (com.aura.ui.Links.parse(text) != null || text.startsWith("aura://")) {
                com.aura.ui.Links.receive(view)
            } else {
                Pills.error("That isn't an AURA code", context)
            }
        }
    }

    val signedIn by AccountStore.signedIn.collectAsState()
    if (casting) com.aura.ui.cast.CastSheet { casting = false }

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            .onFailure { Pills.error("No app here can open that link", context) }
    }
    fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        runCatching { context.startActivity(Intent.createChooser(send, "Send the link")) }
    }
    fun camera() {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            scanning = true
        } else {
            Pills.error("This device has no camera. Scan the code with the phone's camera app", context)
        }
    }

    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        item { PageHeader("Link a device", onBack = onBack) }
        item {
            Text(
                "Sign in your TV, link AURA on a computer, play on a speaker or listen with a friend.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            )
        }
        item { DevicePicker(target) { target = it } }
        item {
            AnimatedContent(
                targetState = target,
                transitionSpec = {
                    val way = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 420f)) { it / 6 * way } + fadeIn(tween(220, delayMillis = 60)))
                        .togetherWith(slideOutHorizontally(tween(180)) { -it / 8 * way } + fadeOut(tween(140)))
                },
                label = "device",
            ) { shown ->
                Column(Modifier.padding(top = 20.dp)) {
                    when (shown) {
                        LinkTarget.TV -> TvSteps(signedIn, onSignIn, ::camera, ::open, ::share, onRemote)
                        LinkTarget.COMPUTER -> ComputerSteps(
                            pairedComputers = pairedComputers,
                            computers = computers,
                            phoneSecret = phoneSecret,
                            onCamera = ::camera,
                            onOpen = ::open,
                            onShare = ::share,
                        )
                        LinkTarget.SPEAKER -> DeviceCard(
                            "On a speaker or smart TV",
                            "Send what's playing to a Chromecast, a Google speaker or a TV that plays over the network.",
                        ) {
                            Step(1, "Join the same Wi-Fi", "The speaker or TV and this phone need to be on the same network.")
                            Step(2, "Pick it from the list", "AURA looks for devices nearby. The phone stays the remote.", last = true) {
                                Buttons({ SheetButton("Find devices", onClick = { casting = true }, icon = Icons.Rounded.Cast, tone = SheetTone.Primary) })
                            }
                        }
                        LinkTarget.FRIEND -> DeviceCard(
                            "With a friend",
                            "Listen to the same song at the same moment on two phones, wherever each of you is, and chat or talk as it plays.",
                        ) {
                            Step(1, "Start a room", "You get a code and a link to send.")
                            Step(2, "Your friend joins", "They open the link, or type the code in AURA.", last = true) {
                                Buttons({ SheetButton("Listen together", onClick = onTogether, icon = Icons.Rounded.Groups, tone = SheetTone.Primary) })
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/** Computer & Web Player linking: scan computer screen, show phone QR code/link, or enter code. */
@Composable
private fun ComputerSteps(
    pairedComputers: PairedComputers,
    computers: List<PairedComputer>,
    phoneSecret: String,
    onCamera: () -> Unit,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var manualCode by rememberSaveable { mutableStateOf("") }
    var showPhoneQr by rememberSaveable { mutableStateOf(false) }
    var computerToUnlink by remember { mutableStateOf<PairedComputer?>(null) }

    computerToUnlink?.let { pc ->
        AlertDialog(
            onDismissRequest = { computerToUnlink = null },
            title = { Text("Unlink ${pc.name}?") },
            text = { Text("This computer will be disconnected from your phone until you link it again.") },
            confirmButton = {
                TextButton(onClick = {
                    pairedComputers.remove(pc.id)
                    computerToUnlink = null
                    Pills.info("Computer unlinked", context)
                }) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { computerToUnlink = null }) { Text("Cancel") }
            },
        )
    }

    // 1. Linked Computers list
    if (computers.isNotEmpty()) {
        DeviceCard(
            title = "Linked computers",
            about = "Browsers and desktop players connected with this phone for remote control.",
            compact = true,
        ) {
            computers.forEach { pc ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Laptop, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("AURA Web Player · Linked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { computerToUnlink = pc }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Unlink", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Unlink")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // 2. Scan Computer Screen QR Code (Recommended)
    DeviceCard(
        title = "Scan from computer screen",
        about = "Scan the QR code on your computer screen to link instantly with your phone.",
    ) {
        Step(1, "Open AURA Web Player", "On your computer, open the web player address below.") {
            Text(
                WEB_PLAYER.removePrefix("https://"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Buttons(
                { SheetButton("Open here", onClick = { onOpen(WEB_PLAYER) }, icon = Icons.AutoMirrored.Rounded.OpenInNew) },
                { SheetButton("Share address", onClick = { onShare(WEB_PLAYER) }, icon = Icons.Rounded.Share) },
            )
        }
        Step(2, "Show code on your computer", "In the web player, go to Settings › Your devices to display your computer's QR code.")
        Step(3, "Scan with this phone", "Point your camera at the computer screen to link both devices.", last = true) {
            Buttons({
                SheetButton(
                    "Scan computer QR code",
                    onClick = onCamera,
                    icon = Icons.Rounded.QrCodeScanner,
                    tone = SheetTone.Primary,
                )
            })
        }
    }

    // 3. Phone Link & QR Code (Alternative: Computer joins Phone)
    val webJoinUrl = PairedComputers.webLink(phoneSecret)
    DeviceCard(
        title = "Or link from phone",
        about = "Send this phone's pairing link to your computer, or show this phone's QR code.",
        compact = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Open this link on your computer browser to join:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                webJoinUrl.removePrefix("https://"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
            Buttons(
                {
                    SheetButton(
                        "Copy link",
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cb?.setPrimaryClip(ClipData.newPlainText("AURA Connect Link", webJoinUrl))
                            Pills.info("Link copied to clipboard", context)
                        },
                        icon = Icons.Rounded.ContentCopy,
                        tone = SheetTone.Primary,
                    )
                },
                {
                    SheetButton(
                        "Share link",
                        onClick = { onShare(webJoinUrl) },
                        icon = Icons.Rounded.Share,
                    )
                },
            )

            TextButton(
                onClick = { showPhoneQr = !showPhoneQr; haptics.tick() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Rounded.QrCode2, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (showPhoneQr) "Hide QR code" else "Show QR code")
            }

            AnimatedVisibility(showPhoneQr) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QrCode(PairedComputers.appLink(phoneSecret), Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Scan with a device running AURA, or open the link on your computer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    // 4. Enter Code Manually
    DeviceCard(
        title = "Enter code manually",
        about = "Paste a link or enter the 43-character pairing code shown in your browser.",
        compact = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it },
                placeholder = { Text("Paste link or 43-character code…") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            SheetButton(
                "Connect",
                onClick = {
                    val parsed = PairedComputers.parse(manualCode)
                    if (parsed != null) {
                        pairedComputers.add(
                            PairedComputer(
                                id = java.util.UUID.randomUUID().toString().take(12),
                                name = "Web Browser",
                                secret = parsed,
                            )
                        )
                        com.aura.data.connect.ConnectManager.start(context, parsed)
                        manualCode = ""
                        Pills.info("Connected to AURA Web Player", context)
                    } else {
                        Pills.error("Please enter a valid AURA connect link or code", context)
                    }
                },
                icon = Icons.Rounded.Check,
                tone = SheetTone.Primary,
            )
        }
    }
}

/** The steps for a TV: the TV shows a code and this phone signs it in. */
@Composable
private fun TvSteps(
    signedIn: Boolean,
    onSignIn: () -> Unit,
    camera: () -> Unit,
    open: (String) -> Unit,
    share: (String) -> Unit,
    onRemote: () -> Unit,
) {
    DeviceCard("Remote control", "Use this phone as a trackpad and remote for AURA on the TV: a cursor you steer with a finger, or a pad of buttons.", compact = true) {
        Buttons({ SheetButton("Control the TV", onClick = onRemote, icon = Icons.Rounded.Tv, tone = SheetTone.Primary) })
    }
    DeviceCard("Android TV", "Sign the TV in with this phone's YouTube Music account. No typing with the remote.") {
        Step(1, "Install AURA for TV", "A separate app made for the remote. Get AURA-TV from the latest release.") {
            Buttons(
                { SheetButton("Get the app", onClick = { open(RELEASES) }, icon = Icons.Rounded.Download) },
                { SheetButton("Share", onClick = { share(RELEASES) }, icon = Icons.Rounded.Share) },
            )
        }
        Step(2, "Show the code on the TV", "On the TV, open Settings, then Account, and choose Sign in.")
        Step(3, "Scan it with this phone", "Scan it right here in AURA, or with the phone's own camera app, then approve on this phone.", last = true) {
            if (signedIn) {
                Buttons({ SheetButton("Open the camera", onClick = camera, icon = Icons.Rounded.CameraAlt, tone = SheetTone.Primary) })
            } else {
                Text(
                    "Sign in to YouTube Music on this phone first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Buttons({ SheetButton("Sign in", onClick = onSignIn, tone = SheetTone.Primary) })
            }
        }
        Buttons({ SheetButton("TV information", onClick = { open(com.aura.data.config.RemoteConfig.current.tv) }, icon = Icons.AutoMirrored.Rounded.OpenInNew) })
        Note(Icons.Rounded.Lock, "Both on the same Wi-Fi. The code lasts two minutes, and the session goes straight to the TV, encrypted. Each keeps its own library.")
    }
}

/** The row of devices, one tile each; the chosen one is filled. */
@Composable
private fun DevicePicker(selected: LinkTarget, onPick: (LinkTarget) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinkTarget.entries.forEach { t ->
            DeviceTile(t, t == selected, Modifier.weight(1f)) { onPick(t) }
        }
    }
}

@Composable
private fun DeviceTile(target: LinkTarget, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.55f, stiffness = 700f), label = "press")
    val fill by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerHigh, tween(220), label = "fill")
    val ink by animateColorAsState(if (selected) scheme.onPrimary else scheme.onSurface, tween(220), label = "ink")
    Column(
        modifier
            .graphicsLayer { scaleX = press; scaleY = press }
            .clip(RoundedCornerShape(22.dp))
            .background(fill)
            .clickable(interaction, indication = null, role = Role.Tab) { haptics.tick(); onClick() }
            .semantics { this.selected = selected }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(target.icon, null, Modifier.size(26.dp), tint = ink)
        Spacer(Modifier.height(6.dp))
        Text(target.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = ink, maxLines = 1, textAlign = TextAlign.Center)
    }
}

/** A device's card: a title, a line on what it does, then its steps. */
@Composable
private fun DeviceCard(title: String, about: String, compact: Boolean = false, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 20.dp, vertical = if (compact) 18.dp else 22.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 4.dp),
    ) {
        Text(title, style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!compact) Spacer(Modifier.height(14.dp))
        content()
    }
}

/** One numbered step, joined to the next by a line down the left. */
@Composable
private fun Step(n: Int, title: String, body: String, last: Boolean = false, extra: (@Composable () -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("$n", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).padding(vertical = 4.dp).clip(CircleShape).background(scheme.outlineVariant))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(top = 4.dp, bottom = if (last) 4.dp else 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            extra?.invoke()
        }
    }
}

/** One or two buttons side by side. */
@Composable
private fun Buttons(vararg buttons: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { b -> Box(Modifier.weight(1f)) { b() } }
    }
}

/** A small note at the bottom of a card. */
@Composable
private fun Note(icon: ImageVector, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, Modifier.size(18.dp).padding(top = 1.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val WEB_PLAYER get() = com.aura.data.config.RemoteConfig.current.web
private const val RELEASES = "https://github.com/HyperSpandan/AURA/releases/latest"
