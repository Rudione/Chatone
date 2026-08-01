package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.model.AutomationKind
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.media.NotificationSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal class ChatAutomationController(
    private val scope: CoroutineScope,
    private val apiClient: TwitchApiClient,
    private val getChatState: () -> ChatState,
    private val getSettings: () -> SettingsState,
    private val sendViaHelixOnly: suspend (channelLogin: String, message: String, keepText: Boolean, state: ChatState) -> Unit
) {
    private val automationLastFired = mutableMapOf<String, Long>()
    private var timedMsgCounter = 0
    private var automationTimerJob: Job? = null
    private val invisibleDupMarker = "󠀀"

    fun start() {
        automationTimerJob?.cancel()
        automationTimerJob = scope.launch {
            while (currentCoroutineContext()[Job]?.isActive == true) {
                delay(15_000L)
                val s = getChatState()
                if (s.channelLogin.isEmpty() || s.currentAccessToken.isEmpty()) continue
                val autos = getSettings().automations
                if (autos.isEmpty()) continue
                val now = Clock.System.now().toEpochMilliseconds()
                autos.forEach { auto ->
                    if (!auto.enabled) return@forEach
                    if (auto.kind != AutomationKind.TIMED_MESSAGE) return@forEach
                    if (auto.channelLogin != "*" &&
                        !auto.channelLogin.equals(s.channelLogin, ignoreCase = true)
                    ) return@forEach
                    if (auto.message.isBlank()) return@forEach
                    val interval = auto.intervalMinutes.coerceAtLeast(1) * 60_000L
                    val last = automationLastFired[auto.id]
                    if (last == null) {
                        automationLastFired[auto.id] = now
                        return@forEach
                    }
                    val jitter = (3_000L..20_000L).random()
                    if (now - last >= interval + jitter) {
                        automationLastFired[auto.id] = now
                        val suffix = invisibleDupMarker.repeat(timedMsgCounter % 3)
                        timedMsgCounter++
                        sendViaHelixOnly(s.channelLogin, auto.message + suffix, true, s)
                    }
                }
            }
        }
    }

    private fun textMentionsCurrentUser(text: String): Boolean {
        val s = getChatState()
        val login = s.currentUserLogin
        val display = s.currentDisplayName
        return (login.isNotBlank() && text.contains("@$login", ignoreCase = true)) ||
                (display.isNotBlank() && text.contains("@$display", ignoreCase = true))
    }

    fun onIncomingMessage(message: ChatMessage, isOwnMessage: Boolean) {
        if (isOwnMessage) return
        val autos = getSettings().automations
        if (autos.isEmpty()) return
        val text = message.message
        val now = Clock.System.now().toEpochMilliseconds()
        autos.forEach { auto ->
            if (!auto.enabled) return@forEach
            if (auto.channelLogin != "*" && auto.channelLogin.isNotBlank() &&
                !auto.channelLogin.equals(message.channelName, ignoreCase = true)
            ) return@forEach
            when (auto.kind) {
                AutomationKind.AUTO_REPLY -> {
                    if (auto.message.isBlank()) return@forEach

                    if (auto.keyword.isBlank() && !auto.onlyWhenMentioned) return@forEach
                    if (auto.keyword.isNotBlank() && !text.contains(auto.keyword, ignoreCase = true)) return@forEach
                    if (auto.onlyWhenMentioned && !textMentionsCurrentUser(text)) return@forEach
                    val last = automationLastFired[auto.id] ?: 0L
                    if (now - last < auto.cooldownSeconds.coerceAtLeast(10) * 1000L) return@forEach
                    automationLastFired[auto.id] = now
                    scope.launch {
                        delay((800L..2500L).random())
                        val st = getChatState()
                        if (st.currentAccessToken.isEmpty() || st.channelId.isEmpty()) return@launch
                        apiClient.sendChatMessage(
                            accessToken = st.currentAccessToken,
                            broadcasterId = st.channelId,
                            senderId = st.currentUserId,
                            message = auto.message,
                            replyParentMessageId = message.id
                        )
                    }
                }

                AutomationKind.KEYWORD_SOUND -> {
                    if (auto.keyword.isBlank()) return@forEach
                    if (!text.contains(auto.keyword, ignoreCase = true)) return@forEach
                    val last = automationLastFired[auto.id] ?: 0L
                    if (now - last < auto.cooldownSeconds.coerceAtLeast(5) * 1000L) return@forEach
                    automationLastFired[auto.id] = now
                    val settings = getSettings()
                    NotificationSoundPlayer.playMentionSound(
                        settings.mentionSoundVolume,
                        settings.customMentionSoundPath
                    )
                }

                else -> {}
            }
        }
    }
}
