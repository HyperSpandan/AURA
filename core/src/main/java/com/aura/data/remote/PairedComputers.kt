package com.aura.data.remote

import com.aura.data.account.PairingCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** A computer or browser paired with this phone via AURA Connect. */
@Serializable
data class PairedComputer(
    val id: String,
    val name: String,
    val secret: String,
    val linkedAt: Long = System.currentTimeMillis(),
)

/** Storage for paired computers / browsers. */
class PairedComputers(private val storage: RemoteStorage) {
    private var items: List<PairedComputer> = storage.read()
        ?.let { runCatching { store.decodeFromString(ListSerializer(PairedComputer.serializer()), it) }.getOrNull() }
        ?: emptyList()
    private val _computers = MutableStateFlow(items)
    val computers: StateFlow<List<PairedComputer>> = _computers.asStateFlow()

    @Synchronized
    fun add(computer: PairedComputer) {
        val filtered = items.filterNot { it.id == computer.id || it.secret == computer.secret }
        update(filtered + computer)
    }

    @Synchronized
    fun remove(id: String) {
        update(items.filterNot { it.id == id })
    }

    @Synchronized
    fun clear() {
        update(emptyList())
    }

    private fun update(next: List<PairedComputer>) {
        items = next
        storage.write(store.encodeToString(ListSerializer(PairedComputer.serializer()), next))
        _computers.value = next
    }

    companion object {
        private val store = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        const val WEB_PLAYER = "https://hyperspandan.github.io/AURA/player/"
        private val ENCODED = Regex("^[A-Za-z0-9_-]{43}$")

        /**
         * Parses a secret from a QR code, join link, or raw code.
         * Returns the 43-character base64url secret or null if invalid.
         */
        fun parse(text: String?): String? {
            val value = text?.trim() ?: return null
            val raw = when {
                value.startsWith("aura://connect/#") -> value.removePrefix("aura://connect/#")
                value.contains("/settings/devices") || value.contains("join=") -> {
                    Regex("""[?&#](?:join|connect)=([A-Za-z0-9_-]{43})""").find(value)?.groupValues?.get(1).orEmpty()
                }
                value.startsWith("aura://pair/#") || value.startsWith("aura://remote/#") -> return null
                value.contains("://") -> return null
                else -> value
            }
            return if (ENCODED.matches(raw)) raw else null
        }

        fun generateSecret(): String = PairingCrypto.encode(PairingCrypto.secret())
        fun webLink(secret: String): String = "$WEB_PLAYER#/settings/devices?join=$secret"
        fun appLink(secret: String): String = "aura://connect/#$secret"
    }
}
