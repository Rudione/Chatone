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
import io.rudione.chatone.domain.model.LiveStreamSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.time.Instant

class TwitchLiveStatusClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun liveStreams(logins: Collection<String>): Map<String, LiveStreamSnapshot>? {
        val valid =
            logins.map { it.trim().lowercase() }.filter { LOGIN_PATTERN.matches(it) }.distinct()
        if (valid.isEmpty()) return emptyMap()
        val result = HashMap<String, LiveStreamSnapshot>()
        for (chunk in valid.chunked(MAX_LOGINS_PER_REQUEST)) {
            val users = query(chunk) ?: return null
            users.forEach { user -> snapshotOf(user)?.let { result[it.login] = it } }
        }
        return result
    }

    private suspend fun query(logins: List<String>): List<JsonObject>? = try {
        val payload = buildJsonObject {
            put("query", USERS_QUERY)
            putJsonObject("variables") { putJsonArray("logins") { logins.forEach { add(it) } } }
        }
        val response = httpClient.post(GQL_ENDPOINT) {
            header("Client-Id", WEB_CLIENT_ID)
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) {
            null
        } else {
            val data =
                (json.parseToJsonElement(response.bodyAsText()) as? JsonObject)?.get("data") as? JsonObject
            (data?.get("users") as? JsonArray)?.mapNotNull { it as? JsonObject }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Live status request failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private fun snapshotOf(user: JsonObject): LiveStreamSnapshot? {
        val stream = user["stream"] as? JsonObject ?: return null
        val login =
            user.string("login")?.lowercase()?.takeIf { LOGIN_PATTERN.matches(it) } ?: return null
        val streamId = stream.string("id")?.takeIf { it.isNotEmpty() } ?: return null
        val startedAtMs = stream.string("createdAt")
            ?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
            ?: return null
        return LiveStreamSnapshot(
            login = login,
            displayName = user.string("displayName")?.takeIf { it.isNotBlank() } ?: login,
            streamId = streamId,
            startedAtMs = startedAtMs,
            title = stream.string("title").orEmpty().take(MAX_TEXT_LENGTH),
            gameName = (stream["game"] as? JsonObject)?.string("displayName").orEmpty()
                .take(MAX_TEXT_LENGTH)
        )
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private companion object {
        const val TAG = "TwitchLiveStatus"
        const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        const val WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        const val MAX_LOGINS_PER_REQUEST = 100
        const val MAX_TEXT_LENGTH = 140
        const val USERS_QUERY =
            "query(\$logins: [String!]) { users(logins: \$logins) { login displayName " +
                    "stream { id createdAt title game { displayName } } } }"
        val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
    }
}
