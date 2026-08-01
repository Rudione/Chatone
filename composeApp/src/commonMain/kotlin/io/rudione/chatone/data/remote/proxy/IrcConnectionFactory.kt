package io.rudione.chatone.data.remote.proxy

import io.rudione.chatone.data.remote.TwitchIrcClient
import io.rudione.chatone.presentation.account.AccountManager
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi

class IrcConnectionFactory(
    private val httpClientFactory: HttpClientFactory,
    private val accountManager: AccountManager,
    private val scope: CoroutineScope
) {
    private val clients = mutableMapOf<String, TwitchIrcClient>()

    @OptIn(InternalCoroutinesApi::class)
    private val lock = SynchronizedObject()

    @OptIn(InternalCoroutinesApi::class)
    fun forAccount(userId: String): TwitchIrcClient {
        synchronized(lock) { clients[userId] }?.let { return it }

        val httpClient = httpClientFactory.forAccount(userId)
        val client = TwitchIrcClient(httpClient = httpClient, scope = scope)
        return synchronized(lock) { clients.getOrPut(userId) { client } }
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun disconnect(userId: String) {
        synchronized(lock) { clients.remove(userId) }?.disconnect()
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun disconnectAll() {
        val all = synchronized(lock) {
            val snapshot = clients.values.toList()
            clients.clear()
            snapshot
        }
        all.forEach { it.disconnect() }
    }

    @OptIn(InternalCoroutinesApi::class)
    fun activeAccountIrc(): TwitchIrcClient? {
        val activeId = accountManager.activeAccountId.value
        return synchronized(lock) { clients[activeId] }
    }
}
