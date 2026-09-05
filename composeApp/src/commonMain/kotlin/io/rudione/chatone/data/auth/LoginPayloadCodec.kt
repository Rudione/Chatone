package io.rudione.chatone.data.auth

import io.rudione.chatone.domain.model.LoginCredentials
import io.rudione.chatone.util.security.AEAD_NONCE_BYTES
import io.rudione.chatone.util.security.AEAD_TAG_BYTES
import io.rudione.chatone.util.security.Base64Url
import io.rudione.chatone.util.security.PlatformAead

sealed class LoginPayloadResult {
    data class Success(val credentials: LoginCredentials) : LoginPayloadResult()
    data object Empty : LoginPayloadResult()
    data object Malformed : LoginPayloadResult()
    data object DecryptionFailed : LoginPayloadResult()
    data object Incomplete : LoginPayloadResult()
    data object EncryptionUnsupported : LoginPayloadResult()
}

object LoginPayloadCodec {

    const val ENCRYPTED_PREFIX = "chatone1."

    private val AAD = "chatone-login-v1".encodeToByteArray()

    private const val MAX_PAYLOAD_CHARS = 8192
    private const val MAX_FIELD_CHARS = 512
    private const val MIN_TOKEN_CHARS = 20

    fun looksEncrypted(raw: String): Boolean = raw.trim().startsWith(ENCRYPTED_PREFIX)

    fun looksComplete(raw: String): Boolean {
        val payload = raw.trim()
        if (payload.isEmpty() || payload.length > MAX_PAYLOAD_CHARS) return false
        if (looksEncrypted(payload)) return sealedShapeIsValid(payload)
        val credentials = parseFields(payload) ?: return false
        return credentials.isComplete && credentials.oauthToken.length >= MIN_TOKEN_CHARS
    }

    fun decode(raw: String, key: ByteArray?): LoginPayloadResult {
        val payload = raw.trim()
        if (payload.isEmpty()) return LoginPayloadResult.Empty
        if (payload.length > MAX_PAYLOAD_CHARS) return LoginPayloadResult.Malformed

        val fields = if (looksEncrypted(payload)) {
            if (!PlatformAead.isSupported) return LoginPayloadResult.EncryptionUnsupported
            if (key == null || key.isEmpty()) return LoginPayloadResult.DecryptionFailed
            openSealed(payload, key) ?: return LoginPayloadResult.DecryptionFailed
        } else {
            payload
        }

        val credentials = parseFields(fields) ?: return LoginPayloadResult.Malformed
        if (!credentials.isComplete) return LoginPayloadResult.Incomplete
        return LoginPayloadResult.Success(credentials)
    }

    private fun sealedShapeIsValid(payload: String): Boolean {
        val parts = payload.removePrefix(ENCRYPTED_PREFIX).split('.')
        if (parts.size != 2) return false
        val nonce = Base64Url.decodeOrNull(parts[0]) ?: return false
        val sealedBox = Base64Url.decodeOrNull(parts[1]) ?: return false
        return nonce.size == AEAD_NONCE_BYTES && sealedBox.size > AEAD_TAG_BYTES
    }

    private fun openSealed(payload: String, key: ByteArray): String? {
        val parts = payload.removePrefix(ENCRYPTED_PREFIX).split('.')
        if (parts.size != 2) return null
        val nonce = Base64Url.decodeOrNull(parts[0]) ?: return null
        val sealedBox = Base64Url.decodeOrNull(parts[1]) ?: return null
        if (nonce.size != AEAD_NONCE_BYTES) return null
        if (sealedBox.size <= AEAD_TAG_BYTES) return null

        val opened = PlatformAead.aesGcmOpen(
            key = key,
            nonce = nonce,
            sealedBox = sealedBox,
            aad = AAD
        ) ?: return null
        return opened.decodeToString()
    }

    private fun parseFields(raw: String): LoginCredentials? {
        var username = ""
        var userId = ""
        var clientId = ""
        var oauthToken = ""
        var state = ""
        var recognised = 0

        raw.split(';').forEach { pair ->
            val separator = pair.indexOf('=')
            if (separator <= 0) return@forEach
            val key = pair.substring(0, separator).trim()
            val value = pair.substring(separator + 1).trim()
            if (value.length > MAX_FIELD_CHARS) return@forEach
            when (key) {
                "username" -> { username = value; recognised++ }
                "user_id" -> { userId = value; recognised++ }
                "client_id" -> { clientId = value; recognised++ }
                "oauth_token" -> { oauthToken = value.removePrefix("oauth:"); recognised++ }
                "state" -> { state = value; recognised++ }
            }
        }

        if (recognised == 0) return null
        return LoginCredentials(
            username = username,
            userId = userId,
            clientId = clientId,
            oauthToken = oauthToken,
            state = state
        )
    }

}
