package io.rudione.chatone

import android.app.Application
import io.rudione.chatone.di.androidNotificationModule
import io.rudione.chatone.di.appModules
import io.rudione.chatone.di.startAndroidNotificationServices
import io.rudione.chatone.presentation.notifications.startNotificationServices
import io.rudione.chatone.util.system.AndroidNotifier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChatoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@ChatoneApplication)
            modules(appModules() + androidNotificationModule)
        }.koin
        AndroidNotifier.ensureChannels(this)
        koin.startNotificationServices()
        koin.startAndroidNotificationServices()
    }
}
