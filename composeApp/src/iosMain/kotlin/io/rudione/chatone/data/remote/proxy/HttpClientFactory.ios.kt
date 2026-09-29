package io.rudione.chatone.data.remote.proxy

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.util.link.OutboundUrlPolicy
import kotlinx.serialization.json.Json

private const val TAG = "HttpClientFactory"

actual fun buildHttpClientWithProxy(
    proxy: AccountProxyConfig?,
    blockPrivateNetworks: Boolean
): HttpClient {
    if (proxy?.enabled == true && proxy.isValid) {
        Napier.w("Proxy is configured but not applied: iOS transport does not support it yet", tag = TAG)
    }

    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(WebSockets)
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 30_000
        }
        if (blockPrivateNetworks) {
            install("PrivateNetworkGuard") {
                requestPipeline.intercept(HttpRequestPipeline.Before) {
                    val url = context.url.buildString()
                    if (!OutboundUrlPolicy.isFetchAllowed(url)) {
                        throw IllegalStateException("Blocked request to a non-public address")
                    }
                }
            }
        }
    }
}
