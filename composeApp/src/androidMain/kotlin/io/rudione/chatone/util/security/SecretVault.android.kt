package io.rudione.chatone.util.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.aakira.napier.Napier
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val TAG = "SecretVault"
private const val PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "chatone_secret_key_v1"
private const val IV_BYTES = 12
private const val GCM_TAG_BITS = 128

actual object PlatformSecretCipher {

    private val key: SecretKey? by lazy { resolveKey() }

    actual val backendName: String
        get() = if (key != null) "Android Keystore (AES/GCM)" else "unavailable"

    actual val isOsProtected: Boolean get() = key != null

    actual fun seal(plain: String): String? {
        val secret = key ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secret)
            val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(cipher.iv + body, Base64.NO_WRAP)
        } catch (e: Exception) {
            Napier.e("seal failed: ${e.message}", tag = TAG)
            null
        }
    }

    actual fun open(payload: String): String? {
        val secret = key ?: return null
        return try {
            val raw = Base64.decode(payload, Base64.NO_WRAP)
            if (raw.size <= IV_BYTES) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                secret,
                GCMParameterSpec(GCM_TAG_BITS, raw.copyOfRange(0, IV_BYTES))
            )
            String(cipher.doFinal(raw.copyOfRange(IV_BYTES, raw.size)), Charsets.UTF_8)
        } catch (e: Exception) {
            Napier.w("open failed: ${e.message}", tag = TAG)
            null
        }
    }

    private fun resolveKey(): SecretKey? = try {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: generateKey()
    } catch (e: Exception) {
        Napier.e("keystore unavailable: ${e.message}", tag = TAG)
        null
    }

    private fun generateKey(): SecretKey? = try {
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    } catch (e: Exception) {
        Napier.e("key generation failed: ${e.message}", tag = TAG)
        null
    }
}
