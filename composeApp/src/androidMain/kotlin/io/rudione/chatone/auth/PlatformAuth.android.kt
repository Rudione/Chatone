package io.rudione.chatone.auth

import android.content.Intent
import android.net.Uri
import io.github.aakira.napier.Napier
import io.rudione.chatone.OAuthTokenHolder
import io.rudione.chatone.util.AppConfig
import kotlinx.coroutines.CompletableDeferred

actual class PlatformAuthHandler actual constructor() {
    private val tokenDeferred = CompletableDeferred<String?>()

    actual fun getClientId(): String = AppConfig.TWITCH_CLIENT_ID_MOBILE

    actual fun getRedirectUri(): String = AppConfig.REDIRECT_URI_MOBILE

    actual fun startAuth(authUrl: String) {
        OAuthTokenHolder.setCallback { token ->
            Napier.d("OAuth token received via deep link", tag = "PlatformAuth")
            tokenDeferred.complete(token)
        }
       
        AuthTokenBridge.emitToken("__open_url__:$authUrl")
    }

    actual suspend fun awaitToken(): String? {
        return try {
            tokenDeferred.await()
        } catch (e: Exception) {
            Napier.e("Failed to await token: ${e.message}", e, tag = "PlatformAuth")
            null
        }
    }

    actual fun cleanup() {
       
    }
}
