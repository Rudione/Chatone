package io.rudione.chatone.di

import io.rudione.chatone.data.local.DatabaseDriverFactory
import io.rudione.chatone.data.local.createDatabase
import io.rudione.chatone.util.DesktopWallpaperLoader
import io.rudione.chatone.util.WallpaperLoader
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { DatabaseDriverFactory() }
    single { createDatabase(get()) }
    single<WallpaperLoader> { DesktopWallpaperLoader() }
}
