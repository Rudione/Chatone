package io.rudione.chatone.data.remote.proxy

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.presentation.account.AccountManager
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.serialization.json.Json

expect fun buildHttpClientWithProxy(proxy: AccountProxyConfig?): HttpClient

class HttpClientFactory(
    private val accountManager: AccountManager
) {
    companion object { private const val TAG = "HttpClientFactory" }

    private val clients = mutableMapOf<String, HttpClient>()
    private val proxyKeys = mutableMapOf<String, String>()

    @OptIn(InternalCoroutinesApi::class)
    private val lock = SynchronizedObject()

    @OptIn(InternalCoroutinesApi::class)
    fun forAccount(userId: String): HttpClient {
        val proxy = accountManager.getProxy(userId)?.takeIf { it.enabled && it.isValid }
        val key = proxy?.let { "${it.type}|${it.host}:${it.port}|${it.username ?: ""}" } ?: "default"

        val stale = synchronized(lock) {
            if (proxyKeys[userId] == key) {
                clients[userId]?.let { return it }
            }
            clients.remove(userId)
        }
        stale?.let {
            try { it.close() } catch (e: Exception) { Napier.w("Failed to close client: ${e.message}", tag = TAG) }
        }

        val client = buildHttpClientWithProxy(proxy)
        val winner = synchronized(lock) {
            val raced = clients[userId]?.takeIf { proxyKeys[userId] == key }
            if (raced != null) {
                raced
            } else {
                clients[userId] = client
                proxyKeys[userId] = key
                client
            }
        }
        if (winner !== client) {
            try { client.close() } catch (_: Exception) {}
            return winner
        }

        Napier.d("Built HttpClient for $userId with proxy=${proxy != null}", tag = TAG)
        return client
    }

    @OptIn(InternalCoroutinesApi::class)
    fun release(userId: String) {
        val client = synchronized(lock) {
            proxyKeys.remove(userId)
            clients.remove(userId)
        }
        try { client?.close() } catch (_: Exception) {}
    }

    @OptIn(InternalCoroutinesApi::class)
    fun releaseAll() {
        val all = synchronized(lock) {
            val snapshot = clients.values.toList()
            clients.clear()
            proxyKeys.clear()
            snapshot
        }
        all.forEach { try { it.close() } catch (_: Exception) {} }
    }
}
