package com.aura.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * AURA Refracted Brand Name Component.
 * Powered by WebGL 2.0 with liquid glass refraction, character lift,
 * spectral ray dispersion, and dynamic beam flow.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RefractedBrandText(
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
) {
    val context = LocalContext.current
    val htmlContent = remember {
        runCatching {
            context.assets.open("refracted_aura.html").bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    if (htmlContent != null) {
        AndroidView(
            factory = { ctx ->
                try {
                    WebView(ctx).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = View.OVER_SCROLL_NEVER
                        setOnTouchListener { _, _ -> false }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            cacheMode = WebSettings.LOAD_NO_CACHE
                        }
                        loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "utf-8", null)
                    }
                } catch (e: Exception) {
                    View(ctx)
                }
            },
            modifier = modifier
                .height(height)
                .aspectRatio(1576f / 318f)
        )
    } else {
        Text(
            text = "AURA",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
            modifier = modifier
        )
    }
}
