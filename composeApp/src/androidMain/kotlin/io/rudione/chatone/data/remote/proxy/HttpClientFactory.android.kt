package io.rudione.chatone.data.remote.proxy

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy

private const val TAG = "HttpClientFactory"

actual fun buildHttpClientWithProxy(proxy: AccountProxyConfig?): HttpClient {

    val activeProxy = proxy?.takeIf { it.enabled && it.isValid }

    val javaProxy: Proxy? = activeProxy?.let { cfg ->
        try {
            val type = when (cfg.type) {
                ProxyType.HTTP -> Proxy.Type.HTTP
                ProxyType.SOCKS5 -> Proxy.Type.SOCKS
            }
            Proxy(type, InetSocketAddress.createUnresolved(cfg.host, cfg.port))
        } catch (e: Exception) {
            Napier.w("Failed to build proxy: ${e.message}", tag = TAG)
            null
        }
    }

    if (activeProxy != null && javaProxy == null) {
        Napier.e("Proxy ${activeProxy.host}:${activeProxy.port} could not be applied", tag = TAG)
    }

    return HttpClient(OkHttp) {
        engine {
            if (javaProxy != null) {
                config { proxy(javaProxy) }

                if (activeProxy != null && activeProxy.requiresAuth) {
                    val user = activeProxy.username.orEmpty()
                    val password = activeProxy.password.orEmpty()
                    config {
                        proxyAuthenticator(
                            Authenticator { _: Route?, response ->
                                if (response.request.header("Proxy-Authorization") != null) {
                                    null
                                } else {
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", Credentials.basic(user, password))
                                        .build()
                                }
                            }
                        )
                    }
                }
            }
        }
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
    }
}
