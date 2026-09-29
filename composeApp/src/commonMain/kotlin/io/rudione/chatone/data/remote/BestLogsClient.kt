package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Instant

data class LogMonth(val year: Int, val month: Int)

data class LogDay(val year: Int, val month: Int, val day: Int)

data class NameHistoryEntry(val login: String, val firstSeenMs: Long?, val lastSeenMs: Long?)

sealed interface LogMonthsResult {
    data class Available(val months: List<LogMonth>) : LogMonthsResult
    data object NotLogged : LogMonthsResult
    data object Failed : LogMonthsResult
}

data class ArchivedChatLine(
    val id: String,
    val text: String,
    val timestampMs: Long,
    val kind: Kind,
    val durationSeconds: Int? = null,
    val isDeleted: Boolean = false
) {
    enum class Kind { MESSAGE, TIMEOUT, BAN, NOTICE }
}

class BestLogsClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun availableMonths(channelId: String, userId: String): LogMonthsResult {
        if (!isValidId(channelId) || !isValidId(userId)) return LogMonthsResult.Failed
        return try {
            val response = httpClient.get("$BASE_URL/list") {
                parameter("channelid", channelId)
                parameter("userid", userId)
                timeout { requestTimeoutMillis = LIST_TIMEOUT_MS }
            }
            when {
                response.status == HttpStatusCode.NotFound -> LogMonthsResult.NotLogged
                !response.status.isSuccess() -> LogMonthsResult.Failed
                else -> {
                    val months = rootOf(response)?.get("availableLogs") as? JsonArray
                        ?: return LogMonthsResult.Failed
                    val parsed = months.mapNotNull { element ->
                        val obj = element as? JsonObject ?: return@mapNotNull null
                        val year = obj.string("year")?.toIntOrNull() ?: return@mapNotNull null
                        val month = obj.string("month")?.toIntOrNull() ?: return@mapNotNull null
                        LogMonth(year, month)
                    }.sortedWith(compareByDescending<LogMonth> { it.year }.thenByDescending { it.month })
                    if (parsed.isEmpty()) LogMonthsResult.NotLogged else LogMonthsResult.Available(parsed)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("BestLogs months failed: ${e.message}", tag = TAG)
            LogMonthsResult.Failed
        }
    }

    suspend fun nameHistory(userId: String): List<NameHistoryEntry>? {
        if (!isValidId(userId)) return null
        return try {
            val response = httpClient.get("$BASE_URL/namehistory/$userId") {
                timeout { requestTimeoutMillis = LIST_TIMEOUT_MS }
            }
            when {
                response.status == HttpStatusCode.NotFound -> emptyList()
                !response.status.isSuccess() -> null
                else -> (json.parseToJsonElement(response.bodyAsText()) as? JsonArray)
                    ?.mapNotNull { element ->
                        val obj = element as? JsonObject ?: return@mapNotNull null
                        val login = obj.string("user_login")?.lowercase()
                            ?.takeIf { LOGIN_PATTERN.matches(it) } ?: return@mapNotNull null
                        NameHistoryEntry(
                            login = login,
                            firstSeenMs = obj.string("first_timestamp")?.let(::parseInstantMs),
                            lastSeenMs = obj.string("last_timestamp")?.let(::parseInstantMs)
                        )
                    }
                    ?.sortedByDescending { it.lastSeenMs ?: 0L }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("BestLogs name history failed: ${e.message}", tag = TAG)
            null
        }
    }

    suspend fun channelDays(channelId: String): List<LogDay>? {
        if (!isValidId(channelId)) return null
        return try {
            val response = httpClient.get("$BASE_URL/list") {
                parameter("channelid", channelId)
                timeout { requestTimeoutMillis = LIST_TIMEOUT_MS }
            }
            when {
                response.status == HttpStatusCode.NotFound -> emptyList()
                !response.status.isSuccess() -> null
                else -> (rootOf(response)?.get("availableLogs") as? JsonArray)
                    ?.mapNotNull { element ->
                        val obj = element as? JsonObject ?: return@mapNotNull null
                        LogDay(
                            year = obj.string("year")?.toIntOrNull() ?: return@mapNotNull null,
                            month = obj.string("month")?.toIntOrNull()?.takeIf { it in 1..12 } ?: return@mapNotNull null,
                            day = obj.string("day")?.toIntOrNull()?.takeIf { it in 1..31 } ?: return@mapNotNull null
                        )
                    }
                    ?.sortedWith(compareByDescending<LogDay> { it.year }.thenByDescending { it.month }.thenByDescending { it.day })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("BestLogs channel days failed: ${e.message}", tag = TAG)
            null
        }
    }

    suspend fun channelDayRawLines(channelId: String, day: LogDay): List<String>? {
        if (!isValidId(channelId)) return null
        return try {
            httpClient.prepareGet("$BASE_URL/channelid/$channelId/${day.year}/${day.month}/${day.day}") {
                parameter("raw", "1")
                timeout { requestTimeoutMillis = LINES_TIMEOUT_MS }
            }.execute { response ->
                when {
                    response.status == HttpStatusCode.NotFound -> emptyList()
                    !response.status.isSuccess() -> null
                    else -> readRawLines(response.bodyAsChannel())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("BestLogs channel day failed: ${e.message}", tag = TAG)
            null
        }
    }

    suspend fun monthLines(channelId: String, userId: String, month: LogMonth): List<ArchivedChatLine>? {
        if (!isValidId(channelId) || !isValidId(userId)) return null
        return fetchLines("$BASE_URL/channelid/$channelId/userid/$userId/${month.year}/${month.month}")
    }

    suspend fun search(channelId: String, userId: String, query: String): List<ArchivedChatLine>? {
        if (!isValidId(channelId) || !isValidId(userId)) return null
        val trimmed = query.trim().take(MAX_QUERY_LENGTH)
        if (trimmed.isEmpty()) return emptyList()
        return fetchLines("$BASE_URL/channelid/$channelId/userid/$userId/search", trimmed)
    }

    suspend fun monthMessageTimestamps(channelId: String, userId: String, month: LogMonth): LongArray? {
        if (!isValidId(channelId) || !isValidId(userId)) return null
        return try {
            httpClient.prepareGet("$BASE_URL/channelid/$channelId/userid/$userId/${month.year}/${month.month}") {
                timeout { requestTimeoutMillis = LINES_TIMEOUT_MS }
            }.execute { response ->
                when {
                    response.status == HttpStatusCode.NotFound -> LongArray(0)
                    !response.status.isSuccess() -> null
                    else -> readMessageTimestamps(response.bodyAsChannel())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("BestLogs activity failed: ${e.message}", tag = TAG)
            null
        }
    }

    private suspend fun readRawLines(channel: ByteReadChannel): List<String> {
        val lines = ArrayList<String>()
        while (lines.size < MAX_RAW_LINES_PER_DAY) {
            val line = channel.readUTF8Line(MAX_LOG_LINE_LENGTH) ?: break
            if (line.isNotBlank()) lines += line
        }
        return lines
    }

    private suspend fun readMessageTimestamps(channel: ByteReadChannel): LongArray {
        var buffer = LongArray(INITIAL_TIMESTAMP_CAPACITY)
        var size = 0
        while (true) {
            val line = channel.readUTF8Line(MAX_LOG_LINE_LENGTH) ?: break
            val timestamp = parseLogMessageTimestamp(line) ?: continue
            if (size == buffer.size) buffer = buffer.copyOf(size * 2)
            buffer[size++] = timestamp
        }
        return buffer.copyOf(size)
    }

    private suspend fun fetchLines(url: String, query: String? = null): List<ArchivedChatLine>? = try {
        val response = httpClient.get(url) {
            parameter("jsonBasic", "1")
            if (query != null) parameter("q", query)
            timeout { requestTimeoutMillis = LINES_TIMEOUT_MS }
        }
        when {
            response.status == HttpStatusCode.NotFound -> emptyList()
            !response.status.isSuccess() -> null
            else -> (rootOf(response)?.get("messages") as? JsonArray)?.let(::toLines)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("BestLogs lines failed: ${e.message}", tag = TAG)
        null
    }

    private fun toLines(messages: JsonArray): List<ArchivedChatLine> {
        val rows = messages.mapNotNull { it as? JsonObject }
        val deletedIds = rows.mapNotNullTo(HashSet()) { row -> row.tags()?.string("target-msg-id") }
        return rows.mapNotNull { row ->
            val tags = row.tags() ?: JsonObject(emptyMap())
            if (tags.string("target-msg-id") != null) return@mapNotNull null
            val timestamp = row.string("timestamp")
                ?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
                ?: return@mapNotNull null
            val id = row.string("id")?.takeIf { it.isNotBlank() } ?: "bl_$timestamp"
            val duration = tags.string("ban-duration")?.toIntOrNull()
            val kind = when {
                tags.string("target-user-id") != null ->
                    if (duration != null) ArchivedChatLine.Kind.TIMEOUT else ArchivedChatLine.Kind.BAN
                tags.string("system-msg") != null -> ArchivedChatLine.Kind.NOTICE
                else -> ArchivedChatLine.Kind.MESSAGE
            }
            ArchivedChatLine(
                id = id,
                text = row.string("text").orEmpty(),
                timestampMs = timestamp,
                kind = kind,
                durationSeconds = duration,
                isDeleted = id in deletedIds
            )
        }.sortedBy { it.timestampMs }
    }

    private suspend fun rootOf(response: HttpResponse): JsonObject? =
        json.parseToJsonElement(response.bodyAsText()) as? JsonObject

    private fun JsonObject.tags(): JsonObject? = this["tags"] as? JsonObject

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun isValidId(value: String): Boolean = ID_PATTERN.matches(value)

    private fun parseInstantMs(value: String): Long? =
        runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()

    private companion object {
        const val TAG = "BestLogs"
        const val BASE_URL = "https://logs.zonian.dev"
        const val LIST_TIMEOUT_MS = 15_000L
        const val LINES_TIMEOUT_MS = 30_000L
        const val MAX_QUERY_LENGTH = 200
        const val INITIAL_TIMESTAMP_CAPACITY = 256
        const val MAX_LOG_LINE_LENGTH = 16_384
        const val MAX_RAW_LINES_PER_DAY = 20_000
        val ID_PATTERN = Regex("^\\d{1,20}$")
        val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
    }
}

internal fun parseLogMessageTimestamp(line: String): Long? {
    if (line.length < 26 || line[0] != '[' || line[20] != ']' || line[21] != ' ' || line[22] != '#') return null
    val channelEnd = line.indexOf(' ', 23)
    if (channelEnd < 0) return null
    val nameEnd = line.indexOf(": ", channelEnd + 1)
    if (nameEnd <= channelEnd + 1) return null
    val login = line.substring(channelEnd + 1, nameEnd)
    if (login.contains(' ')) return null
    val text = line.substring(nameEnd + 2)
    if (text.startsWith("$login has been timed out for ") || text.startsWith("$login has been banned") ||
        text.startsWith("$login has been permanently banned")
    ) return null
    val year = line.digitsAt(1, 4) ?: return null
    val month = line.digitsAt(6, 2)?.takeIf { it in 1..12 } ?: return null
    val day = line.digitsAt(9, 2)?.takeIf { it in 1..31 } ?: return null
    val hour = line.digitsAt(12, 2)?.takeIf { it in 0..23 } ?: return null
    val minute = line.digitsAt(15, 2)?.takeIf { it in 0..59 } ?: return null
    val second = line.digitsAt(18, 2)?.takeIf { it in 0..60 } ?: return null
    val days = epochDayOf(year, month, day)
    return (((days * 24 + hour) * 60 + minute) * 60 + second) * 1000
}

private fun String.digitsAt(start: Int, length: Int): Int? {
    var value = 0
    for (i in start until start + length) {
        val digit = this[i] - '0'
        if (digit !in 0..9) return null
        value = value * 10 + digit
    }
    return value
}

private fun epochDayOf(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yearOfEra = y - era * 400
    val shiftedMonth = (month + 9) % 12
    val dayOfYear = (153 * shiftedMonth + 2) / 5 + day - 1
    val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
    return era * 146_097L + dayOfEra - 719_468L
}
