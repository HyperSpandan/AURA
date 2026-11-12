package com.aura

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.aura.data.Http
import com.aura.data.MusicRepository
import com.aura.data.subsonic.Subsonic
import com.aura.ui.ScreenCache
import okio.Path.Companion.toOkioPath

class AuraApp : Application(), SingletonImageLoader.Factory {
    /**
     * Artwork on the app's shared HTTP client (warm connections to Google's
     * image hosts), tuned cache proportion (conserving heap on low-RAM devices),
     * and 100 MB on disk, with a short cross-fade as each one lands.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = am?.isLowRamDevice == true
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Http.client }))
                add(coil3.svg.SvgDecoder.Factory())
                add(coil3.map.Mapper<String, String> { data, _ -> com.aura.data.library.LibraryArtwork.local(data) })
                // A cover on your own server is stored without credentials; sign it here.
                add(coil3.map.Mapper<String, String> { data, _ -> if (Subsonic.isCoverRef(data)) Subsonic.resolveCover(data) else null })
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, if (isLowRam) 0.12 else 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .crossfade(200)
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            ScreenCache.clear()
            SingletonImageLoader.get(this).memoryCache?.clear()
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            ScreenCache.trim(16)
            SingletonImageLoader.get(this).memoryCache?.let { cache ->
                cache.trimToSize(cache.size / 2)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ScreenCache.clear()
        SingletonImageLoader.get(this).memoryCache?.clear()
    }

    override fun onCreate() {
        super.onCreate()
        // First, so a crash anywhere after this is kept for Export diagnostics.
        com.aura.data.CrashLog.install(this)
        // The home-screen widget follows the shared player service.
        com.aura.data.AppHooks.onPlayerChanged = { context, player -> com.aura.widget.NowPlayingWidget.refresh(context, player) }
        com.aura.data.AppHooks.onPlayerStopped = { context -> com.aura.widget.NowPlayingWidget.stopped(context) }
        java.util.Locale.setDefault(com.aura.ui.theme.AppLanguages.wrap(this).resources.configuration.locales[0])
        com.aura.data.Core.init(this)
        // Spotify's personal song radio, when Spotify is connected; the cookie stays with the WebView.
        com.aura.data.spotify.SpotifyWebPlayer.account = {
            if (com.aura.ui.spotify.SpotifyConnection.connected.value) com.aura.ui.spotify.SpotifyConnection.cookie() else null
        }
        // Last session's Home, shown at once and refreshed in the background.
        MusicRepository.init(this)?.let { ScreenCache.put("home:${com.aura.data.settings.AppSettings.playback.value.recommender.name}", it, at = 0L) }
        // After history, library, downloads and account: they tell an existing user from a new one.
        com.aura.data.Welcome.settle(this)
    }
}
