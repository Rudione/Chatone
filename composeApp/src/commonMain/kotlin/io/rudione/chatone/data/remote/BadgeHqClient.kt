package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.rudione.chatone.util.link.OutboundUrlPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock

data class AccountBadge(
    val key: String,
    val title: String,
    val imageUrl: String
)

class BadgeHqClient(private val httpClient: HttpClient) {

    private class CachedBadges(val badges: List<AccountBadge>, val fetchedAtMs: Long)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cache = LinkedHashMap<String, CachedBadges>()
    private val cacheLock = Mutex()

    suspend fun accountBadges(login: String): List<AccountBadge>? {
        val key = login.lowercase().takeIf { LOGIN_PATTERN.matches(it) } ?: return null
        cached(key)?.let { return it }
        val response = request("$BASE_URL/api/user/$key") ?: return null
        val badges = when {
            response.status == HttpStatusCode.NotFound -> emptyList()
            !response.status.isSuccess() -> return null
            else -> parse(response)?.let(::badgesOf) ?: return null
        }
        store(key, badges)
        return badges
    }

    private suspend fun request(url: String): HttpResponse? = try {
        httpClient.get(url) {
            header(HttpHeaders.Accept, "application/json")
            timeout { requestTimeoutMillis = TIMEOUT_MS }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("BadgeHQ request failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private suspend fun parse(response: HttpResponse): JsonObject? =
        runCatching { json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull()

    private fun badgesOf(root: JsonObject): List<AccountBadge> = buildList {
        addSection(root["twitchBadges"]) { obj ->
            val setId = obj.string("setId") ?: return@addSection null
            "tw_${setId}_${obj.string("version") ?: "1"}"
        }
        addSection((root["seventv"] as? JsonObject)?.get("badges")) { obj ->
            obj.string("id")?.let { "7tv_$it" }
        }
        THIRD_PARTY_SECTIONS.forEach { (section, prefix) ->
            addSection(root[section], prefix) { null }
        }
    }

    private fun MutableList<AccountBadge>.addSection(
        section: JsonElement?,
        titlePrefix: String = "",
        keyOf: (JsonObject) -> String?
    ) {
        (section as? JsonArray).orEmpty().take(MAX_PER_SECTION).forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            val imageUrl = imageUrl(obj.string("image")) ?: return@forEach
            val name = if (obj.boolean("founder")) "Founder"
            else obj.string("title")?.takeIf { it.isNotBlank() } ?: obj.string("name")?.takeIf { it.isNotBlank() }
            val title = name?.let { (titlePrefix + it).take(MAX_TITLE_LENGTH) } ?: return@forEach
            add(AccountBadge(key = keyOf(obj) ?: "3rd_$title", title = title, imageUrl = imageUrl))
        }
    }

    private fun imageUrl(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_URL_LENGTH } ?: return null
        val absolute = if (value.startsWith("/") && !value.startsWith("//")) BASE_URL + value else value
        return absolute.takeIf { it.startsWith("https://") && OutboundUrlPolicy.isFetchAllowed(it) }
    }

    private suspend fun cached(key: String): List<AccountBadge>? = cacheLock.withLock {
        cache[key]?.takeIf { nowMs() - it.fetchedAtMs < CACHE_LIFETIME_MS }?.badges
    }

    private suspend fun store(key: String, badges: List<AccountBadge>) = cacheLock.withLock {
        cache.remove(key)
        cache[key] = CachedBadges(badges, nowMs())
        while (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
    }

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.boolean(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull == true

    companion object {
        private const val TAG = "BadgeHQ"
        private const val BASE_URL = "https://www.badgehq.xyz"
        private const val TIMEOUT_MS = 15_000L
        private const val CACHE_LIFETIME_MS = 5 * 60 * 1000L
        private const val CACHE_SIZE = 24
        private const val MAX_PER_SECTION = 500
        private const val MAX_TITLE_LENGTH = 256
        private const val MAX_URL_LENGTH = 2048
        private val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
        private val THIRD_PARTY_SECTIONS = listOf(
            "ffzBadges" to "",
            "bttvBadges" to "",
            "badgehqBadges" to "BadgeHQ ",
            "chatterinoBadges" to "",
            "homiesBadges" to "",
            "moltorinoBadges" to ""
        )
    }
}
