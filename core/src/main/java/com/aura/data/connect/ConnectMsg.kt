package com.aura.data.connect

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** A song representation passed between connected devices. */
@Serializable
data class CTrack(
    val id: String,
    val title: String,
    val artist: String? = null,
    val thumb: String? = null,
    val length: String? = null,
    val album: String? = null,
)

/** Sealed message hierarchy for AURA Connect over Nostr relays. */
sealed interface ConnectMsg {
    val from: String
    val seq: Long
    val at: Long

    @Serializable
    data class Presence(
        override val from: String,
        val name: String,
        val form: String = "phone",
        override val seq: Long,
        override val at: Long = System.currentTimeMillis(),
        val active: Boolean? = null,
    ) : ConnectMsg

    @Serializable
    data class State(
        override val from: String,
        override val seq: Long,
        override val at: Long = System.currentTimeMillis(),
        val track: CTrack? = null,
        val playing: Boolean? = null,
        val pos: Long? = null,
        val queue: List<CTrack>? = null,
        val volume: Double? = null,
        val shuffle: Boolean? = null,
        val repeat: String? = null,
        val since: Long? = null,
        val quality: String? = null,
    ) : ConnectMsg

    @Serializable
    data class Command(
        override val from: String,
        val to: String,
        val id: String,
        val action: String,
        override val seq: Long,
        override val at: Long = System.currentTimeMillis(),
        val pos: Long? = null,
        val value: Double? = null,
        val track: CTrack? = null,
        val tracks: List<CTrack>? = null,
        val mode: String? = null,
    ) : ConnectMsg

    @Serializable
    data class Ack(
        override val from: String,
        val to: String,
        val id: String,
        val ok: Boolean = true,
        val reason: String? = null,
        override val seq: Long,
        override val at: Long = System.currentTimeMillis(),
    ) : ConnectMsg

    @Serializable
    data class Transfer(
        override val from: String,
        val to: String,
        val id: String,
        val queue: List<CTrack>? = null,
        val index: Int? = null,
        val pos: Long? = null,
        val playing: Boolean? = null,
        override val seq: Long,
        override val at: Long = System.currentTimeMillis(),
    ) : ConnectMsg

    @Serializable
    data class Leave(
        override val from: String,
        override val seq: Long,
        override val at: Long = System.currentTimeMillis(),
        val next: String? = null,
    ) : ConnectMsg

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun decode(text: String): ConnectMsg? = runCatching {
            val element = json.parseToJsonElement(text).jsonObject
            val type = element["type"]?.jsonPrimitive?.contentOrNull ?: return null
            when (type) {
                "presence" -> json.decodeFromJsonElement(Presence.serializer(), element)
                "state" -> json.decodeFromJsonElement(State.serializer(), element)
                "command" -> json.decodeFromJsonElement(Command.serializer(), element)
                "ack" -> json.decodeFromJsonElement(Ack.serializer(), element)
                "transfer" -> json.decodeFromJsonElement(Transfer.serializer(), element)
                "leave" -> json.decodeFromJsonElement(Leave.serializer(), element)
                else -> null
            }
        }.getOrNull()

        fun encode(msg: ConnectMsg): String = when (msg) {
            is Presence -> buildString { append("{\"type\":\"presence\","); append(json.encodeToString(Presence.serializer(), msg).removePrefix("{")) }
            is State -> buildString { append("{\"type\":\"state\","); append(json.encodeToString(State.serializer(), msg).removePrefix("{")) }
            is Command -> buildString { append("{\"type\":\"command\","); append(json.encodeToString(Command.serializer(), msg).removePrefix("{")) }
            is Ack -> buildString { append("{\"type\":\"ack\","); append(json.encodeToString(Ack.serializer(), msg).removePrefix("{")) }
            is Transfer -> buildString { append("{\"type\":\"transfer\","); append(json.encodeToString(Transfer.serializer(), msg).removePrefix("{")) }
            is Leave -> buildString { append("{\"type\":\"leave\","); append(json.encodeToString(Leave.serializer(), msg).removePrefix("{")) }
        }
    }
}
