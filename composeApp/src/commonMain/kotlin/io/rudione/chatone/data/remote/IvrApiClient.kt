package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class SubAgeInfo(
    val cumulativeMonths: Int,
    val streakMonths: Int,
    val tier: String?,
    val hidden: Boolean
)

/** ivr.fi public API — sub-age lookup needs no Twitch token at all. */
class IvrApiClient(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun getSubAge(userLogin: String, channelLogin: String): SubAgeInfo? {
        return try {
            val raw = httpClient
                .get("https://api.ivr.fi/v2/twitch/subage/$userLogin/$channelLogin")
                .bodyAsText()
            val root = json.parseToJsonElement(raw).jsonObject
            val hidden = root["statusHidden"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            val cumulative = root["cumulative"]?.let { el ->
                (el as? kotlinx.serialization.json.JsonObject
                    ?: el.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject)
                    ?.get("months")?.jsonPrimitive?.intOrNull
            } ?: 0
            val streak = root["streak"]?.let { el ->
                (el as? kotlinx.serialization.json.JsonObject)?.get("months")?.jsonPrimitive?.intOrNull
            } ?: 0
            val tier = root["meta"]?.let { el ->
                (el as? kotlinx.serialization.json.JsonObject)?.get("tier")?.jsonPrimitive?.contentOrNull
            }
            SubAgeInfo(cumulativeMonths = cumulative, streakMonths = streak, tier = tier, hidden = hidden)
        } catch (e: Exception) {
            Napier.w("IVR subage failed: ${e.message}", tag = "IvrApi")
            null
        }
    }
}
