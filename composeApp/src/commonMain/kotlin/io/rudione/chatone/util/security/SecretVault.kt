package io.rudione.chatone.util.security

import com.russhwolf.settings.Settings

expect object PlatformSecretCipher {
    val backendName: String
    val isOsProtected: Boolean
    fun seal(plain: String): String?
    fun open(payload: String): String?
}

object SecretVault {

    const val PREFIX = "cv1:"

    val backendName: String get() = PlatformSecretCipher.backendName

    val isOsProtected: Boolean get() = PlatformSecretCipher.isOsProtected

    fun isSealed(value: String): Boolean = value.startsWith(PREFIX)

    fun seal(plain: String): String {
        if (plain.isEmpty() || isSealed(plain)) return plain
        return PlatformSecretCipher.seal(plain)?.let { PREFIX + it } ?: plain
    }

    fun open(stored: String): String {
        if (!isSealed(stored)) return stored
        return PlatformSecretCipher.open(stored.removePrefix(PREFIX)).orEmpty()
    }
}

fun Settings.putSecret(key: String, value: String) {
    if (value.isEmpty()) {
        remove(key)
        return
    }
    putString(key, SecretVault.seal(value))
}

fun Settings.getSecret(key: String): String {
    val stored = getStringOrNull(key) ?: return ""
    if (stored.isEmpty()) return ""
    if (SecretVault.isSealed(stored)) return SecretVault.open(stored)
    val resealed = SecretVault.seal(stored)
    if (resealed != stored) putString(key, resealed)
    return stored
}
