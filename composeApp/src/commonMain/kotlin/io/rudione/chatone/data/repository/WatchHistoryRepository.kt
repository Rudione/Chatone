package io.rudione.chatone.data.repository

import kotlinx.coroutines.flow.collectLatest
import com.russhwolf.settings.Settings
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.domain.laboratory.MAX_WATCH_ROWS
import io.rudione.chatone.domain.laboratory.RETENTION_DAYS
import io.rudione.chatone.domain.laboratory.WatchDay
import io.rudione.chatone.domain.laboratory.recordWatchInterval
import io.rudione.chatone.domain.points.WatchActivitySink
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class WatchHistoryRepository(
    private val settings: Settings,
    private val database: ChatoneDatabase,
    scope: CoroutineScope
) : WatchActivitySink {
    private val queries = database.laboratoryQueries
    private val _records = MutableStateFlow<List<WatchDay>>(emptyList())
    val records = _records.asStateFlow()
    private val _enabled = MutableStateFlow(settings.getBoolean(KEY_ENABLED, true))
    val enabled = _enabled.asStateFlow()
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private val trackingActive = MutableStateFlow(false)
    private var activeChannel: String? = null
    private var since = TimeSource.Monotonic.markNow()
    private var wallStartedAt = Clock.System.now().toEpochMilliseconds()
    private var newSession = false
    private var previousChannel: String? = null
    private var stoppedAt = TimeSource.Monotonic.markNow()

    init {
        scope.launch {
            _records.value = load()
            for (command in commands) {
                checkpoint(command.mark)
                when (command) {
                    is Command.Playback -> {
                        if (activeChannel != command.channel) {
                            if (command.channel == null) stoppedAt = command.mark
                            else {
                                val idleMs = (command.mark - stoppedAt).inWholeMilliseconds
                                newSession = newSession || command.channel != previousChannel || idleMs >= SESSION_BREAK_MS
                                previousChannel = command.channel
                            }
                        }
                        activeChannel = command.channel
                    }
                    is Command.Enabled -> {
                        _enabled.value = command.value
                        settings.putBoolean(KEY_ENABLED, command.value)
                        newSession = activeChannel != null
                    }
                    is Command.Clear -> {
                        _records.value = emptyList()
                        queries.clearWatchDays()
                        previousChannel = null
                        newSession = activeChannel != null
                    }
                    is Command.Tick -> Unit
                }
                trackingActive.value = activeChannel != null && _enabled.value
                since = command.mark
                wallStartedAt = command.wallTime
            }
        }
        scope.launch {
            trackingActive.collectLatest { active ->
                if (active) while (isActive) {
                    delay(CHECKPOINT_MS)
                    commands.send(Command.Tick())
                }
            }
        }
    }

    override fun onPlayback(channelLogin: String?, playing: Boolean) {
        val login = channelLogin?.trim()?.removePrefix("#")?.lowercase()
            ?.takeIf { playing && it.isNotEmpty() }
        commands.trySend(Command.Playback(login))
    }

    fun setEnabled(value: Boolean) {
        commands.trySend(Command.Enabled(value))
    }

    fun clear() {
        commands.trySend(Command.Clear())
    }

    private fun checkpoint(mark: TimeSource.Monotonic.ValueTimeMark) {
        val channel = activeChannel ?: return
        if (!_enabled.value) return
        val elapsed = (mark - since).inWholeMilliseconds.coerceAtLeast(0L)
        if (elapsed == 0L) return
        val next = recordWatchInterval(_records.value, channel, wallStartedAt, elapsed, newSession, TimeZone.currentSystemDefault())
        newSession = false
        val previous = _records.value.associateBy { it.date to it.channel }
        database.transaction {
            next.filter { previous[it.date to it.channel] != it }.forEach { row ->
                queries.saveWatchDay(row.date, row.channel, row.durationMs, row.sessions.toLong(), row.lastWatchedAtMs)
            }
            val oldest = Instant.fromEpochMilliseconds(wallStartedAt + elapsed).toLocalDateTime(TimeZone.currentSystemDefault()).date
                .plus(-RETENTION_DAYS + 1, DateTimeUnit.DAY).toString()
            queries.pruneWatchDays(oldest)
            queries.limitWatchDays(MAX_WATCH_ROWS.toLong())
        }
        _records.value = next
    }

    private fun load(): List<WatchDay> {
        val oldest = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            .plus(-RETENTION_DAYS + 1, DateTimeUnit.DAY).toString()
        queries.pruneWatchDays(oldest)
        queries.limitWatchDays(MAX_WATCH_ROWS.toLong())
        return queries.selectWatchDays { date, channel, durationMs, sessions, lastWatchedAtMs ->
            WatchDay(date, channel, durationMs, sessions.toInt(), lastWatchedAtMs)
        }.executeAsList().filter { it.durationMs > 0L && it.sessions >= 0 }
    }

    private sealed class Command {
        val mark = TimeSource.Monotonic.markNow()
        val wallTime = Clock.System.now().toEpochMilliseconds()
        class Playback(val channel: String?) : Command()
        class Enabled(val value: Boolean) : Command()
        class Clear : Command()
        class Tick : Command()
    }

    private companion object {
        const val KEY_ENABLED = "laboratory_watch_enabled"
        const val CHECKPOINT_MS = 30_000L
        const val SESSION_BREAK_MS = 5 * 60_000L
    }
}
