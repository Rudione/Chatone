package io.rudione.chatone.data.remote.proxy

import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.presentation.account.AccountManager
import kotlinx.coroutines.CoroutineScope


class IrcConnectionFactory(
    private val httpClientFactory: HttpClientFactory,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    private val clients = mutableMapOf<String, TwitchIrcClient>()

    fun forAccount(userId: String): TwitchIrcClient {
        clients[userId]?.let { return it }
        val httpClient = httpClientFactory.forAccount(userId)
        val client = TwitchIrcClient(httpClient = httpClient, scope = scope)
        clients[userId] = client
        return client
    }

    suspend fun disconnect(userId: String) {
        clients.remove(userId)?.disconnect()
    }

    suspend fun disconnectAll() {
        clients.values.toList().forEach { it.disconnect() }
        clients.clear()
    }

    fun activeAccountIrc(): TwitchIrcClient? {
        val activeId = accountManager.activeAccountId.value
        return clients[activeId]
    }
}
