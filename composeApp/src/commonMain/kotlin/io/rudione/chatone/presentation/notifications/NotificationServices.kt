package io.rudione.chatone.presentation.notifications

import org.koin.core.Koin

fun Koin.startNotificationServices() {
    get<LiveAlertNotifier>().start()
    get<MentionAlertNotifier>().start()
}
