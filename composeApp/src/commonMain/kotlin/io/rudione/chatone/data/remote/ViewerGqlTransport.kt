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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

sealed interface GqlReply {
    data class Data(val data: JsonObject) : GqlReply
    data class Rejected(val message: String) : GqlReply
    data object Unreachable : GqlReply
}

class ViewerGqlTransport(
    private val httpClient: HttpClient,
    private val tokenResolver: FirstPartyTokenResolver = FirstPartyTokenResolver(httpClient)
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun anonymous(query: String, variables: JsonObject = EMPTY): GqlReply =
        send(TwitchFirstPartyClient.TV, token = null, query = query, variables = variables)

    suspend fun signedIn(token: String, query: String, variables: JsonObject = EMPTY): GqlReply {
        val clean = token.removePrefix("oauth:").trim()
        if (clean.isEmpty()) return GqlReply.Rejected("missing token")
        return send(tokenResolver.clientFor(clean), clean, query, variables)
    }

    private suspend fun send(
        client: TwitchFirstPartyClient,
        token: String?,
        query: String,
        variables: JsonObject
    ): GqlReply = try {
        val payload = buildJsonObject {
            put("query", query)
            put("variables", variables)
        }
        val response = httpClient.post(GQL_ENDPOINT) {
            header("Client-Id", client.clientId)
            header("User-Agent", client.userAgent)
            client.origin?.let { header("Origin", it) }
            client.referer?.let { header("Referer", it) }
            if (token != null) header("Authorization", "OAuth $token")
            setBody(TextContent(payload.toString(), ContentType.Application.Json))
        }
        if (!response.status.isSuccess()) {
            Napier.w("Viewer GQL HTTP ${response.status.value}", tag = TAG)
            GqlReply.Unreachable
        } else {
            parse(response.bodyAsText())
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.w("Viewer GQL failed: ${e::class.simpleName}", tag = TAG)
        GqlReply.Unreachable
    }

    private fun parse(body: String): GqlReply {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull()
            ?: return GqlReply.Unreachable
        val data = root["data"] as? JsonObject
        val error = (root["errors"] as? JsonArray)?.firstOrNull() as? JsonObject
        val message = (error?.get("message") as? JsonPrimitive)?.contentOrNull
        return when {
            data != null && data.values.any { it is JsonObject || it is JsonArray } -> GqlReply.Data(
                data
            )

            message != null -> GqlReply.Rejected(message)
            data != null -> GqlReply.Data(data)
            else -> GqlReply.Unreachable
        }
    }

    private companion object {
        const val TAG = "ViewerGql"
        const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        val EMPTY = JsonObject(emptyMap())
    }
}

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.arr(key: String): JsonArray =
    (this[key] as? JsonArray) ?: JsonArray(emptyList())

internal fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.int(key: String): Int? = str(key)?.toIntOrNull()

internal fun JsonObject.bool(key: String): Boolean = str(key) == "true"

internal fun JsonArray.objects(): List<JsonObject> = mapNotNull { it as? JsonObject }
