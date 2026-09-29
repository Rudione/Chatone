package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class FirstPartyTokenResolver(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val resolved = LinkedHashMap<String, TwitchFirstPartyClient>()

    suspend fun clientFor(token: String): TwitchFirstPartyClient {
        if (token.isBlank()) return TwitchFirstPartyClient.WEB
        return mutex.withLock {
            resolved[token] ?: lookup(token)?.also { remember(token, it) } ?: TwitchFirstPartyClient.WEB
        }
    }

    private fun remember(token: String, client: TwitchFirstPartyClient) {
        if (resolved.size >= MAX_ENTRIES) resolved.remove(resolved.keys.first())
        resolved[token] = client
    }

    private suspend fun lookup(token: String): TwitchFirstPartyClient? = try {
        withTimeoutOrNull(LOOKUP_TIMEOUT_MS) {
            val response = httpClient.get(VALIDATE_ENDPOINT) { header("Authorization", "OAuth $token") }
            when {
                response.status.isSuccess() -> {
                    val body = json.parseToJsonElement(response.bodyAsText()) as? JsonObject
                    val clientId = body?.get("client_id")?.jsonPrimitive?.contentOrNull
                    TwitchFirstPartyClient.fromClientId(clientId) ?: TwitchFirstPartyClient.WEB
                }

                response.status == HttpStatusCode.Unauthorized -> TwitchFirstPartyClient.WEB
                else -> null
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val VALIDATE_ENDPOINT = "https://id.twitch.tv/oauth2/validate"
        const val LOOKUP_TIMEOUT_MS = 6_000L
        const val MAX_ENTRIES = 16
    }
}
