package io.rudione.chatone.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.CachePolicy

actual fun createAnimatedImageLoader(context: PlatformContext): ImageLoader {
    return ImageLoader.Builder(context)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}
