package io.rudione.chatone.di

import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import io.rudione.chatone.presentation.stream.AndroidStreamPlaybackEngineFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformStreamModule: Module = module {
    single<StreamPlaybackEngineFactory> {
        AndroidStreamPlaybackEngineFactory(
            context = androidContext(),
            accountManager = get(),
            adBreakSource = get()
        )
    }
}
