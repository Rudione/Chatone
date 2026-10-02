package io.rudione.chatone.data.auth

import io.rudione.chatone.util.security.AEAD_KEY_BYTES
import io.rudione.chatone.util.security.Base64Url
import io.rudione.chatone.util.security.PlatformAead
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

        return buildString {
            append(loginUrl.trimEnd('#'))
            append(if (loginUrl.contains('#')) '&' else '#')
            append("k=").append(key?.let(Base64Url::encode).orEmpty())
            if (key != null && returnTarget != null) append('&').append(returnTarget.fragmentParameter)
        }
    }

    fun decode(raw: String): LoginPayloadResult = LoginPayloadCodec.decode(raw, sessionKey)

    fun end() {
        sessionKey?.fill(0)
        sessionKey = null
    }
}
