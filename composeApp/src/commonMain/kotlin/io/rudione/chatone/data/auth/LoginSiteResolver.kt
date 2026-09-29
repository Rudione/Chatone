package io.rudione.chatone.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class LoginSiteResolver(
    private val httpClient: HttpClient,
    private val primaryUrl: String = AppConfig.SITE_LOGIN_URL,
    private val mirrorUrl: String = AppConfig.SITE_LOGIN_MIRROR_URL,
    private val probeTimeoutMs: Long = PROBE_TIMEOUT_MS
) {

    suspend fun resolve(): String {
        if (isReachable(primaryUrl)) return primaryUrl
        return if (isReachable(mirrorUrl)) mirrorUrl else primaryUrl
    }

    fun alternateOf(url: String): String? = when {
        url.startsWith(primaryUrl) -> mirrorUrl + url.removePrefix(primaryUrl)
        url.startsWith(mirrorUrl) -> primaryUrl + url.removePrefix(mirrorUrl)
        else -> null
    }

    private suspend fun isReachable(url: String): Boolean = try {
        withTimeoutOrNull(probeTimeoutMs) { httpClient.get(url).status.isSuccess() } == true
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 3_500L
    }
}
