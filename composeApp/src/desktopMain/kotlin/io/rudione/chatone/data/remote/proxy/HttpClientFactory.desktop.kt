package io.rudione.chatone.data.remote.proxy

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import java.net.Proxy


actual fun buildHttpClientWithProxy(proxy: AccountProxyConfig?): HttpClient {

    val sysProxy: Proxy? = proxy
        ?.takeIf { it.enabled && it.isValid }
        ?.let { cfg ->
            try {
                val type = when (cfg.type) {
                    ProxyType.HTTP -> Proxy.Type.HTTP
                    ProxyType.SOCKS5 -> Proxy.Type.SOCKS
                }
                Proxy(type, InetSocketAddress(cfg.host, cfg.port))
            } catch (e: Exception) {
                Napier.w("Failed to build proxy: ${e.message}", tag = "HttpClientFactory")
                null
            }
        }

    if (sysProxy != null) {
        Napier.d("Proxy configured: ${sysProxy.address()} (active via JVM defaults if used)", tag = "HttpClientFactory")
    }

    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.v(message, tag = "HTTP")
                }
            }
            level = LogLevel.INFO
        }
        install(WebSockets)
    }
}
