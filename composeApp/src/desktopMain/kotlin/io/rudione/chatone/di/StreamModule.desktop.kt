package io.rudione.chatone.di

import io.rudione.chatone.domain.media.UnsupportedMediaPlaybackFactory
import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.domain.stream.UnsupportedStreamPlaybackEngineFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStreamModule: Module = module {
    single<StreamPlaybackEngineFactory> { UnsupportedStreamPlaybackEngineFactory }
    single<MediaPlaybackFactory> { UnsupportedMediaPlaybackFactory }
}
