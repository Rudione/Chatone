package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.AutomationKind
import io.rudione.chatone.domain.model.ChatAutomation
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.normalizeAutomationChannel
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.util.media.NotificationSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock

internal class ChatAutomationController(
    private val scope: CoroutineScope,
    private val getChatState: () -> ChatState,
    private val getSettings: () -> SettingsState,
    private val sendAsUser: suspend (channelLogin: String, text: String, replyTo: ChatMessage?) -> Boolean,
    private val playSound: (SettingsState) -> Unit = {
        NotificationSoundPlayer.playMentionSound(it.mentionSoundVolume, it.customMentionSoundPath)
    },
    private val gate: AutomationGate = AutomationGate.shared,
    private val random: Random = Random.Default,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private var timerJob: Job? = null

    fun start() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(TIMER_TICK_MS)
                runDueTimers()
            }
        }
    }

    private suspend fun runDueTimers() {
        val s = getChatState()
        if (!canSpeak(s)) return
        val channel = normalizeAutomationChannel(s.channelLogin)
        getSettings().automations.forEach { auto ->
            if (!auto.enabled || auto.kind != AutomationKind.TIMED_MESSAGE) return@forEach
            if (auto.message.isBlank() || !auto.appliesTo(channel)) return@forEach
            val key = "timer|${auto.id}|$channel"
            val intervalMs = auto.effectiveIntervalMinutes * 60_000L
            val t = now()
            if (!gate.timerReady(key, channel, intervalMs + jitterMs(intervalMs), MIN_CHAT_LINES_FOR_TIMER, t)) {
                return@forEach
            }
            if (!gate.tryAcquireSend(channel, auto.message, t, minGapMs(s))) return@forEach
            gate.markTimerFired(key, channel, t + intervalMs + jitterMs(intervalMs))
            sendAsUser(s.channelLogin, auto.message, null)
        }
    }

    fun onIncomingMessage(message: ChatMessage, isOwnMessage: Boolean) {
        val s = getChatState()
        val channel = normalizeAutomationChannel(message.channelName)
        if (channel.isEmpty() || channel != normalizeAutomationChannel(s.channelLogin)) return
        if (isOwnMessage) {
            gate.recordSent(channel, message.message, now())
            return
        }
        gate.onChatLine(channel, message.id)
        val autos = getSettings().automations
        if (autos.isEmpty()) return
        autos.forEach { auto ->
            if (!auto.enabled || !auto.appliesTo(channel)) return@forEach
            when (auto.kind) {
                AutomationKind.AUTO_REPLY -> scheduleReply(auto, message, channel, s)
                AutomationKind.KEYWORD_SOUND -> playKeywordSound(auto, message, channel)
                AutomationKind.TIMED_MESSAGE -> Unit
            }
        }
    }

    private fun scheduleReply(auto: ChatAutomation, trigger: ChatMessage, channel: String, s: ChatState) {
        if (auto.message.isBlank()) return
        if (auto.keyword.isBlank() && !auto.onlyWhenMentioned) return
        if (auto.keyword.isNotBlank() && !matchesKeyword(trigger.message, auto.keyword)) return
        if (auto.onlyWhenMentioned && !mentionsUser(trigger.message, s)) return
        if (!canSpeak(s)) return
        val cooldownMs = auto.effectiveCooldownSeconds * 1000L
        if (!gate.tryReserveCooldown("reply|${auto.id}|$channel", cooldownMs, now())) return
        scope.launch {
            delay(typingDelayMs(auto.message))
            val slowWait = slowModeWaitMs(getChatState(), channel)
            if (slowWait > MAX_SLOW_MODE_WAIT_MS) return@launch
            if (slowWait > 0) delay(slowWait + random.nextLong(400L, 1_500L))
            val st = getChatState()
            if (normalizeAutomationChannel(st.channelLogin) != channel || !canSpeak(st)) return@launch
            if (!gate.tryAcquireSend(channel, auto.message, now(), minGapMs(st))) return@launch
            sendAsUser(st.channelLogin, auto.message, trigger)
        }
    }

    private fun playKeywordSound(auto: ChatAutomation, message: ChatMessage, channel: String) {
        if (auto.keyword.isBlank()) return
        if (!AutomationGate.stripInvisible(message.message).contains(auto.keyword.trim(), ignoreCase = true)) return
        val cooldownMs = auto.effectiveCooldownSeconds * 1000L
        if (!gate.tryReserveCooldown("sound|${auto.id}|$channel", cooldownMs, now())) return
        playSound(getSettings())
    }

    private fun canSpeak(s: ChatState): Boolean =
        s.channelLogin.isNotEmpty() && s.currentAccessToken.isNotEmpty() && s.currentUserId.isNotEmpty() &&
                s.isConnected && !s.isBanned

    private fun slowModeMs(s: ChatState): Long =
        if (s.isMod || s.isBroadcaster) 0L else s.roomState.slowMode.coerceAtLeast(0) * 1000L

    private fun minGapMs(s: ChatState): Long = maxOf(AutomationGate.MIN_GAP_MS, slowModeMs(s))

    private fun slowModeWaitMs(s: ChatState, channel: String): Long {
        val window = slowModeMs(s)
        if (window == 0L) return 0L
        return (gate.lastSentAt(channel) + window - now()).coerceAtLeast(0L)
    }

    private fun typingDelayMs(text: String): Long {
        val reading = random.nextLong(1_200L, 2_600L)
        val typing = text.length * random.nextLong(40L, 75L)
        return (reading + typing).coerceIn(MIN_REPLY_DELAY_MS, MAX_REPLY_DELAY_MS)
    }

    private fun jitterMs(intervalMs: Long): Long =
        random.nextLong(10_000L, 10_000L + (intervalMs / 5).coerceIn(1L, 120_000L))

    private fun mentionsUser(text: String, s: ChatState): Boolean =
        listOf(s.currentUserLogin, s.currentDisplayName)
            .filter { it.isNotBlank() }
            .any { matchesKeyword(text, "@$it") }

    companion object {
        const val TIMER_TICK_MS = 15_000L
        const val MIN_CHAT_LINES_FOR_TIMER = 3
        const val MIN_REPLY_DELAY_MS = 2_000L
        const val MAX_REPLY_DELAY_MS = 9_000L
        const val MAX_SLOW_MODE_WAIT_MS = 20_000L

        fun matchesKeyword(text: String, keyword: String): Boolean {
            val k = keyword.trim()
            if (k.isEmpty()) return false
            val clean = AutomationGate.stripInvisible(text)
            val checkBefore = isWordChar(k.first())
            val checkAfter = isWordChar(k.last())
            var from = 0
            while (from <= clean.length - k.length) {
                val i = clean.indexOf(k, from, ignoreCase = true)
                if (i < 0) return false
                val beforeOk = !checkBefore || i == 0 || !isWordChar(clean[i - 1])
                val end = i + k.length
                val afterOk = !checkAfter || end == clean.length || !isWordChar(clean[end])
                if (beforeOk && afterOk) return true
                from = i + 1
            }
            return false
        }

        private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
    }
}
