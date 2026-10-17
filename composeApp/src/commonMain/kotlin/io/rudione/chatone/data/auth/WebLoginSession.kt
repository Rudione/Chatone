package io.rudione.chatone.data.auth

import io.rudione.chatone.util.security.AEAD_KEY_BYTES
import io.rudione.chatone.util.security.Base64Url
import io.rudione.chatone.util.security.PlatformAead
import io.rudione.chatone.util.security.randomHex
import io.rudione.chatone.util.settings.AppConfig

class WebLoginSession {

    private var sessionKey: ByteArray? = null

    fun begin(loginUrl: String = AppConfig.SITE_LOGIN_URL, returnTarget: LoginReturnTarget? = null): String {
        val key = if (PlatformAead.isSupported) {
            PlatformAead.secureRandomBytes(AEAD_KEY_BYTES)
        } else {
            null
        }
        sessionKey?.fill(0)
        sessionKey = key

        val base = loginUrl.substringBefore('#')
        val fragment = loginUrl.substringAfter('#', missingDelimiterValue = "")
        return buildString {
            append(base)
            append(if (base.contains('?')) '&' else '?')
            append("s=").append(randomHex(SESSION_TAG_BYTES))
            append('#')
            if (fragment.isNotEmpty()) append(fragment).append('&')
            append("k=").append(key?.let(Base64Url::encode).orEmpty())
            if (key != null && returnTarget != null) append('&').append(returnTarget.fragmentParameter)
        }
    }

    fun decode(raw: String): LoginPayloadResult = LoginPayloadCodec.decode(raw, sessionKey)

    fun end() {
        sessionKey?.fill(0)
        sessionKey = null
    }

    private companion object {
        const val SESSION_TAG_BYTES = 6
    }
}
