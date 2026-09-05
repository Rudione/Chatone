package io.rudione.chatone.util.security

import io.github.aakira.napier.Napier
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "PlatformAead"

actual object PlatformAead {

    private val random = SecureRandom()

    actual val isSupported: Boolean = true

    actual fun secureRandomBytes(size: Int): ByteArray =
        ByteArray(size).also { random.nextBytes(it) }

    actual fun aesGcmOpen(
        key: ByteArray,
        nonce: ByteArray,
        sealedBox: ByteArray,
        aad: ByteArray
    ): ByteArray? = try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(AEAD_TAG_BYTES * 8, nonce)
        )
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        cipher.doFinal(sealedBox)
    } catch (e: Exception) {
        Napier.w("aesGcmOpen failed: ${e.message}", tag = TAG)
        null
    }
}
