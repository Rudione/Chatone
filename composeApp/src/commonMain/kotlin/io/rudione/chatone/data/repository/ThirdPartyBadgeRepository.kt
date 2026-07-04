package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** A global FFZ or BTTV badge (bot/developer/supporter/subwoofer…) shown left of the
 * username, like moltorino/Chatterino do. */
data class ThirdPartyBadge(
    val title: String,
    val imageUrl: String,
    val colorHex: String?
)

/**
 * Global third-party badge sets. FFZ keys badge membership by LOGIN, BTTV by Twitch user ID —
 * both are fetched once per app run; lookups are O(1) map hits at message-render time.
 */
class ThirdPartyBadgeRepository(private val httpClient: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _ffzByLogin = MutableStateFlow<Map<String, List<ThirdPartyBadge>>>(emptyMap())
    val ffzByLogin: StateFlow<Map<String, List<ThirdPartyBadge>>> = _ffzByLogin

    private val _bttvByUserId = MutableStateFlow<Map<String, ThirdPartyBadge>>(emptyMap())
    val bttvByUserId: StateFlow<Map<String, ThirdPartyBadge>> = _bttvByUserId

    suspend fun loadAll() {
        loadFfz()
        loadBttv()
    }

    private suspend fun loadFfz() {
        try {
            val raw = httpClient.get("https://api.frankerfacez.com/v1/badges").bodyAsText()
            val root = json.parseToJsonElement(raw).jsonObject
            val badgesById = mutableMapOf<String, ThirdPartyBadge>()
            root["badges"]?.jsonArray?.forEach { el ->
                val o = el.jsonObject
                val id = o["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                // urls["2"] is the 2x png; fall back to base image
                val url = o["urls"]?.jsonObject?.get("2")?.jsonPrimitive?.contentOrNull
                    ?: o["image"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                badgesById[id] = ThirdPartyBadge(
                    title = o["title"]?.jsonPrimitive?.contentOrNull ?: "FFZ",
                    imageUrl = if (url.startsWith("//")) "https:$url" else url,
                    colorHex = o["color"]?.jsonPrimitive?.contentOrNull
                )
            }
            val byLogin = mutableMapOf<String, MutableList<ThirdPartyBadge>>()
            root["users"]?.jsonObject?.forEach { (badgeId, loginsEl) ->
                val badge = badgesById[badgeId] ?: return@forEach
                loginsEl.jsonArray.forEach { loginEl ->
                    val login = loginEl.jsonPrimitive.contentOrNull?.lowercase() ?: return@forEach
                    byLogin.getOrPut(login) { mutableListOf() }.add(badge)
                }
            }
            _ffzByLogin.value = byLogin
            Napier.d("FFZ badges loaded: ${badgesById.size} badges, ${byLogin.size} users", tag = TAG)
        } catch (e: Exception) {
            Napier.w("FFZ badges load failed: ${e.message}", tag = TAG)
        }
    }

    private suspend fun loadBttv() {
        try {
            val raw = httpClient.get("https://api.betterttv.net/3/cached/badges").bodyAsText()
            val arr = json.parseToJsonElement(raw).jsonArray
            val byUserId = mutableMapOf<String, ThirdPartyBadge>()
            arr.forEach { el ->
                val o = el.jsonObject
                val providerId = o["providerId"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val badge = o["badge"]?.jsonObject ?: return@forEach
                val svg = badge["svg"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                byUserId[providerId] = ThirdPartyBadge(
                    title = badge["description"]?.jsonPrimitive?.contentOrNull ?: "BTTV",
                    imageUrl = svg,
                    colorHex = null
                )
            }
            _bttvByUserId.value = byUserId
            Napier.d("BTTV badges loaded: ${byUserId.size} users", tag = TAG)
        } catch (e: Exception) {
            Napier.w("BTTV badges load failed: ${e.message}", tag = TAG)
        }
    }

    companion object {
        private const val TAG = "ThirdPartyBadges"
    }
}
