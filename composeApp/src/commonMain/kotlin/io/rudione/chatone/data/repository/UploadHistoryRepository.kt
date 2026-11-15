package io.rudione.chatone.data.repository

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.domain.model.UploadHistoryEntry
import io.rudione.chatone.domain.model.UploadRetention
import io.rudione.chatone.util.security.SecretVault
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class UploadHistoryRepository(private val database: ChatoneDatabase, scope: CoroutineScope) {
    private val queries = database.laboratoryQueries
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<UploadHistoryEntry>>(emptyList())
    val entries = _entries.asStateFlow()

    init {
        scope.launch {
            mutex.withLock { _entries.value = load() }
            prune()
            entries.map { it.isNotEmpty() }.distinctUntilChanged().collectLatest { hasEntries ->
                if (hasEntries) while (isActive) {
                    delay(60_000L)
                    prune()
                }
            }
        }
    }

    suspend fun record(link: String, fileName: String, nowMs: Long = Clock.System.now().toEpochMilliseconds()) = mutex.withLock {
        val entry = UploadHistoryEntry(link, fileName, nowMs)
        database.transaction {
            queries.saveUpload("$nowMs-${Random.nextLong()}", SecretVault.seal(json.encodeToString(entry)), nowMs)
            queries.pruneUploads(nowMs - UploadRetention.HISTORY_MS, nowMs)
            queries.limitUploads(UploadRetention.MAX_ENTRIES.toLong())
        }
        _entries.value = UploadRetention.retained(listOf(entry) + _entries.value, nowMs)
    }

    suspend fun clear() = mutex.withLock {
        queries.clearUploads()
        _entries.value = emptyList()
    }

    private suspend fun prune() = mutex.withLock {
        val now = Clock.System.now().toEpochMilliseconds()
        queries.pruneUploads(now - UploadRetention.HISTORY_MS, now)
        _entries.value = UploadRetention.retained(_entries.value, now)
    }

    private fun load(): List<UploadHistoryEntry> = UploadRetention.retained(
        queries.selectUploads(UploadRetention.MAX_ENTRIES.toLong()).executeAsList().mapNotNull { row ->
            runCatching { json.decodeFromString<UploadHistoryEntry>(SecretVault.open(row.payload)) }.getOrNull()
        },
        Clock.System.now().toEpochMilliseconds()
    )
}
