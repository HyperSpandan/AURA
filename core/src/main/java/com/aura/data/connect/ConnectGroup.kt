package com.aura.data.connect

import java.nio.charset.StandardCharsets.UTF_8
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * A person's own devices sharing an end-to-end encrypted group over Nostr relays.
 * Matches ConnectGroup in the web player.
 */
class ConnectGroup private constructor(
    val secret: ByteArray,
    val tag: String,
    private val key: SecretKeySpec,
) {
    val encoded: String get() = Base64.getUrlEncoder().withoutPadding().encodeToString(secret)
    val appLink: String get() = "aura://connect/#$encoded"
    val webLink: String get() = "$WEB_PLAYER#/settings/devices?join=$encoded"

    fun seal(plain: String): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(tag.toByteArray(UTF_8))
        val ciphertext = cipher.doFinal(plain.toByteArray(UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    fun open(sealed: String): String? = runCatching {
        val cleaned = sealed.trim()
        val all = runCatching { Base64.getDecoder().decode(cleaned) }
            .recoverCatching { Base64.getUrlDecoder().decode(cleaned) }
            .getOrNull() ?: return null
        if (all.size < 12 + 16) return null
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, all, 0, 12))
        cipher.updateAAD(tag.toByteArray(UTF_8))
        String(cipher.doFinal(all, 12, all.size - 12), UTF_8)
    }.getOrNull()

    companion object {
        const val WEB_PLAYER = "https://hyperspandan.github.io/AURA/player/"
        private val ENCODED = Regex("^[A-Za-z0-9_-]{43}$")
        private val random = SecureRandom()

        fun of(secret: ByteArray): ConnectGroup {
            require(secret.size == 32) { "A group secret must be 32 bytes" }
            val tagBytes = hkdf(secret, "opentune/connect/tag", 16)
            val tagHex = tagBytes.joinToString("") { "%02x".format(it) }
            val keyBytes = hkdf(secret, "opentune/connect/key", 32)
            val keySpec = SecretKeySpec(keyBytes, "AES")
            return ConnectGroup(secret, tagHex, keySpec)
        }

        fun generate(): ConnectGroup {
            val secret = ByteArray(32).also(random::nextBytes)
            return of(secret)
        }

        fun parse(text: String?): String? {
            val value = text?.trim() ?: return null
            val raw = when {
                value.startsWith("aura://connect/#") -> value.removePrefix("aura://connect/#")
                value.contains("/settings/devices") || value.contains("join=") -> {
                    Regex("""[?&#](?:join|connect)=([A-Za-z0-9_-]{43})""").find(value)?.groupValues?.get(1).orEmpty()
                }
                value.contains("://") -> return null
                else -> value
            }
            return if (ENCODED.matches(raw)) raw else null
        }

        fun fromText(text: String?): ConnectGroup? {
            val raw = parse(text) ?: return null
            return runCatching {
                val pad = when (raw.length % 4) {
                    2 -> "=="
                    3 -> "="
                    else -> ""
                }
                val bytes = Base64.getUrlDecoder().decode(raw + pad)
                of(bytes)
            }.getOrNull()
        }

        fun fromSecret(secret: String): ConnectGroup =
            fromText(secret) ?: throw IllegalArgumentException("Invalid connect group secret: $secret")

        /** HKDF-SHA256 RFC 5869 with zero salt. */
        private fun hkdf(ikm: ByteArray, info: String, length: Int): ByteArray {
            val salt = ByteArray(32)
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(salt, "HmacSHA256"))
            val prk = mac.doFinal(ikm)

            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(info.toByteArray(UTF_8))
            mac.update(1.toByte())
            val okm = mac.doFinal()
            return okm.copyOf(length)
        }
    }
}
