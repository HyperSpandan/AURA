package com.aura.ui.browse

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Link
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.Transformation
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import com.aura.data.DebugLog
import com.aura.data.covers.ArtistSignature
import com.aura.data.covers.ArtistSignatures
import com.aura.data.settings.AppSettings
import kotlinx.coroutines.launch

private val InvertToWhiteMatrix = ColorMatrix(
    floatArrayOf(
        -1f,  0f,  0f, 0f, 255f,
         0f, -1f,  0f, 0f, 255f,
         0f,  0f, -1f, 0f, 255f,
         0f,  0f,  0f, 1f,   0f,
    ),
)

/**
 * The artist's name over their photo: their handwritten signature in white
 * with "Signed by" and the name under it, when the signature archive has one,
 * and the artist's name in bold type otherwise, or until the signature has loaded.
 */
@Composable
internal fun ArtistNameOrSignature(name: String, compact: Boolean = false) {
    val ui by AppSettings.ui.collectAsState()
    val api = ui.signatureApi
    var signature by remember(name, api) { mutableStateOf(if (ui.artistSignatures) ArtistSignatures.cached(name, api) else null) }
    var drawn by remember(name, api) { mutableStateOf(false) }
    var failed by remember(name, api) { mutableStateOf(false) }

    LaunchedEffect(name, api, ui.artistSignatures) {
        if (ui.artistSignatures) {
            val cached = ArtistSignatures.cached(name, api)
            if (cached != null) {
                signature = cached
            } else {
                signature = ArtistSignatures.signatureFor(name, api)
            }
        } else {
            signature = null
        }
    }

    val sig = signature?.takeIf { !failed }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        if (sig != null) {
            // Loaded off screen first, so the name doesn't blink out for an empty space.
            SignatureImage(
                sig = sig,
                name = name,
                onDrawn = { drawn = true },
                onFailed = {
                    DebugLog.w("ArtistSignature", "Signature drawing failed for $name")
                    failed = true
                },
                shown = drawn,
                compact = compact,
            )
        }

        AnimatedContent(
            targetState = (sig != null && drawn),
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) },
            label = "signature_signed_state",
        ) { isSigned ->
            if (isSigned) {
                Text(
                    text = buildAnnotatedString {
                        append("Signed by ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(name) }
                    },
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            } else {
                Text(
                    text = name,
                    style = if (compact) {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            color = Color.White,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.95f),
                                blurRadius = 14f,
                                offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                            ),
                        )
                    } else {
                        MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 40.sp,
                            lineHeight = 44.sp,
                            color = Color.White,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.95f),
                                blurRadius = 16f,
                                offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                            ),
                        )
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SignatureImage(
    sig: ArtistSignature,
    name: String,
    onDrawn: () -> Unit,
    onFailed: () -> Unit,
    shown: Boolean,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val url = remember(sig) { sig.imageUrl.ifBlank { sig.svgUrl.orEmpty() } }
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .size(Size(1200, 600))
            .build()
    }
    // Tall signatures stay short enough to leave the photo showing; wide ones fill the width.
    val height = if (compact) {
        if (sig.aspect >= 1.4f) 64.dp else 80.dp
    } else {
        if (sig.aspect >= 1.4f) 120.dp else 150.dp
    }
    val colorFilter = if (sig.isWhiteInk) null else ColorFilter.colorMatrix(InvertToWhiteMatrix)

    AsyncImage(
        model = request,
        contentDescription = "$name's signature",
        contentScale = ContentScale.Fit,
        colorFilter = colorFilter,
        onSuccess = { onDrawn() },
        onError = { err ->
            DebugLog.w("ArtistSignature", "Failed to load signature $url: ${err.result.throwable.message}")
            onFailed()
        },
        modifier = Modifier
            .padding(horizontal = if (compact) 16.dp else 40.dp)
            .widthIn(max = if (compact) 280.dp else 420.dp)
            .fillMaxWidth()
            .height(if (shown) height else 1.dp)
            .graphicsLayer {
                alpha = if (shown) 1f else 0f
                if (!sig.isWhiteInk) {
                    blendMode = BlendMode.Screen
                }
            },
    )
}

