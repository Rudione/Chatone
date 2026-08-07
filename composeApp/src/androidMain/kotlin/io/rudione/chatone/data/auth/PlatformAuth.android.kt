package io.rudione.chatone.data.auth

import io.github.aakira.napier.Napier
import io.rudione.chatone.OAuthTokenHolder
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private const val AUTH_TIMEOUT_MS = 5 * 60 * 1000L

actual class PlatformAuthHandler actual constructor() {
    private var tokenDeferred = CompletableDeferred<String?>()
    private var expectedState: String = ""
    private var loopbackServer: LoopbackAuthServer? = null

    actual fun getClientId(): String =
        if (AppConfig.ANDROID_USE_LOOPBACK_REDIRECT) AppConfig.TWITCH_CLIENT_ID_DESKTOP
        else AppConfig.TWITCH_CLIENT_ID_MOBILE

    actual fun getRedirectUri(): String =
        if (AppConfig.ANDROID_USE_LOOPBACK_REDIRECT) AppConfig.REDIRECT_URI_DESKTOP
        else AppConfig.REDIRECT_URI_MOBILE

    actual fun newAuthSession(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        val state = bytes.joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
        expectedState = state
        return state
    }

    actual fun startAuth(authUrl: String) {
        tokenDeferred = CompletableDeferred()
        OAuthTokenHolder.reset()

        if (AppConfig.ANDROID_USE_LOOPBACK_REDIRECT) {
            val server = LoopbackAuthServer(AppConfig.OAUTH_CALLBACK_PORT) { token, state ->
                acceptToken(token, state, "loopback")
            }
            if (!server.start()) {
                Napier.e(
                    "Aborting auth: loopback port ${AppConfig.OAUTH_CALLBACK_PORT} is already in use",
                    tag = TAG
                )
                throw IllegalStateException(
                    "Port ${AppConfig.OAUTH_CALLBACK_PORT} is occupied by another app. " +
                            "Close it and try again."
                )
            }
            loopbackServer = server
        }

        OAuthTokenHolder.setCallback { token, state ->
            acceptToken(token, state, "deep link")
        }

        AuthTokenBridge.emitToken("__open_url__:$authUrl")
    }

    private fun acceptToken(token: String, state: String, source: String): Boolean {
        if (expectedState.isEmpty() || state != expectedState) {
            Napier.e("OAuth state mismatch from $source, rejecting token", tag = TAG)
            return false
        }
        Napier.d("OAuth token received via $source (${token.take(8)}...)", tag = TAG)
        tokenDeferred.complete(token)
        return true
    }

    actual suspend fun awaitToken(): String? {
        return try {
            withTimeoutOrNull(AUTH_TIMEOUT_MS) { tokenDeferred.await() }
        } catch (e: Exception) {
            Napier.e("Failed to await token: ${e.message}", e, tag = TAG)
            null
        }
    }

    actual fun cleanup() {
        loopbackServer?.stop()
        loopbackServer = null
        OAuthTokenHolder.reset()
    }

    private companion object {
        const val TAG = "PlatformAuth"
    }
}
