package io.rudione.chatone.di

import io.rudione.chatone.presentation.notifications.ChatBackgroundController
import io.rudione.chatone.presentation.notifications.LiveAlertScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.dsl.module

val androidNotificationModule = module {
    single {
        ChatBackgroundController(
            context = androidContext(),
            preferences = get(),
            authRepository = get(),
            scope = get(IoScopeQualifier)
        )
    }
    single {
        LiveAlertScheduler(
            context = androidContext(),
            repository = get(),
            scope = get(IoScopeQualifier)
        )
    }
}

fun Koin.startAndroidNotificationServices() {
    get<ChatBackgroundController>().start()
    get<LiveAlertScheduler>().start()
}
