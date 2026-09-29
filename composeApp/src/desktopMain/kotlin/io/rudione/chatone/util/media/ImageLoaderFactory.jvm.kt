package io.rudione.chatone.util.media

import coil3.decode.Decoder
import coil3.svg.SvgDecoder

internal actual fun platformImageDecoders(): List<Decoder.Factory> =
    listOf(SvgDecoder.Factory())
