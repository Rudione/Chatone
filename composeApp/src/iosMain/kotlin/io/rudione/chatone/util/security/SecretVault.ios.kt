package io.rudione.chatone.util.security

actual object PlatformSecretCipher {

    actual val backendName: String get() = "unavailable"

    actual val isOsProtected: Boolean get() = false

    actual fun seal(plain: String): String? = null

    actual fun open(payload: String): String? = null
}
