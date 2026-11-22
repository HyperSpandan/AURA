package com.aura.tv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.aura.data.Core
import com.aura.data.Http
import com.aura.data.MusicRepository
import com.aura.data.subsonic.Subsonic
import okio.Path.Companion.toOkioPath

class TvApp : Application(), SingletonImageLoader.Factory {
    /**
     * Covers are drawn large on a TV, so they get a fair share of memory and
     * disk, on the shared HTTP client the rest of AURA uses.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { Http.client }))
                add(coil3.map.Mapper<String, String> { data, _ -> com.aura.data.library.LibraryArtwork.local(data) })
                add(coil3.map.Mapper<String, String> { data, _ -> if (Subsonic.isCoverRef(data)) Subsonic.resolveCover(data) else null })
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.15).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .crossfade(250)
            .build()

    override fun onCreate() {
        super.onCreate()
        com.aura.data.CrashLog.install(this)
        Core.init(this, phone = false)
        // The words on every screen, in the TV's own language.
        com.aura.i18n.Tr.use(this, "")
        MusicRepository.init(this)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        com.aura.i18n.Tr.use(this, "")
    }
}
