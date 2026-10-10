package io.rudione.chatone.di

import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.presentation.stream.AndroidStreamPlaybackEngineFactory
import io.rudione.chatone.presentation.stream.StreamMediaSessions
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(UnstableApi::class)
actual val platformStreamModule: Module = module {
    single { StreamMediaSessions(androidContext()) }
    single<StreamPlaybackEngineFactory> {
        AndroidStreamPlaybackEngineFactory(
            context = androidContext(),
            accountManager = get(),
            adBreakSource = get(),
            sessions = get()
        )
    }
}
