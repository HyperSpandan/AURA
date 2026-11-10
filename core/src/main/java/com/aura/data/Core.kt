package com.aura.data

import android.app.Application
import com.aura.data.account.AccountStore
import com.aura.data.autoeq.AutoEq
import com.aura.data.covers.AlbumCovers
import com.aura.data.download.Downloads
import com.aura.data.history.History
import com.aura.data.innertube.InnerTubeXResolver
import com.aura.data.innertube.UpgradedTracks
import com.aura.data.lastfm.LastFm
import com.aura.data.library.LibraryStore
import com.aura.data.listenbrainz.ListenBrainz
import com.aura.data.settings.AppSettings
import com.aura.data.subsonic.Subsonic
import com.aura.playback.QueueStore

/** Starts the shared services, in the order they depend on each other. */
object Core {
    /**
     * [phone] is false on the watch, which leaves out what only the phone
     * and TV app does: new-release alerts, update checks and usage counts.
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun init(app: Application, phone: Boolean = true) {
        // Media3's warnings and errors (a failed load, a decoder that won't
        // start) into the app's own log as well as logcat, for "Copy log".
        androidx.media3.common.util.Log.setLogger(object : androidx.media3.common.util.Log.Logger {
            private val system = androidx.media3.common.util.Log.Logger.DEFAULT
            override fun d(tag: String, message: String, throwable: Throwable?) = system.d(tag, message, throwable)
            override fun i(tag: String, message: String, throwable: Throwable?) = system.i(tag, message, throwable)
            override fun w(tag: String, message: String, throwable: Throwable?) {
                AppLog.add('W', tag, message, throwable)
                system.w(tag, message, throwable)
            }
            override fun e(tag: String, message: String, throwable: Throwable?) {
                AppLog.add('E', tag, message, throwable)
                system.e(tag, message, throwable)
            }
        })
        AppSettings.init(app)
        com.aura.data.config.RemoteConfig.init(app)
        com.aura.data.addons.AddonStore.init(app)
        com.aura.data.addons.AddonOutput.init(app)
        com.aura.data.addons.AddonStore.onChanged = com.aura.data.lossless.ExternalStreams::retryLookups
        History.init(app)
        com.aura.data.history.ListenedCollections.init(app)
        com.aura.data.covers.ArtistPhotos.init(app)
        LoudnessStore.init(app)
        UpgradedTracks.init(app)
        QueueStore.init(app)
        com.aura.data.together.Together.init(app)
        LastFm.init(app)
        Subsonic.init(app)
        ListenBrainz.init(app)
        com.aura.data.radio.Radio.init(app)
        if (phone) {
            com.aura.data.addons.AddonRefreshWorker.schedule(app)
            com.aura.data.releases.NewReleases.init(app)
            com.aura.data.UpdateCheck.init(app)
            com.aura.data.UpdateLock.init(app)
            com.aura.data.UpdateCheck.schedule(app)
            com.aura.data.Usage.init(app)
        }
        // Spotify sign-in is gone; drop any tokens an earlier version kept.
        app.deleteSharedPreferences("spotify")
        // The waveform seek bar is gone; drop the waveforms an earlier version saved.
        java.io.File(app.filesDir, "waveforms").takeIf { it.exists() }?.let { dir -> Thread { dir.deleteRecursively() }.start() }
        com.aura.data.podcasts.Podcasts.init(app)
        com.aura.data.library.ResumePoints.init(app)
        AlbumCovers.init(app)
        com.aura.data.spotify.SpotifyWebPlayer.init(app)
        com.aura.data.spotify.SpotifySync.init(app)
        AutoEq.init(app)
        com.aura.data.library.LibraryArtwork.init(app)
        LibraryStore.init(app)
        Downloads.init(app)
        com.aura.data.download.DownloadedLists.init(app)
        AccountStore.init(app)
        com.aura.data.library.LibraryCache.init(app)
        // After library and account: it sends playlist changes still waiting, when signed in.
        com.aura.data.library.PlaylistSync.init(app)
        InnerTubeXResolver.init(app)
    }

    /**
     * Saves at once what's still waiting for its save (each store waits a moment
     * after a change, so a run of changes is written once): the app is leaving
     * the screen or the player is closing, and the process may go any moment.
     */
    fun flush() {
        LibraryStore.flush()
        History.flush()
        com.aura.data.history.ListenedCollections.flush()
        Downloads.flush()
    }
}
