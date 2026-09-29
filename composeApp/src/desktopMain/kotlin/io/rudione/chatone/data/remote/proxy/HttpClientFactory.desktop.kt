package io.rudione.chatone.data.remote.proxy

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import io.rudione.chatone.util.link.OutboundUrlPolicy
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

private const val TAG = "HttpClientFactory"

actual fun buildHttpClientWithProxy(
    proxy: AccountProxyConfig?,
    blockPrivateNetworks: Boolean
): HttpClient {

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
            if (blockPrivateNetworks) {
                addNetworkInterceptor(PrivateNetworkGuard)
            }
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
                                        .header(
                                            "Proxy-Authorization",
                                            Credentials.basic(user, password)
                                        )
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
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.v(message, tag = "HTTP")
                }
            }
            level = LogLevel.NONE
        }
        install(WebSockets)
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 30_000
        }
    }.also {
        Napier.d("Built desktop HttpClient (proxy=${javaProxy != null})", tag = TAG)
    }
}

private object PrivateNetworkGuard : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val route = chain.connection()?.route()
        if (route != null && route.proxy.type() == Proxy.Type.DIRECT) {
            val address = route.socketAddress.address?.address
            if (address == null || !OutboundUrlPolicy.isPublicResolvedAddress(address)) {
                throw IOException("Blocked request to a non-public address")
            }
        }
        return chain.proceed(chain.request())
    }
}
