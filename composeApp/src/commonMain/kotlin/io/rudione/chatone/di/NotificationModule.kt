package io.rudione.chatone.di

import io.rudione.chatone.data.remote.TwitchLiveStatusClient
import io.rudione.chatone.data.repository.LiveAlertsRepository
import io.rudione.chatone.data.repository.NotificationPreferencesRepository
import io.rudione.chatone.presentation.notifications.LiveAlertNotifier
import io.rudione.chatone.presentation.notifications.MentionAlertNotifier
import org.koin.dsl.module

val notificationModule = module {
    single { NotificationPreferencesRepository(settings = get()) }
    single { LiveAlertsRepository(settings = get()) }
    single { TwitchLiveStatusClient(httpClient = get()) }
    single {
        LiveAlertNotifier(
            repository = get(),
            liveStatusClient = get(),
            scope = get(IoScopeQualifier)
        )
    }
    single {
        MentionAlertNotifier(
            chatRepository = get(),
            authRepository = get(),
            accountManager = get(),
            muteRepository = get(),
            preferences = get(),
            scope = get(IoScopeQualifier)
        )
    }
}
