package io.rudione.chatone.util.security

import io.github.aakira.napier.Napier
import kotlin.io.encoding.Base64

private const val TAG = "SecretVault"
private const val SERVICE = "io.rudione.chatone"
private const val ACCOUNT = "secret-master-key-v1"
private val AAD = "chatone-secret-v1".encodeToByteArray()

actual object PlatformSecretCipher {

    private val gcm: AesGcm? by lazy { resolveKey()?.let(::iosAesGcm) }

    actual val backendName: String
        get() = if (gcm != null) "iOS Keychain (AES/GCM)" else "unavailable"

    actual val isOsProtected: Boolean get() = gcm != null

    actual fun seal(plain: String): String? {
        val cipher = gcm ?: return null
        return runCatching {
            val nonce = PlatformAead.secureRandomBytes(AEAD_NONCE_BYTES)
            Base64.encode(nonce + cipher.seal(nonce, plain.encodeToByteArray(), AAD))
        }.onFailure { Napier.e("seal failed: ${it.message}", tag = TAG) }.getOrNull()
    }

    actual fun open(payload: String): String? {
        val cipher = gcm ?: return null
        return runCatching {
            val raw = Base64.decode(payload)
            if (raw.size <= AEAD_NONCE_BYTES) return null
            cipher.open(raw.copyOfRange(0, AEAD_NONCE_BYTES), raw.copyOfRange(AEAD_NONCE_BYTES, raw.size), AAD)
                ?.decodeToString()
        }.onFailure { Napier.w("open failed: ${it.message}", tag = TAG) }.getOrNull()
    }

    private fun resolveKey(): ByteArray? = when (val stored = IosKeychain.read(SERVICE, ACCOUNT)) {
        is KeychainRead.Found -> stored.bytes.takeIf { it.size == AEAD_KEY_BYTES }
        KeychainRead.Missing -> createKey()
        is KeychainRead.Failed -> {
            Napier.e("keychain read failed: ${stored.status}", tag = TAG)
            null
        }
    }

    private fun createKey(): ByteArray? {
        val key = PlatformAead.secureRandomBytes(AEAD_KEY_BYTES)
        return when (val status = IosKeychain.add(SERVICE, ACCOUNT, key)) {
            IosKeychain.successStatus -> key
            IosKeychain.duplicateStatus -> (IosKeychain.read(SERVICE, ACCOUNT) as? KeychainRead.Found)?.bytes
            else -> {
                Napier.e("keychain write failed: $status", tag = TAG)
                null
            }
        }
    }
}
