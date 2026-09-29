package io.rudione.chatone.data.remote.emote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PersonalSetExtractor(private val httpClient: HttpClient) {
    companion object {
        private const val TAG = "PersonalSetExtractor"

        private const val FLAG_PERSONAL = 1 shl 2
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchPersonalSetId(twitchUserId: String): String? {
        if (twitchUserId.isBlank()) return null
        return try {
            val raw = httpClient.get("https://7tv.io/v3/users/twitch/$twitchUserId").bodyAsText()
            val root = json.parseToJsonElement(raw).jsonObject

            val userObj = root["user"]?.jsonObject ?: root
            val emoteSets = userObj["emote_sets"]?.jsonArray ?: return null
            for (set in emoteSets) {
                val setObj = set.jsonObject
                val flags = setObj["flags"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
                if (flags and FLAG_PERSONAL != 0) {
                    val id = setObj["id"]?.jsonPrimitive?.contentOrNull
                    if (!id.isNullOrBlank()) return id
                }
            }

            val emoteSet = root["emote_set"]?.jsonObject
            val esFlags = emoteSet?.get("flags")?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
            if (esFlags and FLAG_PERSONAL != 0) {
                return emoteSet?.get("id")?.jsonPrimitive?.contentOrNull
            }
            null
        } catch (e: Exception) {
            Napier.w("PersonalSetExtractor failed for $twitchUserId: ${e.message}", tag = TAG)
            null
        }
    }
}
