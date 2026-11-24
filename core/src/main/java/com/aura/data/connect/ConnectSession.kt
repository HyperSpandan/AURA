package com.aura.data.connect

import com.aura.data.together.NostrEvent
import com.aura.data.together.RelayPool
import com.aura.data.together.Schnorr
import com.aura.data.together.Together
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ConnectDevice(
    val id: String,
    val name: String,
    val form: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val state: ConnectMsg.State? = null,
)

data class ConnectView(
    val myId: String,
    val myName: String,
    val relays: Int = 0,
    val devices: List<ConnectDevice> = emptyList(),
    val activeId: String? = null,
)

/**
 * Android Connect session over Nostr relays:
 * Announces phone presence, broadcasts local playback state, receives
 * commands from the web player, and supports bi-directional playback transfer.
 */
class ConnectSession(
    val group: ConnectGroup,
    val myId: String,
    val myName: String,
    private val onCommand: (ConnectMsg.Command) -> Unit = {},
    private val onTransfer: (ConnectMsg.Transfer) -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionKey = Schnorr.newPrivateKey()
    private var pool: RelayPool? = null
    private var tickerJob: Job? = null
    private var eventsJob: Job? = null
    private var seq: Long = System.currentTimeMillis()

    private val deviceMap = mutableMapOf<String, ConnectDevice>()
    private val _view = MutableStateFlow(ConnectView(myId = myId, myName = myName))
    val view: StateFlow<ConnectView> = _view.asStateFlow()

    @Volatile
    private var lastState: ConnectMsg.State? = null

    @Volatile
    private var lastPresenceSent = 0L

    fun start() {
        val filter = buildJsonObject {
            put("kinds", buildJsonArray { add(JsonPrimitive(KIND)) })
            put("#t", buildJsonArray { add(JsonPrimitive(group.tag)) })
            put("since", (System.currentTimeMillis() / 1000) - 20)
        }
        val p = RelayPool(CONNECT_RELAYS, filter)
        pool = p
        p.start()

        eventsJob = scope.launch {
            p.events.collect { event ->
                if (event.kind == KIND) {
                    runCatching { handleEvent(event) }
                }
            }
        }

        scope.launch {
            p.connected.collect { count ->
                _view.value = _view.value.copy(relays = count)
                if (count > 0) {
                    sayPresence()
                    lastState?.let { sendState(it) }
                }
            }
        }

        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                tick()
            }
        }

        sayPresence()
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        var changed = false
        val iterator = deviceMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value.lastSeen > GONE_MS) {
                iterator.remove()
                changed = true
            }
        }
        if (changed) updateView()

        if (now - lastPresenceSent > PRESENCE_MS) {
            sayPresence()
        }
    }

    fun sayPresence() {
        lastPresenceSent = System.currentTimeMillis()
        val msg = ConnectMsg.Presence(
            from = myId,
            name = myName,
            form = "phone",
            seq = ++seq,
            at = lastPresenceSent,
            active = lastState?.playing == true,
        )
        send(msg)
    }

    fun broadcastState(
        track: CTrack?,
        playing: Boolean,
        pos: Long,
        queue: List<CTrack>,
        volume: Double = 1.0,
        shuffle: Boolean = false,
        repeat: String = "off",
        quality: String? = null,
    ) {
        val state = ConnectMsg.State(
            from = myId,
            seq = ++seq,
            at = System.currentTimeMillis(),
            track = track,
            playing = playing,
            pos = pos,
            queue = queue.take(100),
            volume = volume,
            shuffle = shuffle,
            repeat = repeat,
            since = if (playing) System.currentTimeMillis() else null,
            quality = quality,
        )
        lastState = state
        sendState(state)
    }

    private fun sendState(state: ConnectMsg.State) {
        send(state)
        updateView()
    }

    fun command(
        targetId: String,
        action: String,
        pos: Long? = null,
        value: Double? = null,
        track: CTrack? = null,
        tracks: List<CTrack>? = null,
        mode: String? = null,
    ) {
        val cmd = ConnectMsg.Command(
            from = myId,
            to = targetId,
            id = java.util.UUID.randomUUID().toString().take(8),
            action = action,
            seq = ++seq,
            at = System.currentTimeMillis(),
            pos = pos,
            value = value,
            track = track,
            tracks = tracks,
            mode = mode,
        )
        send(cmd)
    }

    fun transferTo(targetId: String, queue: List<CTrack>, index: Int, pos: Long, playing: Boolean) {
        val transfer = ConnectMsg.Transfer(
            from = myId,
            to = targetId,
            id = java.util.UUID.randomUUID().toString().take(8),
            queue = queue.take(100),
            index = index,
            pos = pos,
            playing = playing,
            seq = ++seq,
            at = System.currentTimeMillis(),
        )
        send(transfer)
    }

    private fun handleEvent(event: NostrEvent) {
        val plain = group.open(event.content) ?: return
        val msg = ConnectMsg.decode(plain) ?: return
        if (msg.from == myId) return

        when (msg) {
            is ConnectMsg.Presence -> {
                val existing = deviceMap[msg.from]
                deviceMap[msg.from] = ConnectDevice(
                    id = msg.from,
                    name = msg.name,
                    form = msg.form,
                    lastSeen = System.currentTimeMillis(),
                    state = existing?.state,
                )
                updateView()
            }
            is ConnectMsg.State -> {
                val existing = deviceMap[msg.from]
                deviceMap[msg.from] = ConnectDevice(
                    id = msg.from,
                    name = existing?.name ?: "Web Player",
                    form = existing?.form ?: "web",
                    lastSeen = System.currentTimeMillis(),
                    state = msg,
                )
                updateView()
            }
            is ConnectMsg.Command -> {
                if (msg.to == myId) {
                    onCommand(msg)
                    send(ConnectMsg.Ack(from = myId, to = msg.from, id = msg.id, ok = true, seq = ++seq))
                }
            }
            is ConnectMsg.Transfer -> {
                if (msg.to == myId) {
                    onTransfer(msg)
                    send(ConnectMsg.Ack(from = myId, to = msg.from, id = msg.id, ok = true, seq = ++seq))
                }
            }
            is ConnectMsg.Leave -> {
                deviceMap.remove(msg.from)
                updateView()
            }
            else -> {}
        }
    }

    private fun updateView() {
        val playingRemote = deviceMap.values
            .filter { it.id != myId && it.state?.playing == true }
            .maxByOrNull { it.state?.since ?: it.lastSeen }

        val activeRemoteWithTrack = deviceMap.values
            .filter { it.id != myId && it.state?.track != null }
            .maxByOrNull { it.state?.since ?: it.lastSeen }

        val phonePlaying = lastState?.playing == true
        val phoneHasTrack = lastState?.track != null

        _view.value = _view.value.copy(
            devices = deviceMap.values.toList(),
            activeId = when {
                phonePlaying -> myId
                playingRemote != null -> playingRemote.id
                activeRemoteWithTrack != null && !phoneHasTrack -> activeRemoteWithTrack.id
                phoneHasTrack -> myId
                activeRemoteWithTrack != null -> activeRemoteWithTrack.id
                else -> myId
            }
        )
    }

    private fun send(msg: ConnectMsg) {
        val p = pool ?: return
        scope.launch(Dispatchers.Default) {
            val encoded = ConnectMsg.encode(msg)
            val sealed = group.seal(encoded)
            val event = NostrEvent.signed(
                privateKey = sessionKey,
                kind = KIND,
                tags = listOf(listOf("t", group.tag)),
                content = sealed,
            )
            p.publish(event)
        }
    }

    fun stop() {
        send(ConnectMsg.Leave(from = myId, seq = ++seq))
        tickerJob?.cancel()
        eventsJob?.cancel()
        pool?.close()
        pool = null
    }

    companion object {
        val CONNECT_RELAYS = listOf(
            "wss://relay.damus.io",
            "wss://relay.primal.net",
            "wss://nos.lol",
        )
        const val KIND = 21421
        const val PRESENCE_MS = 25_000L
        const val GONE_MS = 75_000L
        const val TICK_MS = 5_000L
    }
}
