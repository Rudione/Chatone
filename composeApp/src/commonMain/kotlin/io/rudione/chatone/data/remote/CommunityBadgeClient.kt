package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readRemaining
import io.rudione.chatone.util.link.OutboundUrlPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock

class CommunityBadgeClient(
    private val httpClient: HttpClient,
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private class Directory(val name: String, val url: String) {
        val lock = Mutex()
        var badges: Map<String, List<AccountBadge>> = emptyMap()
        var nextUpdateMs = 0L
        var etag: String? = null
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val directories = listOf(
        Directory("Chatterino", "https://api.chatterino.com/badges"),
        Directory("Homies", "https://chatterinohomies.com/api/badges/list")
    )
    private val publishLock = Mutex()
    private val published = mutableMapOf<String, Map<String, List<AccountBadge>>>()
    private val _badgesByUserId = MutableStateFlow<Map<String, List<AccountBadge>>>(emptyMap())
    val badgesByUserId: StateFlow<Map<String, List<AccountBadge>>> = _badgesByUserId

    suspend fun badgesFor(userId: String): List<AccountBadge> {
        if (!USER_ID.matches(userId)) return emptyList()
        loadAll()
        return _badgesByUserId.value[userId].orEmpty()
    }

    suspend fun loadAll(): Unit = coroutineScope {
        directories.map { directory -> async { load(directory) } }.forEach { it.await() }
    }

    private suspend fun load(directory: Directory): Map<String, List<AccountBadge>> = directory.lock.withLock {
        if (nowMs() < directory.nextUpdateMs) return@withLock directory.badges
        try {
            val response = httpClient.get(directory.url) {
                header(HttpHeaders.Accept, "application/json")
                directory.etag?.let { header(HttpHeaders.IfNoneMatch, it) }
                timeout { requestTimeoutMillis = 5_000L }
            }
            if (response.status != HttpStatusCode.NotModified) {
                check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
                val bytes = response.bodyAsChannel().readRemaining(MAX_BYTES + 1).readByteArray()
                check(bytes.size <= MAX_BYTES) { "Badge directory too large" }
                directory.badges = withContext(Dispatchers.Default) { parseDirectory(bytes.decodeToString(), directory.name) }
                publishLock.withLock {
                    published[directory.name] = directory.badges
                    _badgesByUserId.value = withContext(Dispatchers.Default) {
                        val merged = mutableMapOf<String, MutableList<AccountBadge>>()
                        published.values.forEach { users -> users.forEach { (id, badges) -> merged.getOrPut(id) { mutableListOf() }.addAll(badges) } }
                        merged.mapValues { (_, badges) -> badges.distinctBy { it.imageUrl } }
                    }
                }
                directory.etag = response.headers[HttpHeaders.ETag]
            }
            directory.nextUpdateMs = nowMs() + CACHE_MS
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            directory.nextUpdateMs = nowMs() + RETRY_MS
            Napier.w("${directory.name} badge directory unavailable: ${e::class.simpleName}", tag = "CommunityBadges")
        }
        directory.badges
    }

    internal fun parseDirectory(raw: String, provider: String): Map<String, List<AccountBadge>> {
        val root = json.parseToJsonElement(raw)
        val entries = (if (root is JsonObject) root["badges"] else root) as? JsonArray
            ?: error("Invalid badge directory")
        check(entries.size <= MAX_ENTRIES) { "Too many badge entries" }
        val result = mutableMapOf<String, MutableList<AccountBadge>>()
        var users = 0
        entries.forEach { entry ->
            val badge = entry as? JsonObject ?: return@forEach
            val image = listOf("image2", "image1", "image3").firstNotNullOfOrNull { key ->
                badge.text(key)?.takeIf { it.startsWith("https://") && it.length <= 2048 && OutboundUrlPolicy.isFetchAllowed(it) }
            } ?: return@forEach
            val title = badge.text("tooltip")?.takeIf { it.isNotBlank() }?.take(256) ?: provider
            val visual = AccountBadge("${provider.lowercase()}:$image", if (title.startsWith(provider, true)) title else "$provider · $title", image)
            fun add(userId: String?) {
                if (userId == null || !USER_ID.matches(userId) || users >= MAX_USERS) return
                val values = result.getOrPut(userId) { mutableListOf() }
                if (values.size < 12 && values.none { it.key == visual.key }) { values.add(visual); users++ }
            }
            val members = badge["users"] as? JsonArray
            if (members != null) members.forEach { add((it as? JsonPrimitive)?.contentOrNull) }
            else add(badge.text("userId"))
        }
        return result
    }

    private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull

    companion object {
        private const val MAX_BYTES = 8 * 1024 * 1024L
        private const val MAX_ENTRIES = 50_000
        private const val MAX_USERS = 200_000
        private const val CACHE_MS = 6 * 60 * 60 * 1000L
        private const val RETRY_MS = 60_000L
        private val USER_ID = Regex("[0-9]{1,20}")
    }
}
