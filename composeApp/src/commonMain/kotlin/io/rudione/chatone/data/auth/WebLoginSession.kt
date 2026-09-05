package io.rudione.chatone.data.auth

import io.rudione.chatone.util.security.AEAD_KEY_BYTES
import io.rudione.chatone.util.security.Base64Url
import io.rudione.chatone.util.security.PlatformAead
import io.rudione.chatone.util.settings.AppConfig

class WebLoginSession(private val loginUrl: String = AppConfig.SITE_LOGIN_URL) {

    private var sessionKey: ByteArray? = null

    fun begin(deviceUserCode: String? = null): String {
        val key = if (PlatformAead.isSupported) {
            PlatformAead.secureRandomBytes(AEAD_KEY_BYTES)
        } else {
            null
        }
        sessionKey = key

        return buildString {
            append(loginUrl.trimEnd('#'))
            append(if (loginUrl.contains('#')) '&' else '#')
            append("k=").append(key?.let(Base64Url::encode).orEmpty())
            if (!deviceUserCode.isNullOrBlank()) {
                append("&d=").append(sanitize(deviceUserCode))
            }
        }
    }

    fun decode(raw: String): LoginPayloadResult = LoginPayloadCodec.decode(raw, sessionKey)

    fun end() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    private fun sanitize(value: String): String =
        value.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
}
