package io.rudione.chatone.presentation.notifications

import io.rudione.chatone.data.remote.TwitchLiveStatusClient
import io.rudione.chatone.data.repository.LiveAlertsRepository
import io.rudione.chatone.domain.live.LiveAlertPolicy
import io.rudione.chatone.domain.model.LiveStreamSnapshot
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.format
import io.rudione.chatone.util.system.NotificationTarget
import io.rudione.chatone.util.system.NotificationTopic
import io.rudione.chatone.util.system.notifySystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

class LiveAlertNotifier(
    private val repository: LiveAlertsRepository,
    private val liveStatusClient: TwitchLiveStatusClient,
    private val scope: CoroutineScope
) {
    private val mutex = Mutex()
    private var pollJob: Job? = null

    fun start() {
        if (pollJob != null) return
        pollJob = scope.launch {
            repository.channels.collectLatest { channels ->
                if (channels.isEmpty()) return@collectLatest
                while (isActive) {
                    checkNow()
                    delay(POLL_INTERVAL_MS.milliseconds)
                }
            }
        }
    }

    suspend fun checkNow(): Boolean = mutex.withLock {
        val enabled = repository.channels.value
        if (enabled.isEmpty()) return@withLock true
        val live = liveStatusClient.liveStreams(enabled) ?: return@withLock false
        val outcome = LiveAlertPolicy.evaluate(enabled, repository.ledger(), live)
        repository.saveLedger(outcome.entries)
        outcome.alerts.forEach(::announce)
        true
    }

    private fun announce(stream: LiveStreamSnapshot) {
        val strings = AppStrings.forLocale(SettingsViewModel.currentLanguage())
        val details = listOf(stream.title, stream.gameName).filter { it.isNotBlank() }.joinToString(" · ")
        notifySystem(
            title = strings.format(strings.liveNotifyTitle, stream.displayName),
            body = details.ifEmpty { strings.liveNotifyBody },
            topic = NotificationTopic.LIVE,
            target = NotificationTarget.of(stream.login)
        )
    }

    private companion object {
        const val POLL_INTERVAL_MS = 60_000L
    }
}
