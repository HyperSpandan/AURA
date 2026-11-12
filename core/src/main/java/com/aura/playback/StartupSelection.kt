package com.aura.playback

import android.net.Uri
import androidx.media3.datasource.DataSpec
import com.aura.data.lossless.ExternalStreams
import com.aura.data.model.Song
import com.aura.data.model.durationMillis
import com.aura.data.settings.AppSettings
import com.aura.data.settings.AudioQuality
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Prefer a cached source or a pre-resolved lossless match, tried before YouTube's own
 * stream is looked up; songs in the queue resolve lossless audio ahead of time so they
 * start in lossless directly from the start without mid-song buffering.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class StartupSelection(
    private val song: Song,
    private val original: Uri,
    private val maximum: () -> Boolean = { AppSettings.effectiveAudioQuality == AudioQuality.MAX },
    /** Whether lossless is wanted at all, at any quality setting (an add-on can give it). */
    private val losslessWanted: () -> Boolean = { ExternalStreams.allowed(true) },
    /** A stream picked by hand in Search's Add-ons tab for this song, if there is one. */
    private val picked: (String) -> ExternalStreams.Rendition? = { id -> ExternalStreams.cached(id)?.takeIf { it.picked } },
    /** Whether JioSaavn may be tried before YouTube's own stream: whenever external sources are allowed, not only at Max. */
    private val saavnWanted: () -> Boolean = { ExternalStreams.allowed(false) },
    private val lookupBudgetMs: Long = 6_000L,
    private val findLossless: suspend (Song, Long) -> ExternalStreams.Rendition? = { s, d ->
        ExternalStreams.find(s, d, beforePlayback = true, losslessOnly = true)
    },
    private val findJioSaavn: suspend (Song, Long) -> ExternalStreams.Rendition? = ExternalStreams::findJioSaavn,
    private val cached: (String) -> ExternalStreams.Rendition? = ExternalStreams::cached,
) {
    private val mutex = Mutex()
    @Volatile var selected: Uri? = null
        private set
    suspend fun resolve(spec: DataSpec): DataSpec = mutex.withLock {
        currentCoroutineContext().ensureActive()
        if (selected == null) {
            // A lossless file found ahead of time starts the song whenever lossless is on, not only at Max.
            // A stream picked by hand in Search starts the song whatever the quality setting.
            val ready = picked(song.videoId)?.takeIf { it.mimeType == null }
                ?: if (maximum() || losslessWanted()) cached(song.videoId)?.takeIf { it.mimeType == null && it.lossless } else null

            // If lossless is wanted and an in-flight prefetch is active, await it within budget to start in lossless directly
            val rendition = ready ?: if (maximum() || losslessWanted()) try {
                val lookup = ExternalStreams.background.async { findLossless(song, song.durationMillis()) }
                withTimeoutOrNull(lookupBudgetMs) { lookup.await() }?.takeIf { it.mimeType == null && it.lossless }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } else null

            // Fallback to JioSaavn if lossless is not available
            val chosen = rendition ?: if (maximum() || saavnWanted()) try {
                val lookup = ExternalStreams.background.async { findJioSaavn(song, song.durationMillis()) }
                withTimeoutOrNull(lookupBudgetMs) { lookup.await() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            } else null

            selected = chosen?.let { externalStreamUri(song.videoId, it.key) } ?: original
            com.aura.data.DebugLog.d("StartupSelection", "${song.videoId} starts on " + when {
                chosen != null -> chosen.label
                maximum() || losslessWanted() || saavnWanted() -> "YouTube: no external match within ${lookupBudgetMs / 1000}s"
                else -> "YouTube: external sources are off for this quality setting"
            })
        }
        spec.withUri(selected!!)
    }
}

internal fun startupToken(uri: Uri): String? = if (videoIdOf(uri) != null) uri.getQueryParameter("startup") else null
internal fun withoutStartup(uri: Uri): Uri = uri.buildUpon().clearQuery().apply {
    uri.queryParameterNames.filterNot { it == "startup" }.forEach { name ->
        uri.getQueryParameters(name).forEach { appendQueryParameter(name, it) }
    }
}.build()
internal fun youtubeFallbackUri(id: String, upgraded: Boolean = false): Uri = streamUri(id, upgraded).buildUpon().appendQueryParameter("fallback", "youtube").build()



