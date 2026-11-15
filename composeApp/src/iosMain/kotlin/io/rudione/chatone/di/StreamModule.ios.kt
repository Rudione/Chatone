package io.rudione.chatone.di

import io.rudione.chatone.domain.media.MediaPlaybackFactory
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.presentation.media.IosMediaPlaybackFactory
import io.rudione.chatone.presentation.stream.IosPlaybackAudioSession
import io.rudione.chatone.presentation.stream.IosStreamPlaybackEngineFactory
import io.rudione.chatone.presentation.stream.lowlatency.LowLatencyPlayerProvider
import io.rudione.chatone.presentation.stream.lowlatency.StreamPlaylistGateway
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStreamModule: Module = module {
    single { IosPlaybackAudioSession() }
    single { StreamPlaylistGateway() }
    single<StreamPlaybackEngineFactory> {
        IosStreamPlaybackEngineFactory(
            adBreakSource = get(),
            audioSession = get(),
            lowLatencyPlayers = getOrNull<LowLatencyPlayerProvider>(),
            playlistGateway = get()
        )
    }
    single<MediaPlaybackFactory> { IosMediaPlaybackFactory(audioSession = get()) }
}

internal fun lowLatencyPlayerModule(provider: LowLatencyPlayerProvider): Module = module {
    single<LowLatencyPlayerProvider> { provider }
}
