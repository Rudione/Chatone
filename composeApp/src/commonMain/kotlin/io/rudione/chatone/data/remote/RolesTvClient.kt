package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Instant

enum class ChannelRole(val path: String) {
    MODERATOR("moderators"),
    VIP("vips"),
    FOUNDER("founders"),
    ARTIST("artists")
}

data class ChannelRoleCounts(private val counts: Map<ChannelRole, Int>) {
    fun of(role: ChannelRole): Int = counts[role] ?: 0

    val held: List<ChannelRole> get() = ChannelRole.entries.filter { of(it) > 0 }

    companion object {
        val NONE = ChannelRoleCounts(emptyMap())
    }
}

data class RoleChannel(
    val id: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val followers: Int,
    val createdAtMs: Long?,
    val grantedAtMs: Long?,
    val isPartner: Boolean,
    val isAffiliate: Boolean
)

data class RoleChannelPage(
    val total: Int,
    val channels: List<RoleChannel>,
    val nextCursor: String?
)

class RolesTvClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun roleCounts(userId: String, login: String): ChannelRoleCounts? {
        val key = userKey(userId, login) ?: return null
        val response = request("$BASE_URL/user/$key") ?: return null
        if (response.status == HttpStatusCode.NotFound) return ChannelRoleCounts.NONE
        if (!response.status.isSuccess()) return null
        val roles = (parse(response)?.get("data") as? JsonObject)?.get("roles") as? JsonObject
            ?: return ChannelRoleCounts.NONE
        return ChannelRoleCounts(ChannelRole.entries.associateWith { roles.int(it.path) ?: 0 })
    }

    suspend fun roleChannels(
        role: ChannelRole,
        userId: String,
        login: String,
        cursor: String?
    ): RoleChannelPage? {
        val key = userKey(userId, login) ?: return null
        val response = request("$BASE_URL/stats/user/${role.path}/$key") {
            parameter("per_page", PAGE_SIZE)
            if (!cursor.isNullOrBlank()) parameter("after", cursor)
        } ?: return null
        if (response.status == HttpStatusCode.NotFound) return RoleChannelPage(0, emptyList(), null)
        if (!response.status.isSuccess()) return null
        val root = parse(response) ?: return null
        val page = root.int("page") ?: 1
        val pages = root.int("pages") ?: 1
        val channels = (root["data"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::channelOf) }
        return RoleChannelPage(
            total = root.int("total") ?: channels.size,
            channels = channels,
            nextCursor = root.string("cursor")?.takeIf { it.isNotBlank() && page < pages }
        )
    }

    private suspend fun request(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse? = try {
        httpClient.get(url) {
            timeout { requestTimeoutMillis = TIMEOUT_MS }
            block()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("roles.tv request failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private suspend fun parse(response: HttpResponse): JsonObject? =
        runCatching { json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull()

    private fun channelOf(obj: JsonObject): RoleChannel? {
        val id = obj.string("id")?.takeIf { ID_PATTERN.matches(it) } ?: return null
        val login = obj.string("login")?.takeIf { LOGIN_PATTERN.matches(it.lowercase()) } ?: return null
        return RoleChannel(
            id = id,
            login = login,
            displayName = obj.string("displayName")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = obj.string("avatar")?.takeIf { it.startsWith("https://") },
            followers = obj.int("followers") ?: 0,
            createdAtMs = parseRolesTimestamp(obj.string("createdAt")),
            grantedAtMs = parseRolesTimestamp(obj.string("grantedAt")),
            isPartner = obj.boolean("isPartner"),
            isAffiliate = obj.boolean("isAffiliate")
        )
    }

    private fun userKey(userId: String, login: String): String? = when {
        ID_PATTERN.matches(userId) -> "id/$userId"
        LOGIN_PATTERN.matches(login.lowercase()) -> "login/${login.lowercase()}"
        else -> null
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    private fun JsonObject.boolean(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull == true

    companion object {
        private const val TAG = "RolesTv"
        private const val BASE_URL = "https://roles.tv/api"
        private const val TIMEOUT_MS = 15_000L
        private const val PAGE_SIZE = 50
        private val ID_PATTERN = Regex("^\\d{1,20}$")
        private val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")

        fun profileUrl(login: String): String = "https://roles.tv/u/${login.lowercase()}"
    }
}

internal fun parseRolesTimestamp(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    val iso = value.trim().replace(' ', 'T').let { if (it.endsWith("Z") || it.contains('+')) it else "${it}Z" }
    return runCatching { Instant.parse(iso).toEpochMilliseconds() }.getOrNull()
}
