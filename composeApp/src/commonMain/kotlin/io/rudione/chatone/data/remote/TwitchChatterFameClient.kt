package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class ChatterFame(
    val userId: String,
    val login: String,
    val displayName: String,
    val avatarUrl: String?,
    val isPartner: Boolean,
    val followers: Int,
    val chatColor: String? = null
) {
    val isNotable: Boolean get() = isPartner || followers >= NOTABLE_FOLLOWERS

    companion object {
        const val NOTABLE_FOLLOWERS = 50_000
    }
}

class TwitchChatterFameClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val cache = LinkedHashMap<String, ChatterFame?>()
    private val loginCache = LinkedHashMap<String, ChatterFame?>()

    suspend fun lookup(userIds: Collection<String>): Map<String, ChatterFame>? =
        resolve(userIds.filter { ID_PATTERN.matches(it) }.distinct(), cache, USERS_BY_ID_QUERY, "ids") { it.userId }

    suspend fun lookupLogins(logins: Collection<String>): Map<String, ChatterFame>? =
        resolve(
            logins.map { it.lowercase() }.filter { LOGIN_PATTERN.matches(it) }.distinct(),
            loginCache,
            USERS_BY_LOGIN_QUERY,
            "logins"
        ) { it.login.lowercase() }

    private suspend fun resolve(
        wanted: List<String>,
        store: LinkedHashMap<String, ChatterFame?>,
        document: String,
        variable: String,
        keyOf: (ChatterFame) -> String
    ): Map<String, ChatterFame>? {
        val missing = mutex.withLock { wanted.filterNot(store::containsKey) }
        for (chunk in missing.chunked(MAX_KEYS_PER_REQUEST)) {
            val found = query(document, variable, chunk)?.associateBy(keyOf) ?: return null
            mutex.withLock {
                chunk.forEach { key -> store[key] = found[key] }
                while (store.size > CACHE_LIMIT) store.remove(store.keys.first())
            }
        }
        return mutex.withLock { wanted.mapNotNull { key -> store[key]?.let { key to it } }.toMap() }
    }

    private suspend fun query(document: String, variable: String, keys: List<String>): List<ChatterFame>? = try {
        val payload = buildJsonObject {
            put("query", document)
            putJsonObject("variables") { putJsonArray(variable) { keys.forEach { add(it) } } }
        }
        val response = httpClient.post(GQL_ENDPOINT) {
            header("Client-Id", TwitchFirstPartyClient.WEB.clientId)
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) {
            null
        } else {
            val users = ((json.parseToJsonElement(response.bodyAsText()) as? JsonObject)
                ?.get("data") as? JsonObject)?.get("users") as? JsonArray
            users?.mapNotNull { (it as? JsonObject)?.let(::fameOf) }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Chatter lookup failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private fun fameOf(user: JsonObject): ChatterFame? {
        val id = user.string("id")?.takeIf { ID_PATTERN.matches(it) } ?: return null
        val login = user.string("login").orEmpty()
        return ChatterFame(
            userId = id,
            login = login,
            displayName = user.string("displayName")?.takeIf { it.isNotBlank() } ?: login,
            avatarUrl = user.string("profileImageURL")?.takeIf { it.startsWith("https://") },
            isPartner = ((user["roles"] as? JsonObject)?.get("isPartner") as? JsonPrimitive)?.contentOrNull == "true",
            followers = ((user["followers"] as? JsonObject)?.get("totalCount") as? JsonPrimitive)?.intOrNull ?: 0,
            chatColor = user.string("chatColor")?.takeIf { COLOR_PATTERN.matches(it) }
        )
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

    private companion object {
        const val TAG = "ChatterFame"
        const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        const val MAX_KEYS_PER_REQUEST = 100
        const val CACHE_LIMIT = 20_000
        val ID_PATTERN = Regex("^\\d{1,20}$")
        val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
        val COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
        const val USER_FIELDS =
            "id login displayName chatColor profileImageURL(width: 50) roles { isPartner } followers { totalCount }"
        const val USERS_BY_ID_QUERY =
            "query ChatoneChatterFame(\$ids: [ID!]) { users(ids: \$ids) { $USER_FIELDS } }"
        const val USERS_BY_LOGIN_QUERY =
            "query ChatoneChatterFameByLogin(\$logins: [String!]) { users(logins: \$logins) { $USER_FIELDS } }"
    }
}
