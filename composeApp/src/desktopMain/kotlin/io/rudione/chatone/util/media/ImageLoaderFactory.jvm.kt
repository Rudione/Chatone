package io.rudione.chatone.util.media

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy
import coil3.svg.SvgDecoder

actual fun createAnimatedImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .components { add(SvgDecoder.Factory()) }
        .build()
}
