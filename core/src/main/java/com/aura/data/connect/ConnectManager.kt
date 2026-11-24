package com.aura.data.connect

import android.content.Context
import com.aura.data.model.Song
import com.aura.data.remote.PairedComputers
import com.aura.data.remote.PrefsStorage
import com.aura.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Manages the active AURA Connect session on Android.
 * Bridges playback events between Android's [PlayerConnection] and the
 * web player/computer over Nostr relays.
 */
object ConnectManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var session: ConnectSession? = null
    private var player: PlayerConnection? = null
    private var syncJob: Job? = null
    private var viewJob: Job? = null
    private var currentSecret: String? = null

    private val _view = MutableStateFlow(ConnectView(myId = "", myName = "This phone"))
    val view: StateFlow<ConnectView> = _view.asStateFlow()

    private val _isLocalPlaying = MutableStateFlow(false)
    val isLocalPlaying: StateFlow<Boolean> = _isLocalPlaying.asStateFlow()

    private val _activeRemoteDevice = MutableStateFlow<ConnectDevice?>(null)
    val activeRemoteDevice: StateFlow<ConnectDevice?> = _activeRemoteDevice.asStateFlow()

    fun computeActiveRemote(v: ConnectView, phonePlaying: Boolean): ConnectDevice? {
        if (phonePlaying) return null

        val playingRemote = v.devices.firstOrNull { it.id != v.myId && it.state?.playing == true }
        if (playingRemote != null) return playingRemote

        if (v.activeId != null && v.activeId != v.myId) {
            val dev = v.devices.firstOrNull { it.id == v.activeId && it.state?.track != null }
            if (dev != null) return dev
        }

        val phoneHasTrack = player?.currentSong?.value != null
        if (!phoneHasTrack) {
            return v.devices.firstOrNull { it.id != v.myId && it.state?.track != null }
        }

        return null
    }

    fun bind(context: Context, playerConnection: PlayerConnection) {
        appContext = context.applicationContext
        player = playerConnection

        scope.launch {
            playerConnection.isPlaying.collect { playing ->
                _isLocalPlaying.value = playing
                if (playing) {
                    pauseActiveRemote()
                } else {
                    _activeRemoteDevice.value = computeActiveRemote(_view.value, playing)
                }
            }
        }

        scope.launch(Dispatchers.IO) {
            val secret = resolveSecret(context)
            if (secret != null) {
                start(context, secret)
            }
        }
    }

    fun pauseActiveRemote() {
        val s = session ?: return
        val active = _activeRemoteDevice.value
            ?: _view.value.devices.firstOrNull { it.id != _view.value.myId && it.state?.playing == true }
            ?: return
        s.command(active.id, "pause")
        _activeRemoteDevice.value = null
    }

    fun resolveSecret(context: Context): String? {
        // 1. Paired computers list
        val paired = PairedComputers(PrefsStorage(context, "remote_computers"))
        val firstComputer = paired.computers.value.firstOrNull()
        if (firstComputer != null) return firstComputer.secret

        // 2. Explicit group secret
        val explicit = PrefsStorage(context, "connect_group_secret").read()
        if (explicit != null && PairedComputers.parse(explicit) != null) return PairedComputers.parse(explicit)

        // 3. Phone pairing secret
        val phoneSec = PrefsStorage(context, "connect_phone_secret").read()
        if (phoneSec != null && PairedComputers.parse(phoneSec) != null) return PairedComputers.parse(phoneSec)

        return null
    }

    @Synchronized
    fun start(context: Context, secretText: String) {
        val parsedSecret = PairedComputers.parse(secretText) ?: return
        if (session != null && currentSecret == parsedSecret) return

        appContext = context.applicationContext
        stop()
        currentSecret = parsedSecret
        PrefsStorage(context, "connect_group_secret").write(parsedSecret)

        val group = ConnectGroup.fromSecret(parsedSecret)
        val myId = "phone_" + UUID.randomUUID().toString().take(8)

        val newSession = ConnectSession(
            group = group,
            myId = myId,
            myName = "AURA Android",
            onCommand = { handleCommand(it) },
            onTransfer = { handleTransfer(it) },
        )
        session = newSession
        newSession.start()

        var lastPlayingRemoteId: String? = null
        viewJob?.cancel()
        viewJob = scope.launch {
            newSession.view.collect { v ->
                _view.value = v

                val phonePlaying = _isLocalPlaying.value
                val playingRemote = v.devices.firstOrNull { it.id != v.myId && it.state?.playing == true }
                _activeRemoteDevice.value = computeActiveRemote(v, phonePlaying)

                // When remote device starts playing, pause local playback if phone was playing
                if (playingRemote != null && playingRemote.id != lastPlayingRemoteId && phonePlaying) {
                    player?.let { p ->
                        withContext(Dispatchers.Main) { p.pause() }
                    }
                }
                lastPlayingRemoteId = playingRemote?.id
            }
        }

        startSyncLoop()
    }

    fun broadcastCurrentState(forcedPos: Long? = null, forcedPlaying: Boolean? = null) {
        val p = player ?: return
        val s = session ?: return
        scope.launch(Dispatchers.Default) {
            val song = p.currentSong.value
            val isPlaying = forcedPlaying ?: p.isPlaying.value
            val queue = p.queue.value
            val shuffle = p.shuffleEnabled.value
            val durMs = p.durationMs.value
            val cTrack = song?.let { songToCTrack(it, durMs) }
            val cQueue = queue.take(25).map { songToCTrack(it) }
            val pos = forcedPos ?: runCatching { withContext(Dispatchers.Main) { p.currentPositionMs() } }.getOrDefault(0L)
            val vol: Double = appContext?.let { ctx ->
                val am = ctx.getSystemService(android.media.AudioManager::class.java)
                val max = am?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
                val cur = am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: max
                (cur.toDouble() / max.toDouble()).coerceIn(0.0, 1.0)
            } ?: 1.0

            s.broadcastState(
                track = cTrack,
                playing = isPlaying,
                pos = pos,
                queue = cQueue,
                volume = vol,
                shuffle = shuffle,
            )
        }
    }

    fun broadcastStateExplicit(
        track: CTrack?,
        playing: Boolean,
        pos: Long,
        queue: List<CTrack>? = null,
    ) {
        val s = session ?: return
        scope.launch(Dispatchers.Default) {
            val vol: Double = appContext?.let { ctx ->
                val am = ctx.getSystemService(android.media.AudioManager::class.java)
                val max = am?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 1
                val cur = am?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: max
                (cur.toDouble() / max.toDouble()).coerceIn(0.0, 1.0)
            } ?: 1.0

            s.broadcastState(
                track = track,
                playing = playing,
                pos = pos,
                queue = queue ?: emptyList(),
                volume = vol,
                shuffle = player?.shuffleEnabled?.value ?: false,
            )
        }
    }

    private fun startSyncLoop() {
        val p = player ?: return

        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.Default) {
            // Core state changes including duration resolution
            launch {
                combine(
                    p.currentSong,
                    p.isPlaying,
                    p.queue,
                    p.shuffleEnabled,
                    p.durationMs,
                ) { song, isPlaying, queue, shuffle, durationMs ->
                    SyncSnapshot(song, isPlaying, queue, shuffle, durationMs)
                }.distinctUntilChanged().collectLatest {
                    broadcastCurrentState()
                }
            }

            // Immediately broadcast on seeks and skips
            launch {
                p.jumps.collect {
                    broadcastCurrentState()
                }
            }

            // Periodic heartbeat broadcast every 5s while playing so remotes stay in lockstep
            launch {
                while (true) {
                    kotlinx.coroutines.delay(5000)
                    if (p.isPlaying.value) {
                        broadcastCurrentState()
                    }
                }
            }
        }
    }

    private fun handleCommand(cmd: ConnectMsg.Command) {
        val p = player ?: return
        scope.launch(Dispatchers.Main) {
            when (cmd.action) {
                "play" -> {
                    p.play()
                    broadcastCurrentState(forcedPlaying = true)
                }
                "pause" -> {
                    p.pause()
                    broadcastCurrentState(forcedPlaying = false)
                }
                "toggle" -> {
                    val willPlay = !p.isPlaying.value
                    if (willPlay) p.play() else p.pause()
                    broadcastCurrentState(forcedPlaying = willPlay)
                }
                "next" -> p.skipNext()
                "previous" -> p.skipPrevious()
                "seek" -> cmd.pos?.let {
                    p.seekTo(it)
                    broadcastCurrentState(forcedPos = it)
                }
                "shuffle" -> p.cycleShuffle()
                "repeat" -> p.cycleRepeatMode()
                "volume" -> cmd.value?.let { vol ->
                    appContext?.let { ctx ->
                        val am = ctx.getSystemService(android.media.AudioManager::class.java)
                        if (am != null) {
                            val max = am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val target = kotlin.math.round(vol * max).toInt().coerceIn(0, max)
                            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, target, 0)
                            broadcastCurrentState()
                        }
                    }
                }
                "playTrack" -> cmd.track?.let { ct ->
                    val song = cTrackToSong(ct)
                    p.play(song, "Connected device")
                    broadcastStateExplicit(
                        track = ct,
                        playing = true,
                        pos = 0L,
                    )
                }
                "playTracks", "load" -> {
                    val tracks = cmd.tracks ?: cmd.track?.let { listOf(it) }
                    if (!tracks.isNullOrEmpty()) {
                        val songs = tracks.map { cTrackToSong(it) }
                        val startIndex = cmd.pos?.toInt()?.coerceIn(0, songs.size - 1) ?: 0
                        p.playAll(songs, startIndex = startIndex, shuffle = false, source = "Connected device")
                        val currentCTrack = tracks.getOrNull(startIndex) ?: tracks.first()
                        broadcastStateExplicit(
                            track = currentCTrack,
                            playing = true,
                            pos = 0L,
                            queue = tracks.drop(startIndex + 1),
                        )
                    }
                }
                "playNext" -> {
                    val tracks = cmd.tracks ?: cmd.track?.let { listOf(it) }
                    if (!tracks.isNullOrEmpty()) {
                        val songs = tracks.map { cTrackToSong(it) }
                        songs.reversed().forEach { p.playNext(it) }
                        broadcastCurrentState()
                    }
                }
                "addToQueue" -> {
                    val tracks = cmd.tracks ?: cmd.track?.let { listOf(it) }
                    if (!tracks.isNullOrEmpty()) {
                        val songs = tracks.map { cTrackToSong(it) }
                        songs.forEach { p.addToQueue(it) }
                        broadcastCurrentState()
                    }
                }
                "jump" -> {
                    cmd.pos?.toInt()?.let { index ->
                        p.playQueueItem(index)
                        broadcastCurrentState()
                    }
                }
                "remove" -> {
                    cmd.pos?.toInt()?.let { index ->
                        p.removeQueueItem(index)
                        broadcastCurrentState()
                    }
                }
            }
        }
    }

    private fun handleTransfer(transfer: ConnectMsg.Transfer) {
        val p = player ?: return
        val queue = transfer.queue?.map { cTrackToSong(it) } ?: emptyList()
        if (queue.isNotEmpty()) {
            scope.launch(Dispatchers.Main) {
                p.playAll(
                    songs = queue,
                    startIndex = transfer.index ?: 0,
                    startPositionMs = transfer.pos ?: 0L,
                    shuffle = false,
                )
            }
        }
    }

    fun transferTo(targetDeviceId: String): Boolean {
        val s = session ?: return false
        val p = player ?: return false

        scope.launch(Dispatchers.Main) {
            val currentSong = p.currentSong.value
            val queue = p.queue.value
            val currentIndex = p.currentIndex.value
            val currentPos = runCatching { p.currentPositionMs() }.getOrDefault(0L)
            val isPlaying = p.isPlaying.value

            val songsToSend = if (queue.isNotEmpty()) queue else listOfNotNull(currentSong)
            val cQueue = songsToSend.map { songToCTrack(it) }

            // Pause phone locally
            p.pause()

            // Transfer to target device
            s.transferTo(
                targetId = targetDeviceId,
                queue = cQueue,
                index = currentIndex.coerceIn(0, (cQueue.size - 1).coerceAtLeast(0)),
                pos = currentPos,
                playing = isPlaying,
            )
        }
        return true
    }

    fun pullToPhone(): Boolean {
        val s = session ?: return false
        val p = player ?: return false
        val myId = view.value.myId
        val remote = view.value.devices.firstOrNull { it.id != myId && it.state?.playing == true }
            ?: view.value.devices.firstOrNull { it.id == view.value.activeId && it.id != myId && it.state?.track != null }
            ?: view.value.devices.firstOrNull { it.id != myId && it.state?.track != null }
            ?: return false
        val state = remote.state ?: return false
        val track = state.track ?: return false

        // Pause remote playback
        s.command(remote.id, "pause")

        // Build queue
        val currentSong = cTrackToSong(track)
        val queueSongs = state.queue?.map { cTrackToSong(it) }?.filter { it.videoId.isNotBlank() } ?: emptyList()
        val allSongs = if (queueSongs.any { it.videoId == currentSong.videoId }) queueSongs else listOf(currentSong) + queueSongs
        val startIndex = allSongs.indexOfFirst { it.videoId == currentSong.videoId }.coerceAtLeast(0)

        val elapsed = if (state.playing == true && state.at > 0) (System.currentTimeMillis() - state.at).coerceIn(0L, 30_000L) else 0L
        val startPos = (state.pos ?: 0L) + elapsed

        scope.launch(Dispatchers.Main) {
            p.playAll(
                songs = allSongs,
                startIndex = startIndex,
                startPositionMs = startPos,
                shuffle = state.shuffle == true,
            )
        }
        return true
    }

    fun sendCommand(targetDeviceId: String, action: String, pos: Long? = null) {
        session?.command(targetDeviceId, action, pos = pos)
    }

    fun sendCommand(action: String, targetId: String? = null, pos: Long? = null, value: Double? = null): Boolean {
        val s = session ?: return false
        val target = targetId ?: _activeRemoteDevice.value?.id ?: _view.value.activeId ?: return false
        s.command(target, action, pos = pos, value = value)
        return true
    }

    fun sendPlayTrack(targetDeviceId: String, song: Song): Boolean {
        val s = session ?: return false
        val cTrack = songToCTrack(song)
        s.command(targetDeviceId, "playTrack", track = cTrack)
        return true
    }

    fun sendPlayTracks(targetDeviceId: String, songs: List<Song>, startIndex: Int = 0): Boolean {
        val s = session ?: return false
        val cTracks = songs.map { songToCTrack(it) }
        s.command(targetDeviceId, "playTracks", pos = startIndex.toLong(), tracks = cTracks)
        return true
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
        viewJob?.cancel()
        viewJob = null
        session?.stop()
        session = null
        currentSecret = null
    }

    fun formatMs(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSec = ms / 1000L
        val m = totalSec / 60L
        val s = totalSec % 60L
        return "$m:${s.toString().padStart(2, '0')}"
    }

    fun songToCTrack(song: Song, durMs: Long? = null): CTrack {
        val len = song.durationText?.takeIf { it.isNotBlank() } ?: durMs?.takeIf { it > 0L }?.let { formatMs(it) }
        return CTrack(
            id = song.videoId,
            title = song.title,
            artist = song.artist,
            thumb = song.thumbnailUrl,
            length = len,
            album = song.albumName,
        )
    }

    fun cTrackToSong(track: CTrack): Song = Song(
        videoId = track.id,
        title = track.title,
        artist = track.artist ?: "Unknown Artist",
        thumbnailUrl = track.thumb,
        durationText = track.length,
        albumName = track.album,
    )

    fun lengthToMs(length: String?): Long {
        if (length.isNullOrBlank()) return 0L
        val parts = length.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60 + parts[1]) * 1000L
            3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
            else -> 0L
        }
    }

    private data class SyncSnapshot(
        val song: Song?,
        val isPlaying: Boolean,
        val queue: List<Song>,
        val shuffle: Boolean,
        val durationMs: Long = 0L,
    )
}
