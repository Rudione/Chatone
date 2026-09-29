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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

typealias BadgeImageSets = Map<String, Map<String, String>>

class TwitchBadgeGqlClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun globalBadges(): BadgeImageSets? =
        query(GLOBAL_QUERY, variables = null)?.let { data -> badgeSets(data["badges"] as? JsonArray) }

    suspend fun channelBadges(channelId: String): BadgeImageSets? {
        if (channelId.isEmpty() || channelId.any { !it.isDigit() }) return null
        val data = query(CHANNEL_QUERY, variables = channelId) ?: return null
        val user = data["user"] as? JsonObject ?: return emptyMap()
        return badgeSets(user["broadcastBadges"] as? JsonArray)
    }

    private suspend fun query(text: String, variables: String?): JsonObject? = try {
        val payload = buildJsonObject {
            put("query", text)
            if (variables != null) putJsonObject("variables") { put("id", variables) }
        }
        val response = httpClient.post(GQL_ENDPOINT) {
            header("Client-Id", WEB_CLIENT_ID)
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) {
            null
        } else {
            (json.parseToJsonElement(response.bodyAsText()) as? JsonObject)?.get("data") as? JsonObject
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Badge GQL request failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private fun badgeSets(badges: JsonArray?): BadgeImageSets {
        if (badges == null) return emptyMap()
        val result = HashMap<String, HashMap<String, String>>()
        for (element in badges) {
            val badge = element as? JsonObject ?: continue
            val setId = badge["setID"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: continue
            val version = badge["version"]?.jsonPrimitive?.contentOrNull?.lowercase() ?: continue
            val url = badge["imageURL"]?.jsonPrimitive?.contentOrNull ?: continue
            if (!url.startsWith(TRUSTED_IMAGE_PREFIX)) continue
            result.getOrPut(setId) { HashMap() }[version] = url
        }
        return result
    }

    private companion object {
        const val TAG = "TwitchBadgeGql"
        const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        const val WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        const val TRUSTED_IMAGE_PREFIX = "https://static-cdn.jtvnw.net/"
        const val GLOBAL_QUERY = "query { badges { setID version imageURL(size: DOUBLE) } }"
        const val CHANNEL_QUERY =
            "query(\$id: ID!) { user(id: \$id) { broadcastBadges { setID version imageURL(size: DOUBLE) } } }"
    }
}
