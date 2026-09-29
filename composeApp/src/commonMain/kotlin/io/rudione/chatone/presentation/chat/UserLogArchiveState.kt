package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.rudione.chatone.data.remote.ArchivedChatLine
import io.rudione.chatone.data.remote.BestLogsClient
import io.rudione.chatone.data.remote.LogMonth
import io.rudione.chatone.data.remote.LogMonthsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val MIN_LINES_ON_OPEN = 40
private const val MAX_AUTO_MONTHS_ON_OPEN = 3
private const val SEARCH_DEBOUNCE_MS = 450L

class UserLogArchiveState internal constructor(
    private val scope: CoroutineScope,
    private val client: BestLogsClient,
    private val channelId: String,
    private val userId: String
) {
    enum class Availability { Unknown, Loading, Available, NotLogged, Failed }

    var availability by mutableStateOf(Availability.Unknown)
        private set
    var months by mutableStateOf<List<LogMonth>>(emptyList())
        private set
    var isActive by mutableStateOf(false)
        private set
    var anchorMonth by mutableStateOf<LogMonth?>(null)
        private set
    var lines by mutableStateOf<List<ArchivedChatLine>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
        private set
    private var nextOlderIndex by mutableStateOf(0)

    private var monthsJob: Job? = null
    private var linesJob: Job? = null
    private var fallbackRequested = false
    private var openedBySearch = false

    val isSupported: Boolean get() = channelId.isNotBlank() && userId.isNotBlank()
    val isSearching: Boolean get() = query.isNotBlank()
    val canLoadOlder: Boolean get() = isActive && !isSearching && nextOlderIndex < months.size

    fun ensureMonths() {
        if (!isSupported) return
        if (availability == Availability.Available || availability == Availability.NotLogged) return
        if (monthsJob?.isActive == true) return
        availability = Availability.Loading
        monthsJob = scope.launch { fetchMonths() }
    }

    fun open(month: LogMonth?) {
        if (!isSupported) return
        openedBySearch = false
        isActive = true
        anchorMonth = month
        query = ""
        lines = emptyList()
        loadFailed = false
        linesJob?.cancel()
        linesJob = scope.launch {
            if (availability != Availability.Available) fetchMonths()
            if (availability != Availability.Available) return@launch
            nextOlderIndex = month?.let { months.indexOf(it) }?.takeIf { it >= 0 } ?: 0
            var loadedMonths = 0
            do {
                if (!appendOlderMonth()) break
                loadedMonths++
            } while (month == null && lines.size < MIN_LINES_ON_OPEN &&
                loadedMonths < MAX_AUTO_MONTHS_ON_OPEN && nextOlderIndex < months.size
            )
        }
    }

    fun openIfLogged() {
        if (!isSupported || isActive || fallbackRequested) return
        fallbackRequested = true
        scope.launch {
            if (availability != Availability.Available) fetchMonths()
            if (availability == Availability.Available && !isActive) open(null)
        }
    }

    fun loadOlder() {
        if (!canLoadOlder || isLoading) return
        linesJob = scope.launch { appendOlderMonth() }
    }

    fun updateQuery(value: String) {
        if (!isSupported) return
        if (!isActive) {
            if (value.isBlank()) return
            isActive = true
            anchorMonth = null
            openedBySearch = true
        }
        query = value
        linesJob?.cancel()
        if (value.isBlank()) {
            if (openedBySearch) close() else open(anchorMonth)
            return
        }
        linesJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            isLoading = true
            loadFailed = false
            val found = client.search(channelId, userId, value)
            if (query == value) {
                lines = found.orEmpty()
                loadFailed = found == null
            }
            isLoading = false
        }
    }

    fun close() {
        linesJob?.cancel()
        openedBySearch = false
        isActive = false
        isLoading = false
        loadFailed = false
        query = ""
        lines = emptyList()
    }

    private suspend fun fetchMonths() {
        availability = Availability.Loading
        availability = when (val result = client.availableMonths(channelId, userId)) {
            is LogMonthsResult.Available -> {
                months = result.months
                Availability.Available
            }
            LogMonthsResult.NotLogged -> Availability.NotLogged
            LogMonthsResult.Failed -> Availability.Failed
        }
    }

    private suspend fun appendOlderMonth(): Boolean {
        val month = months.getOrNull(nextOlderIndex) ?: return false
        isLoading = true
        loadFailed = false
        val fetched = client.monthLines(channelId, userId, month)
        isLoading = false
        if (fetched == null) {
            loadFailed = true
            return false
        }
        nextOlderIndex++
        if (fetched.isNotEmpty()) {
            val known = lines.mapTo(HashSet()) { it.id }
            lines = fetched.filterNot { it.id in known } + lines
        }
        return true
    }
}

@Composable
internal fun rememberUserLogArchive(channelId: String, userId: String): UserLogArchiveState {
    val scope = rememberCoroutineScope()
    val client: BestLogsClient = koinInject()
    return remember(channelId, userId) {
        UserLogArchiveState(scope, client, channelId, userId)
    }
}
