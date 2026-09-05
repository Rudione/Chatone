package io.rudione.chatone.util.media

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.decode.Decoder
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import io.rudione.chatone.data.remote.proxy.buildHttpClientWithProxy
import io.rudione.chatone.domain.model.AccountProxyConfig

internal expect fun platformImageDecoders(): List<Decoder.Factory>

fun createAnimatedImageLoader(
    context: PlatformContext,
    proxy: AccountProxyConfig? = null
): ImageLoader = ImageLoader.Builder(context)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .components {
        platformImageDecoders().forEach { add(it) }
        add(KtorNetworkFetcherFactory(httpClient = { buildHttpClientWithProxy(proxy) }))
    }
    .build()
