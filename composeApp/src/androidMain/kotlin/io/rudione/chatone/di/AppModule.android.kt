package io.rudione.chatone.di

import io.rudione.chatone.data.local.DatabaseDriverFactory
import io.rudione.chatone.data.local.createDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { createDatabase(get()) }
}
