package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.rudione.chatone.util.Result
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.datetime.Instant
import kotlin.random.Random

data class PinnedChatInfo(
    val pinId: String,
    val messageId: String,
    val text: String,
    val authorId: String,
    val authorName: String,
    val authorLogin: String,
    val authorColor: String?,
    val endsAtEpochMs: Long?,
    val pinnedAtEpochMs: Long?,
    val pinnerName: String,
    val pinnerLogin: String
)

data class ModLogMessage(val id: String, val text: String, val sentAtEpochMs: Long?)

private fun parseIsoInstantOrNull(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

/** Strips wrapping quotes / "Authorization:"/"OAuth "/"Bearer "/"oauth:" prefixes some
 * stored tokens may carry, so the raw token always reaches the GQL Authorization header. */
private fun normalizeGqlToken(raw: String): String {
    var token = raw.trim()
    while (token.isNotEmpty()) {
        val before = token
        if (token.length >= 2 &&
            ((token.startsWith('"') && token.endsWith('"')) || (token.startsWith('\'') && token.endsWith('\'')))
        ) {
            token = token.substring(1, token.length - 1).trim()
        }
        if (token.startsWith("Authorization:", ignoreCase = true)) {
            token = token.substring("Authorization:".length).trim()
        }
        if (token.startsWith("OAuth ", ignoreCase = true)) {
            token = token.substring("OAuth ".length).trim()
        }
        if (token.startsWith("Bearer ", ignoreCase = true)) {
            token = token.substring("Bearer ".length).trim()
        }
        if (token.startsWith("oauth:", ignoreCase = true)) {
            token = token.substring("oauth:".length).trim()
        }
        if (token == before) break
    }
    return token
}

private fun randomHexId(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

class TwitchGqlClient(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Twitch's GQL gateway rejects requests that don't look like they came from the real
    // web client — stable per-process session/device ids + a browser UA/build-version are
    // required in addition to a valid token, or every call comes back 401 regardless of auth.
    private val sessionId = randomHexId()
    private val deviceId = randomHexId()

    private suspend fun persisted(
        operationName: String,
        sha256Hash: String,
        token: String,
        buildVariables: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
    ): Result<JsonArray> {
        return try {
            val body = buildJsonArray {
                addJsonObject {
                    put("operationName", operationName)
                    put("variables", buildJsonObject(buildVariables))
                    putJsonObject("extensions") {
                        putJsonObject("persistedQuery") {
                            put("version", 1)
                            put("sha256Hash", sha256Hash)
                        }
                    }
                }
            }
            val response = httpClient.post(GQL_ENDPOINT) {
                header("Client-Id", WEB_CLIENT_ID)
                header("Client-Session-Id", sessionId)
                header("Client-Version", CLIENT_VERSION)
                header("User-Agent", BROWSER_USER_AGENT)
                header("X-Device-Id", deviceId)
                header("Authorization", "OAuth ${normalizeGqlToken(token)}")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonArray.serializer(), body))
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Napier.w("GQL $operationName HTTP ${response.status.value}: $text", tag = TAG)
                return Result.Error(Exception("HTTP ${response.status.value}"))
            }
            Result.Success(json.parseToJsonElement(text).jsonArray)
        } catch (e: Exception) {
            Napier.w("GQL $operationName failed: ${e.message}", tag = TAG)
            Result.Error(e)
        }
    }

    suspend fun pinMessage(channelId: String, messageId: String, durationSeconds: Int, token: String): Result<String?> {
        val r = persisted("PinChatMessage", PIN_HASH, token) {
            putJsonObject("input") {
                put("channelID", channelId)
                put("messageID", messageId)
                put("type", "MOD")
                if (durationSeconds > 0) put("durationSeconds", durationSeconds)
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstOrNull()?.jsonObject
        val gqlError = node?.get("errors")
        if (gqlError != null) return Result.Error(Exception(gqlError.toString()))
        val pinId = node?.get("data")?.jsonObject
            ?.get("pinChatMessage")?.jsonObject
            ?.get("pinnedMessage")?.jsonObject
            ?.get("id")?.jsonPrimitive?.contentOrNull
        return Result.Success(pinId)
    }

    suspend fun unpinMessage(pinId: String, token: String): Result<Unit> {
        val r = persisted("unpinChatMessage", UNPIN_HASH, token) {
            putJsonObject("input") {
                put("id", pinId)
                put("reason", "UNPIN")
            }
        }
        return if (r is Result.Error) r else Result.Success(Unit)
    }

    suspend fun getCurrentPinId(channelId: String, token: String): String? {
        val r = persisted("GetPinnedChat", GET_PIN_HASH, token) {
            put("channelID", channelId)
            put("count", 1)
        }
        if (r !is Result.Success) return null
        return r.data.firstOrNull()?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("channel")?.jsonObject
            ?.get("pinnedChatMessages")?.jsonObject
            ?.get("edges")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("node")?.jsonObject
            ?.get("id")?.jsonPrimitive?.contentOrNull
    }

    suspend fun getPinnedChat(channelId: String, token: String): PinnedChatInfo? {
        val r = persisted("GetPinnedChat", GET_PIN_HASH, token) {
            put("channelID", channelId)
            put("count", 1)
        }
        if (r !is Result.Success) return null
        val node = r.data.firstOrNull()?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("channel")?.jsonObject
            ?.get("pinnedChatMessages")?.jsonObject
            ?.get("edges")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("node")?.jsonObject ?: return null

        val pinId = node["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val pinnedMessage = node["pinnedMessage"]?.jsonObject
        val sender = pinnedMessage?.get("sender")?.jsonObject
        val pinnedBy = node["pinnedBy"]?.jsonObject
        val authorName = sender?.get("displayName")?.jsonPrimitive?.contentOrNull.orEmpty()
        val authorLogin = sender?.get("login")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: authorName
        val pinnerName = pinnedBy?.get("displayName")?.jsonPrimitive?.contentOrNull.orEmpty()
        val pinnerLogin = pinnedBy?.get("login")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: pinnerName

        return PinnedChatInfo(
            pinId = pinId,
            messageId = pinnedMessage?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty(),
            text = pinnedMessage?.get("content")?.jsonObject
                ?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty(),
            authorId = sender?.get("id")?.jsonPrimitive?.contentOrNull.orEmpty(),
            authorName = authorName,
            authorLogin = authorLogin,
            authorColor = sender?.get("chatColor")?.jsonPrimitive?.contentOrNull,
            endsAtEpochMs = parseIsoInstantOrNull(node["endsAt"]?.jsonPrimitive?.contentOrNull),
            pinnedAtEpochMs = parseIsoInstantOrNull(node["updatedAt"]?.jsonPrimitive?.contentOrNull),
            pinnerName = pinnerName,
            pinnerLogin = pinnerLogin
        )
    }

    /** Claims the channel-points bonus chest. NOTE: like all our GQL, this may be rejected
     * (401) for third-party client tokens — callers must surface that honestly. */
    suspend fun claimCommunityPoints(channelId: String, claimId: String, token: String): Result<Unit> {
        val r = persisted("ClaimCommunityPoints", CLAIM_POINTS_HASH, token) {
            putJsonObject("input") {
                put("channelID", channelId)
                put("claimID", claimId)
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstOrNull()?.jsonObject
        node?.get("errors")?.let { return Result.Error(Exception(it.toString())) }
        return Result.Success(Unit)
    }

    suspend fun createRaid(sourceId: String, targetId: String, token: String): Result<String> {
        val r = persisted("chatCreateRaid", CREATE_RAID_HASH, token) {
            putJsonObject("input") {
                put("sourceID", sourceId)
                put("targetID", targetId)
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstOrNull()?.jsonObject
        node?.get("errors")?.let { return Result.Error(Exception(it.toString())) }
        val payload = node?.get("data")?.jsonObject?.get("createRaid")?.jsonObject
        val payloadError = payload?.get("error")
        if (payloadError != null && payloadError !is kotlinx.serialization.json.JsonNull) {
            val message = (payloadError as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                ?: payloadError.jsonObject.let { obj ->
                    obj["message"]?.jsonPrimitive?.contentOrNull
                        ?: obj["reason"]?.jsonPrimitive?.contentOrNull
                        ?: obj["code"]?.jsonPrimitive?.contentOrNull
                }
            return Result.Error(Exception(message ?: "Twitch rejected the raid action"))
        }
        val raidId = payload?.get("raid")?.jsonObject?.get("id")?.jsonPrimitive?.contentOrNull
        return if (raidId != null) Result.Success(raidId) else Result.Error(Exception("Failed to start raid"))
    }

    suspend fun cancelRaidGql(sourceId: String, token: String): Result<Unit> {
        val r = persisted("CancelRaid", CANCEL_RAID_HASH, token) {
            putJsonObject("input") {
                put("sourceID", sourceId)
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstOrNull()?.jsonObject
        node?.get("errors")?.let { return Result.Error(Exception(it.toString())) }
        return Result.Success(Unit)
    }

    suspend fun goRaidNow(sourceId: String, token: String): Result<Unit> {
        val r = persisted("GoRaid", GO_RAID_HASH, token) {
            putJsonObject("input") {
                put("sourceID", sourceId)
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstOrNull()?.jsonObject
        node?.get("errors")?.let { return Result.Error(Exception(it.toString())) }
        return Result.Success(Unit)
    }

    suspend fun getLatestModLogMessageBySender(
        channelId: String,
        senderId: String,
        token: String
    ): ModLogMessage? {
        val r = persisted("ViewerCardModLogsMessagesBySender", MODLOG_HASH, token) {
            put("channelID", channelId)
            put("senderID", senderId)
        }
        if (r !is Result.Success) return null
        val edges = r.data.firstOrNull()?.jsonObject
            ?.get("data")?.jsonObject
            ?.get("viewerCardModLogs")?.jsonObject
            ?.get("messages")?.jsonObject
            ?.get("edges")?.jsonArray ?: return null
        for (edge in edges) {
            val node = edge.jsonObject["node"]?.jsonObject ?: continue
            val isDeleted = node["isDeleted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            if (isDeleted) continue
            val id = node["id"]?.jsonPrimitive?.contentOrNull ?: continue
            if (id.isBlank()) continue
            val text = node["content"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
            val sentAt = parseIsoInstantOrNull(node["sentAt"]?.jsonPrimitive?.contentOrNull)
            return ModLogMessage(id, text, sentAt)
        }
        return null
    }

    companion object {
        private const val TAG = "TwitchGql"
        private const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        private const val WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        private const val CLIENT_VERSION = "ef928475-9403-42f2-8a34-55784bd08e16"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
        private const val PIN_HASH = "214191369c21f1ad67ac074795d53832329c70e4088c979040c9f86334a7d736"
        private const val UNPIN_HASH = "86409b9c86510bdc9f2c6d8e58fdc4041963c001de53577160ab649e03334511"
        private const val GET_PIN_HASH = "2d099d4c9b6af80a07d8440140c4f3dbb04d516b35c401aab7ce8f60765308d5"
        private const val MODLOG_HASH = "eb4e9869e1bb0b3ed553e1ed657fa09f8553781093569c3a5813ad09ee9c0776"
        private const val CLAIM_POINTS_HASH = "46aaeebe02c99afdf4fc97c7c0cba964124bf6b0af229395f1f6d1feed05b3d0"
        private const val CREATE_RAID_HASH = "f4fc7ac482599d81dfb6aa37100923c8c9edeea9ca2be854102a6339197f840a"
        private const val CANCEL_RAID_HASH = "42a2a699ac85256d72fff2471c75803f7ffbc767ba790725de5ad5d6e0163648"
        private const val GO_RAID_HASH = "878ca88bed0c5a5f0687ad07562cffc0bf6a3136f15e5015c0f5f5f7f367f70a"
    }
}
