package io.rudione.chatone.util.media

import coil3.ImageLoader
import coil3.PlatformContext

expect fun createAnimatedImageLoader(context: PlatformContext): ImageLoader
