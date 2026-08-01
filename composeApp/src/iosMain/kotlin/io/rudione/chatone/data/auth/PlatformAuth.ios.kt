package io.rudione.chatone.data.auth

import io.github.aakira.napier.Napier
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.CompletableDeferred
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class PlatformAuthHandler actual constructor() {
    private val tokenDeferred = CompletableDeferred<String?>()

    actual fun getClientId(): String = AppConfig.TWITCH_CLIENT_ID_MOBILE

    actual fun getRedirectUri(): String = AppConfig.REDIRECT_URI_MOBILE

    actual fun newAuthSession(): String =
        (0 until 32).joinToString("") { kotlin.random.Random.nextInt(16).toString(16) }

    actual fun startAuth(authUrl: String) {
        val url = NSURL.URLWithString(authUrl)
        if (url != null) {
            UIApplication.sharedApplication.openURL(url)
        }
    }

    actual suspend fun awaitToken(): String? {
        return try {
            tokenDeferred.await()
        } catch (e: Exception) {
            Napier.e("Failed to await token: ${e.message}", e, tag = "PlatformAuth")
            null
        }
    }

    fun onTokenReceived(token: String) {
        tokenDeferred.complete(token)
    }

    actual fun cleanup() {

    }
}
