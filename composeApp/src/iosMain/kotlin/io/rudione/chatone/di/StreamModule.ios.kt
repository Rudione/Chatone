package io.rudione.chatone.di

import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.presentation.media.IosMediaPlaybackFactory
import io.rudione.chatone.presentation.stream.IosPlaybackAudioSession
import io.rudione.chatone.presentation.stream.IosStreamPlaybackEngineFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStreamModule: Module = module {
    single { IosPlaybackAudioSession() }
    single<StreamPlaybackEngineFactory> { IosStreamPlaybackEngineFactory(adBreakSource = get(), audioSession = get()) }
    single<MediaPlaybackFactory> { IosMediaPlaybackFactory(audioSession = get()) }
}
