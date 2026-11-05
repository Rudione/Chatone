package io.rudione.chatone.data.remote

import io.ktor.client.request.HttpRequestBuilder
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import io.rudione.chatone.domain.profile.StreamCategory
import io.rudione.chatone.domain.profile.StreamSettings
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class TwitchChannelSettingsClient(private val httpClient: HttpClient, private val clientId: String) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(token: String, broadcasterId: String): StreamSettings? {
        val root = get(token, CHANNELS) { parameter("broadcaster_id", broadcasterId) } ?: return null
        val item = root.arr("data").objects().firstOrNull() ?: return null
        val gameId = item.str("game_id").orEmpty()
        return StreamSettings(
            title = item.str("title").orEmpty(),
            category = gameId.takeIf { it.isNotBlank() }?.let { StreamCategory(it, item.str("game_name").orEmpty(), "") },
            tags = item.arr("tags").mapNotNull { (it as? JsonPrimitive)?.content },
            language = item.str("broadcaster_language").orEmpty()
        )
    }

    suspend fun searchCategories(token: String, query: String): List<StreamCategory> {
        val trimmed = query.trim().take(MAX_QUERY)
        if (trimmed.isEmpty()) return emptyList()
        val root = get(token, SEARCH) {
            parameter("query", trimmed)
            parameter("first", SEARCH_LIMIT)
        } ?: return emptyList()
        return root.arr("data").objects().mapNotNull { item ->
            val id = item.str("id")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            StreamCategory(
                id = id,
                name = item.str("name").orEmpty(),
                artUrl = item.str("box_art_url").orEmpty().replace("{width}", "52").replace("{height}", "72")
            )
        }
    }

    suspend fun update(token: String, broadcasterId: String, settings: StreamSettings): Boolean = try {
        val body = buildJsonObject {
            put("title", settings.title.take(MAX_TITLE))
            settings.category?.let { put("game_id", it.id) }
            if (settings.language.isNotBlank()) put("broadcaster_language", settings.language)
            putJsonArray("tags") { settings.tags.take(MAX_TAGS).forEach { add(it) } }
        }
        val response = httpClient.patch(CHANNELS) {
            header("Authorization", "Bearer $token")
            header("Client-Id", clientId)
            parameter("broadcaster_id", broadcasterId)
            setBody(TextContent(body.toString(), ContentType.Application.Json))
        }
        response.status.isSuccess()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Channel update failed: ${e::class.simpleName}", tag = TAG)
        false
    }

    private suspend fun get(token: String, url: String, params: HttpRequestBuilder.() -> Unit): JsonObject? = try {
        val response = httpClient.get(url) {
            header("Authorization", "Bearer $token")
            header("Client-Id", clientId)
            params()
        }
        if (response.status.isSuccess()) json.parseToJsonElement(response.bodyAsText()) as? JsonObject else null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Channel settings request failed: ${e::class.simpleName}", tag = TAG)
        null
    }

    private companion object {
        const val TAG = "ChannelSettings"
        const val CHANNELS = "https://api.twitch.tv/helix/channels"
        const val SEARCH = "https://api.twitch.tv/helix/search/categories"
        const val SEARCH_LIMIT = 8
        const val MAX_QUERY = 60
        const val MAX_TITLE = 140
        const val MAX_TAGS = 10
    }
}
