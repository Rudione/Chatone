package io.rudione.chatone.presentation.chat.presence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.rudione.chatone.data.remote.BestLogsClient
import io.rudione.chatone.data.remote.LogMonth
import io.rudione.chatone.data.remote.LogMonthsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class UserPresenceState internal constructor(
    private val scope: CoroutineScope,
    private val client: BestLogsClient,
    private val channelId: String,
    private val userId: String
) {
    enum class Status { Idle, Loading, Ready, NotLogged, Failed }

    var isOpen by mutableStateOf(false)
        private set
    var status by mutableStateOf(Status.Idle)
        private set
    var presence by mutableStateOf(ChatPresence.Empty)
        private set
    var loadedMonths by mutableIntStateOf(0)
        private set
    var totalMonths by mutableIntStateOf(0)
        private set
    var partial by mutableStateOf(false)
        private set

    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    private var job: Job? = null

    val isSupported: Boolean get() = channelId.isNotBlank() && userId.isNotBlank()

    fun toggle() {
        isOpen = !isOpen
        if (isOpen) load()
    }

    fun retry() {
        if (status == Status.Failed || partial) {
            status = Status.Idle
            load()
        }
    }

    private fun load() {
        if (!isSupported || status == Status.Loading || status == Status.Ready) return
        cache[cacheKey]?.let { cached ->
            presence = cached
            status = Status.Ready
            return
        }
        job?.cancel()
        job = scope.launch { fetch() }
    }

    private suspend fun fetch() {
        status = Status.Loading
        partial = false
        loadedMonths = 0
        val months = when (val result = client.availableMonths(channelId, userId)) {
            is LogMonthsResult.Available -> result.months.filter { it.inWindowOf(today) }
            LogMonthsResult.NotLogged -> {
                status = Status.NotLogged
                return
            }
            LogMonthsResult.Failed -> {
                status = Status.Failed
                return
            }
        }
        totalMonths = months.size
        val accumulator = ChatPresenceAccumulator(TimeZone.currentSystemDefault(), today)
        var failures = 0
        months.forEach { month ->
            val timestamps = client.monthMessageTimestamps(channelId, userId, month)
            if (timestamps == null) {
                failures++
            } else {
                presence = withContext(Dispatchers.Default) {
                    accumulator.add(timestamps)
                    accumulator.snapshot()
                }
            }
            loadedMonths++
        }
        partial = failures > 0
        status = if (failures == months.size && months.isNotEmpty()) Status.Failed else Status.Ready
        if (status == Status.Ready && !partial) remember(presence)
    }

    private val cacheKey: String get() = "$channelId:$userId"

    private fun remember(value: ChatPresence) {
        if (cache.size >= CACHE_LIMIT) cache.remove(cache.keys.first())
        cache[cacheKey] = value
    }

    private fun LogMonth.inWindowOf(today: LocalDate): Boolean {
        val index = year * 12 + (month - 1)
        val current = today.year * 12 + (today.month.ordinal)
        return index in (current - WINDOW_MONTHS + 1)..current
    }

    private companion object {
        const val WINDOW_MONTHS = 13
        const val CACHE_LIMIT = 12
        val cache = LinkedHashMap<String, ChatPresence>()
    }
}