/** A signature as white ink on clear, the paper of a scan dropped away ([ArtistSignatures.whiteInk]). */
internal object WhiteInk : Transformation() {
    override val cacheKey = "aura.whiteInk.v4"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val safeInput = if (input.config == Bitmap.Config.HARDWARE) {
            input.copy(Bitmap.Config.ARGB_8888, false) ?: input
        } else {
            input
        }
        val w = safeInput.width
        val h = safeInput.height
        val px = IntArray(w * h)
        safeInput.getPixels(px, 0, w, 0, 0, w, h)
        // Safely isolate dark strokes, or transform luma into white ink
        val ink = ArtistSignatures.whiteInk(px, w, h) ?: run {
            IntArray(w * h) { i ->
                val c = px[i]
                val a = c ushr 24
                if (a < 16) 0 else {
                    val r = (c shr 16) and 0xFF
                    val g = (c shr 8) and 0xFF
                    val b = c and 0xFF
                    val luma = (0.2126f * r + 0.7152f * g + 0.0722f * b).toInt()
                    val strokeAlpha = ((255 - luma) * a / 255).coerceIn(0, 255)
                    (strokeAlpha shl 24) or 0x00FFFFFF
                }
            }
        }
        return Bitmap.createBitmap(ink, w, h, Bitmap.Config.ARGB_8888)
    }
}

/**
 * Settings' artist-signature rows: the switch, and the service's address,
 * checked against its health address before it's kept. Blank goes back to
 * AURA's default service.
 */
@Composable
internal fun ArtistSignatureSettings() {
    val ui by AppSettings.ui.collectAsState()
    var editing by remember { mutableStateOf(false) }
    com.aura.ui.components.ToggleRow(
        "Artist signatures", ui.artistSignatures, { v -> AppSettings.updateUi { it.copy(artistSignatures = v) } },
        summary = "An artist's handwritten signature over their photo, in place of their name, when there's one",
        icon = androidx.compose.material.icons.Icons.Rounded.Draw,
        beta = true,
    )
    if (ui.artistSignatures) {
        com.aura.ui.components.SettingRow(
            "Signature service",
            summary = ui.signatureApi.removePrefix("https://").removePrefix("http://"),
            icon = androidx.compose.material.icons.Icons.Rounded.Link,
            onClick = { editing = true },
        )
    }
    if (editing) {
        var address by remember { mutableStateOf(ui.signatureApi) }
        var problem by remember { mutableStateOf<String?>(null) }
        var checking by remember { mutableStateOf(false) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!checking) editing = false },
            title = {
                com.aura.ui.components.FrostBehind(enabled = !ui.reduceBlur)
                Text("Signature service")
            },
            text = {
                Column {
                    Text("An artist-signature service, asked for each artist's signature by name.", style = MaterialTheme.typography.bodyMedium)
                    androidx.compose.material3.OutlinedTextField(
                        address, { address = it; problem = null },
                        singleLine = true,
                        placeholder = { Text(ArtistSignatures.DEFAULT_API) },
                        isError = problem != null,
                        supportingText = problem?.let { p -> { Text(p) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !checking,
                    onClick = {
                        val chosen = address.trim().ifEmpty { ArtistSignatures.DEFAULT_API }
                        val base = ArtistSignatures.normalize(chosen)
                        if (base == null) {
                            problem = "Use an address starting with https://"
                            return@TextButton
                        }
                        checking = true
                        scope.launch {
                            val ok = ArtistSignatures.reachable(base)
                            checking = false
                            if (ok) {
                                ArtistSignatures.clear()
                                AppSettings.updateUi { it.copy(signatureApi = base) }
                                editing = false
                            } else {
                                problem = "That service didn't answer"
                            }
                        }
                    },
                ) { Text(if (checking) "Checking…" else "Save") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { editing = false }, enabled = !checking) { Text("Cancel") } },
        )
    }
}
