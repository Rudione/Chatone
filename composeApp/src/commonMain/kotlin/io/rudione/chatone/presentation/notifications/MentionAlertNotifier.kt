package io.rudione.chatone.presentation.notifications

import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.MentionMuteRepository
import io.rudione.chatone.data.repository.NotificationPreferencesRepository
import io.rudione.chatone.domain.mention.MentionMatcher
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.system.NotificationTarget
import io.rudione.chatone.util.system.NotificationTopic
import io.rudione.chatone.util.system.isAppInForeground
import io.rudione.chatone.util.system.notifySystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class MentionAlertNotifier(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val accountManager: AccountManager,
    private val muteRepository: MentionMuteRepository,
    private val preferences: NotificationPreferencesRepository,
    private val scope: CoroutineScope
) {
    private val gatedBySetting = currentFormFactor() == DeviceFormFactor.PHONE
    private val identity = MutableStateFlow<TwitchAccount?>(null)
    private val recentIds = ArrayDeque<String>()
    @Volatile
    private var cachedRules: List<HighlightRule>? = null
    private var jobs: List<Job> = emptyList()

    fun start() {
        if (jobs.isNotEmpty()) return
        jobs = listOf(
            scope.launch { observeIdentity() },
            scope.launch { SettingsViewModel.changeBroadcast.collect { cachedRules = null } },
            scope.launch { chatRepository.messages.collect(::onMessage) }
        )
    }

    private suspend fun observeIdentity() {
        val accounts = flow { emitAll(authRepository.getAccounts()) }
        combine(accounts, accountManager.activeAccountId) { list, activeId ->
            list.firstOrNull { it.userId == activeId } ?: list.firstOrNull()
        }.collect { identity.value = it }
    }

    private fun onMessage(message: ChatMessage) {
        if (gatedBySetting && !preferences.mentionAlerts.value) return
        if (isAppInForeground()) return
        val account = identity.value ?: return
        if (message.userId == account.userId || message.id.isEmpty()) return
        MentionMatcher.match(
            text = message.message,
            login = account.login,
            displayName = account.displayName,
            replyParentLogin = message.replyParentUserLogin,
            replyParentDisplayName = message.replyParentDisplayName,
            rules = highlightRules()
        ) ?: return
        val channel = message.channelName.lowercase().removePrefix("#")
        if (muteRepository.isMuted(userLogin = message.username, channelLogin = channel)) return
        if (!markSeen(message.id)) return
        notifySystem(
            title = "${message.displayName.ifEmpty { message.username }} · #$channel",
            body = message.message.take(MAX_BODY_LENGTH),
            topic = NotificationTopic.MENTION,
            target = NotificationTarget.of(channel, message.id)
        )
    }

    private fun highlightRules(): List<HighlightRule> =
        cachedRules ?: SettingsViewModel.loadInitialState().highlightRules.also { cachedRules = it }

    private fun markSeen(messageId: String): Boolean {
        if (messageId in recentIds) return false
        recentIds.addLast(messageId)
        if (recentIds.size > MAX_REMEMBERED_IDS) recentIds.removeFirst()
        return true
    }

    private companion object {
        const val MAX_BODY_LENGTH = 180
        const val MAX_REMEMBERED_IDS = 200
    }
}
