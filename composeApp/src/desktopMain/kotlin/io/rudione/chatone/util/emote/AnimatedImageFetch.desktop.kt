package io.rudione.chatone.util.emote

import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.model.ProxyType
import io.rudione.chatone.util.concurrent.IoDispatcher
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.Base64

object AnimatedEmoteProxy {
    @Volatile
    private var provider: () -> AccountProxyConfig? = { null }

    @Volatile
    private var signature: String = ""

    fun setProvider(provider: () -> AccountProxyConfig?) {
        this.provider = provider
    }

    internal fun current(): AccountProxyConfig? {
        val proxy = runCatching { provider() }.getOrNull()?.takeIf { it.enabled && it.isValid }
        val next = proxy?.let { "${it.type}|${it.host}:${it.port}" }.orEmpty()
        if (next != signature) {
            signature = next
            AnimatedEmoteLoader.clear()
        }
        return proxy
    }
}

internal actual suspend fun fetchAnimatedImageBytes(url: String, accept: String, maxBytes: Int): ByteArray? =
    withContext(IoDispatcher) {
        val proxy = AnimatedEmoteProxy.current()
        val connection = URI(url).toURL().openConnection(proxy.toJavaProxy())
        proxy?.proxyAuthorizationHeader()?.let { connection.setRequestProperty("Proxy-Authorization", it) }
        connection.setRequestProperty("Accept", accept)
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.getInputStream().use { it.readNBytes(maxBytes) }
    }

private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

private fun AccountProxyConfig?.toJavaProxy(): Proxy {
    val config = this ?: return Proxy.NO_PROXY
    val type = when (config.type) {
        ProxyType.HTTP -> Proxy.Type.HTTP
        ProxyType.SOCKS5 -> Proxy.Type.SOCKS
    }
    return runCatching { Proxy(type, InetSocketAddress.createUnresolved(config.host, config.port)) }
        .getOrDefault(Proxy.NO_PROXY)
}

private fun AccountProxyConfig.proxyAuthorizationHeader(): String? {
    if (type != ProxyType.HTTP || !requiresAuth) return null
    val raw = "${username.orEmpty()}:${password.orEmpty()}"
    return "Basic " + Base64.getEncoder().encodeToString(raw.encodeToByteArray())
}
