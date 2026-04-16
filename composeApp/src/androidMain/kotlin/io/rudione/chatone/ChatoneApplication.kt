package io.rudione.chatone

import android.app.Application
import io.rudione.chatone.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChatoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ChatoneApplication)
            modules(appModules())
        }
    }
}
