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
import kotlinx.serialization.json.Json

expect fun buildHttpClientWithProxy(proxy: AccountProxyConfig?): HttpClient


class HttpClientFactory(
    private val accountManager: AccountManager
) {
    companion object { private const val TAG = "HttpClientFactory" }

    private val clients = mutableMapOf<String, HttpClient>()
    private val proxyKeys = mutableMapOf<String, String>()

    fun forAccount(userId: String): HttpClient {
        val proxy = accountManager.getProxy(userId)?.takeIf { it.enabled && it.isValid }
        val key = proxy?.let { "${it.type}|${it.host}:${it.port}|${it.username ?: ""}" } ?: "default"
        val existingKey = proxyKeys[userId]
        if (existingKey == key) {
            clients[userId]?.let { return it }
        }
        clients[userId]?.let {
            try { it.close() } catch (e: Exception) { Napier.w("Failed to close client: ${e.message}", tag = TAG) }
        }
        val client = buildHttpClientWithProxy(proxy)
        clients[userId] = client
        proxyKeys[userId] = key
        Napier.d("Built HttpClient for $userId with proxy=${proxy != null}", tag = TAG)
        return client
    }

    fun release(userId: String) {
        clients.remove(userId)?.let {
            try { it.close() } catch (_: Exception) {}
        }
        proxyKeys.remove(userId)
    }

    fun releaseAll() {
        clients.values.forEach { try { it.close() } catch (_: Exception) {} }
        clients.clear()
        proxyKeys.clear()
    }
}
