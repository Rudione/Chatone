package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.dto.ChannelData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

class LiveStreamsBoard(
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    class Reading(val stream: ChannelData?, val checkedAtMs: Long)

    private val readings = MutableStateFlow<Map<String, Reading>>(emptyMap())

    fun publish(checkedLogins: Collection<String>, live: Collection<ChannelData>) {
        if (checkedLogins.isEmpty()) return
        val now = nowMs()
        val liveByLogin = live.associateBy { it.userLogin.lowercase() }
        readings.update { current ->
            val next = HashMap(current)
            checkedLogins.forEach { login ->
                val key = login.lowercase()
                next[key] = Reading(liveByLogin[key], now)
            }
            next
        }
    }

    fun fresh(login: String, maxAgeMs: Long): Reading? =
        readings.value[login.lowercase()]?.takeIf { nowMs() - it.checkedAtMs <= maxAgeMs }

    fun freshReadings(login: String, maxAgeMs: Long): Flow<Reading> {
        val key = login.lowercase()
        return readings
            .mapNotNull { it[key] }
            .distinctUntilChanged { old, new -> old === new }
            .filter { nowMs() - it.checkedAtMs <= maxAgeMs }
    }

    suspend fun awaitFresh(login: String, maxAgeMs: Long): Reading = freshReadings(login, maxAgeMs).first()
}
