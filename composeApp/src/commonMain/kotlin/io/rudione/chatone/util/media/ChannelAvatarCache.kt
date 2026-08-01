package io.rudione.chatone.util.media

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.util.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object ChannelAvatarCache {
    private val cache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    fun cached(login: String): String? = cache[login.lowercase()]

    suspend fun fetch(apiClient: TwitchApiClient, accessToken: String, login: String): String? {
        val key = login.lowercase()
        cache[key]?.let { return it }
        if (accessToken.isBlank() || login.isBlank()) return null
        return mutex.withLock {
            cache[key]?.let { return it }
            val result = apiClient.getUsers(accessToken, logins = listOf(login))
            val url = (result as? Result.Success)?.data?.data?.firstOrNull()?.profileImageUrl
            if (!url.isNullOrBlank()) cache[key] = url
            url
        }
    }
}
