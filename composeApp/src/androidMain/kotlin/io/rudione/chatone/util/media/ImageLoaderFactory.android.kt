package io.rudione.chatone.util.media

import android.os.Build
import coil3.decode.Decoder
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.svg.SvgDecoder

internal actual fun platformImageDecoders(): List<Decoder.Factory> = listOf(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AnimatedImageDecoder.Factory() else GifDecoder.Factory(),
    SvgDecoder.Factory()
)
