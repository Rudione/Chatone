package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.normalizeAutomationChannel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class AutomationGate {

    private class Sent(val at: Long, val text: String, val automated: Boolean)

    private class TimerSlot(var nextDueAt: Long, var linesMark: Long)

    private val lock = SynchronizedObject()
    private val sent = mutableMapOf<String, ArrayDeque<Sent>>()
    private val cooldowns = mutableMapOf<String, Long>()
    private val timers = mutableMapOf<String, TimerSlot>()
    private val chatLines = mutableMapOf<String, Long>()
    private val seenLineIds = LinkedHashSet<String>()

    fun recordSent(channel: String, text: String, at: Long, automated: Boolean = false) = synchronized(lock) {
        val log = sent.getOrPut(normalizeAutomationChannel(channel)) { ArrayDeque() }
        log.addLast(Sent(at, normalizeOutgoing(text), automated))
        while (log.size > SENT_LOG_LIMIT) log.removeFirst()
    }

    fun lastSentAt(channel: String): Long = synchronized(lock) {
        sent[normalizeAutomationChannel(channel)]?.lastOrNull()?.at ?: 0L
    }

    fun onChatLine(channel: String, messageId: String) = synchronized(lock) {
        if (messageId.isNotEmpty()) {
            if (!seenLineIds.add(messageId)) return@synchronized
            if (seenLineIds.size > SEEN_LINES_LIMIT) seenLineIds.remove(seenLineIds.first())
        }
        val key = normalizeAutomationChannel(channel)
        chatLines[key] = (chatLines[key] ?: 0L) + 1
    }

    fun tryReserveCooldown(key: String, cooldownMs: Long, now: Long): Boolean = synchronized(lock) {
        val last = cooldowns[key]
        if (last != null && now - last < cooldownMs) return@synchronized false
        cooldowns[key] = now
        true
    }

    fun timerReady(key: String, channel: String, intervalMs: Long, minLines: Int, now: Long): Boolean =
        synchronized(lock) {
            val lines = chatLines[normalizeAutomationChannel(channel)] ?: 0L
            val slot = timers[key]
            if (slot == null) {
                timers[key] = TimerSlot(now + intervalMs, lines)
                return@synchronized false
            }
            now >= slot.nextDueAt && lines - slot.linesMark >= minLines
        }

    fun markTimerFired(key: String, channel: String, nextDueAt: Long) = synchronized(lock) {
        val lines = chatLines[normalizeAutomationChannel(channel)] ?: 0L
        timers[key] = TimerSlot(nextDueAt, lines)
    }

    fun tryAcquireSend(channel: String, text: String, now: Long, minGapMs: Long): Boolean = synchronized(lock) {
        val key = normalizeAutomationChannel(channel)
        val log = sent[key]
        val normalized = normalizeOutgoing(text)
        if (log != null) {
            val last = log.lastOrNull()
            if (last != null && now - last.at < minGapMs) return@synchronized false
            if (log.any { now - it.at < DUPLICATE_WINDOW_MS && it.text.equals(normalized, ignoreCase = true) }) {
                return@synchronized false
            }
            if (log.count { it.automated && now - it.at < RATE_WINDOW_MS } >= MAX_AUTOMATED_PER_WINDOW) {
                return@synchronized false
            }
        }
        val target = sent.getOrPut(key) { ArrayDeque() }
        target.addLast(Sent(now, normalized, automated = true))
        while (target.size > SENT_LOG_LIMIT) target.removeFirst()
        true
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 30_000L
        const val RATE_WINDOW_MS = 60_000L
        const val MAX_AUTOMATED_PER_WINDOW = 3
        const val MIN_GAP_MS = 2_500L
        private const val SENT_LOG_LIMIT = 40
        private const val SEEN_LINES_LIMIT = 512

        val shared = AutomationGate()

        private val whitespace = Regex("\\s+")

        private fun isInvisible(c: Char): Boolean =
            c == '­' || c == '͏' || c == '﻿' || c in '​'..'‏' || c in '⁠'..'⁤'

        fun stripInvisible(text: String): String {
            if (text.none { isInvisible(it) || it == '\uDB40' }) return text
            val out = StringBuilder(text.length)
            var i = 0
            while (i < text.length) {
                val c = text[i]
                if (c == '\uDB40' && i + 1 < text.length && text[i + 1] in '\uDC00'..'\uDC7F') {
                    i += 2
                    continue
                }
                if (!isInvisible(c)) out.append(c)
                i++
            }
            return out.toString()
        }

        fun normalizeOutgoing(text: String): String = stripInvisible(text).trim().replace(whitespace, " ")
    }
}
