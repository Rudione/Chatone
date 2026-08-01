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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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

data class GqlUsercardMessage(
    val id: String,
    val text: String,
    val sentAtEpochMs: Long?,
    val cursor: String,
    val isDeleted: Boolean,
    val deletedBy: String?
)

data class GqlUsercardMessagePage(
    val messages: List<GqlUsercardMessage>,
    val nextCursor: String?,
    val hasNextPage: Boolean
)

data class GqlTokenIdentity(val userId: String, val login: String, val displayName: String)

data class GqlChannelPointReward(
    val id: String,
    val title: String,
    val prompt: String,
    val cost: Long,
    val pricingType: String,
    val isEnabled: Boolean,
    val isInStock: Boolean,
    val isUserInputRequired: Boolean,
    val imageUrl: String? = null
)

data class GqlChannelPointRewardsInfo(
    val channelId: String,
    val balance: Long,
    val rewards: List<GqlChannelPointReward>,
    val currencyIconUrl: String? = null
)

data class GqlChannelPointRedeemResult(val balance: Long)

private fun parseIsoInstantOrNull(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return try {
        Instant.parse(iso).toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

private fun normalizeGqlToken(raw: String): String {
    var token = raw.trim()
    while (token.isNotEmpty()) {
        val before = token
        if (token.length >= 2 &&
            ((token.startsWith('"') && token.endsWith('"')) || (token.startsWith('\'') && token.endsWith(
                '\''
            )))
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
    return bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
}

private fun randomUuid(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
    val hex = bytes.joinToString("") { it.toUByte().toString(16).padStart(2, '0') }
    return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
            "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
}

class TwitchGqlClient(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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

    private suspend fun persistedTvBatch(
        entries: List<Triple<String, String, kotlinx.serialization.json.JsonObject>>,
        token: String
    ): Result<JsonArray> {
        return try {
            val body = buildJsonArray {
                entries.forEach { (operationName, sha256Hash, variables) ->
                    addJsonObject {
                        put("operationName", operationName)
                        put("variables", variables)
                        putJsonObject("extensions") {
                            putJsonObject("persistedQuery") {
                                put("version", 1)
                                put("sha256Hash", sha256Hash)
                            }
                        }
                    }
                }
            }
            val response = httpClient.post(GQL_ENDPOINT) {
                header("Client-Id", TV_CLIENT_ID)
                header("Client-Session-Id", sessionId)
                header("Client-Version", CLIENT_VERSION)
                header("Origin", TV_ORIGIN)
                header("Referer", TV_REFERER)
                header("User-Agent", TV_USER_AGENT)
                header("X-Device-Id", deviceId)
                header("Authorization", "OAuth ${normalizeGqlToken(token)}")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonArray.serializer(), body))
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Napier.w("GQL TV batch HTTP ${response.status.value}: $text", tag = TAG)
                return Result.Error(Exception("HTTP ${response.status.value}"))
            }
            Result.Success(json.parseToJsonElement(text).jsonArray)
        } catch (e: Exception) {
            Napier.w("GQL TV batch failed: ${e.message}", tag = TAG)
            Result.Error(e)
        }
    }

    private fun JsonArray.dataNodeForOperation(operationName: String): kotlinx.serialization.json.JsonObject? {
        forEach { el ->
            val obj = el.jsonObject
            val opName =
                obj["extensions"]?.jsonObject?.get("operationName")?.jsonPrimitive?.contentOrNull
            if (opName != null && opName.equals(operationName, ignoreCase = true)) return obj
        }
        return firstDataNode()
    }

    suspend fun pinMessage(
        channelId: String,
        messageId: String,
        durationSeconds: Int,
        token: String
    ): Result<String?> {
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

    suspend fun claimCommunityPoints(
        channelId: String,
        claimId: String,
        token: String
    ): Result<Unit> {
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
            val isDeleted =
                node["isDeleted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
            if (isDeleted) continue
            val id = node["id"]?.jsonPrimitive?.contentOrNull ?: continue
            if (id.isBlank()) continue
            val text =
                node["content"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
            val sentAt = parseIsoInstantOrNull(node["sentAt"]?.jsonPrimitive?.contentOrNull)
            return ModLogMessage(id, text, sentAt)
        }
        return null
    }

    suspend fun getUsercardMessagesBySender(
        channelId: String,
        senderId: String,
        cursor: String?,
        token: String
    ): GqlUsercardMessagePage? {
        val variables = buildJsonObject {
            put("channelID", channelId)
            put("senderID", senderId)
            if (!cursor.isNullOrBlank()) put("cursor", cursor)
        }
        val r = persistedTvBatch(
            listOf(Triple("ViewerCardModLogsMessagesBySender", MODLOG_HASH, variables)),
            token
        )
        if (r !is Result.Success) return null
        val messagesNode = r.data.dataNodeForOperation("ViewerCardModLogsMessagesBySender")
            ?.get("data")?.jsonObject
            ?.get("viewerCardModLogs")?.jsonObject
            ?.get("messages")?.jsonObject ?: return null
        val edges = messagesNode["edges"]?.jsonArray ?: return null
        val hasNextPage = messagesNode["pageInfo"]?.jsonObject
            ?.get("hasNextPage")?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false

        val messages = edges.mapNotNull { edge ->
            val node = edge.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
            val id = node["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            GqlUsercardMessage(
                id = id,
                text = node["content"]?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty(),
                sentAtEpochMs = parseIsoInstantOrNull(node["sentAt"]?.jsonPrimitive?.contentOrNull),
                cursor = edge.jsonObject["cursor"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                isDeleted = node["isDeleted"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                    ?: false,
                deletedBy = node["lastUpdatedBy"]?.jsonObject?.get("displayName")?.jsonPrimitive?.contentOrNull
            )
        }
        return GqlUsercardMessagePage(
            messages = messages,
            nextCursor = messages.lastOrNull()?.cursor,
            hasNextPage = hasNextPage
        )
    }

    suspend fun getChannelPointRewardsGql(
        channelLogin: String,
        token: String
    ): GqlChannelPointRewardsInfo? {
        val variables = buildJsonObject {
            put("channelLogin", channelLogin)
            putJsonArray("includeGoalTypes") { add("CREATOR"); add("BOOST") }
        }
        val r = persistedTvBatch(
            listOf(Triple("ChannelPointsContext", CHANNEL_POINTS_CONTEXT_HASH, variables)),
            token
        )
        if (r !is Result.Success) return null
        val node = r.data.dataNodeForOperation("ChannelPointsContext") ?: return null
        node.gqlErrorOrNull()?.let { return null }
        val data = node["data"]?.jsonObject ?: return null
        val community = data["community"]?.jsonObject ?: return null
        val channel = community["channel"]?.jsonObject ?: return null
        val settings = channel["communityPointsSettings"]?.jsonObject ?: return null
        val self = channel["self"]?.jsonObject
        val balance = self?.get("communityPoints")?.jsonObject
            ?.get("balance")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L

        val rewards = settings["customRewards"]?.jsonArray.orEmpty().mapNotNull { el ->
            val obj = el.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val pricingType = obj["pricingType"]?.jsonPrimitive?.contentOrNull ?: "POINTS"
            val cost = obj["cost"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
            if ((pricingType != "POINTS" && pricingType != "BITS") || cost <= 0) return@mapNotNull null
            val imageObj = obj["image"] as? JsonObject
            val defaultImageObj = obj["defaultImage"] as? JsonObject
            val imageUrl = imageObj?.get("url4x")?.jsonPrimitive?.contentOrNull
                ?: imageObj?.get("url2x")?.jsonPrimitive?.contentOrNull
                ?: defaultImageObj?.get("url4x")?.jsonPrimitive?.contentOrNull
            GqlChannelPointReward(
                id = id,
                title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                prompt = obj["prompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                cost = cost,
                pricingType = pricingType,
                isEnabled = (obj["isEnabled"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                    ?: false) &&
                        !(obj["isPaused"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                            ?: false),
                isInStock = obj["isInStock"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                    ?: true,
                isUserInputRequired = obj["isUserInputRequired"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
                    ?: false,
                imageUrl = imageUrl
            )
        }
        val settingsImageObj = settings["image"] as? JsonObject
        val currencyIconUrl = settingsImageObj?.get("png")?.jsonPrimitive?.contentOrNull
            ?: settingsImageObj?.get("url")?.jsonPrimitive?.contentOrNull

        return GqlChannelPointRewardsInfo(
            channelId = community["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            balance = balance,
            currencyIconUrl = currencyIconUrl,
            rewards = rewards
        )
    }

    suspend fun redeemCustomRewardGql(
        channelId: String,
        reward: GqlChannelPointReward,
        textInput: String,
        token: String
    ): Result<GqlChannelPointRedeemResult> {
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("channelID", channelId)
                put("cost", reward.cost)
                put("pricingType", reward.pricingType)
                put("rewardID", reward.id)
                put("title", reward.title)
                put("transactionID", randomHexId())
                if (reward.prompt.isNotBlank()) put("prompt", reward.prompt)
                if (textInput.isNotBlank()) put("textInput", textInput)
            }
        }
        val r = persistedTvBatch(
            listOf(Triple("RedeemCustomReward", REDEEM_CUSTOM_REWARD_HASH, variables)),
            token
        )
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.dataNodeForOperation("RedeemCustomReward")
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val payload =
            node?.get("data")?.jsonObject?.get("redeemCommunityPointsCustomReward")?.jsonObject
        val err = payload?.get("error")
        if (err != null && err !is kotlinx.serialization.json.JsonNull) {
            val msg = err.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: err.jsonObject["code"]?.jsonPrimitive?.contentOrNull
            return Result.Error(Exception(msg ?: "Failed to redeem reward"))
        }
        if (payload == null) return Result.Error(Exception("Failed to redeem reward"))
        val balance = payload["balance"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        return Result.Success(GqlChannelPointRedeemResult(balance))
    }

    private suspend fun inlineQuery(
        query: String,
        token: String,
        variables: kotlinx.serialization.json.JsonObject = buildJsonObject { }
    ): Result<JsonArray> {
        return try {
            val body = buildJsonArray {
                addJsonObject {
                    put("query", query)
                    put("variables", variables)
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
                Napier.w("GQL inline HTTP ${response.status.value}: $text", tag = TAG)
                return Result.Error(Exception("HTTP ${response.status.value}"))
            }
            Result.Success(json.parseToJsonElement(text).jsonArray)
        } catch (e: Exception) {
            Napier.w("GQL inline query failed: ${e.message}", tag = TAG)
            Result.Error(e)
        }
    }

    private fun JsonArray.firstDataNode() = firstOrNull()?.jsonObject
    private fun kotlinx.serialization.json.JsonObject.gqlErrorOrNull(): String? =
        get("errors")?.let { it.toString() }

    private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonElement.asArrayOrNull(): JsonArray? = this as? JsonArray

    suspend fun validateCustomToken(token: String): GqlTokenIdentity? {
        val q = "query ChatoneValidateToken { currentUser { id login displayName } }"
        val r = inlineQuery(q, token)
        if (r !is Result.Success) return null
        val node = r.data.firstDataNode() ?: return null
        if (node.gqlErrorOrNull() != null) return null
        val user = node["data"]?.jsonObject?.get("currentUser")?.jsonObject ?: return null
        val id = user["id"]?.jsonPrimitive?.contentOrNull ?: return null
        if (id.isBlank()) return null
        return GqlTokenIdentity(
            userId = id,
            login = user["login"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            displayName = user["displayName"]?.jsonPrimitive?.contentOrNull.orEmpty()
        )
    }

    suspend fun createPollGql(
        channelId: String,
        title: String,
        choices: List<String>,
        durationSeconds: Int,
        pointsPerVote: Int?,
        token: String
    ): Result<Unit> {
        val r = persisted("CreatePoll", CREATE_POLL_HASH, token) {
            putJsonObject("input") {
                put("title", title)
                put("durationSeconds", durationSeconds)
                put("ownedBy", channelId)
                put("multichoiceEnabled", true)
                put("isCommunityPointsVotingEnabled", pointsPerVote != null)
                put("communityPointsCost", pointsPerVote ?: 0)
                putJsonArray("choices") {
                    choices.forEach { c -> addJsonObject { put("title", c) } }
                }
            }
        }
        return finishPollMutation(r, "createPoll", "Failed to create poll")
    }

    suspend fun getViewablePollGql(channelLogin: String, token: String): io.rudione.chatone.data.remote.dto.PollData? {
        val r = persisted("ChannelPollContext_GetViewablePoll", GET_VIEWABLE_POLL_HASH, token) {
            put("login", channelLogin)
        }
        if (r !is Result.Success) return null
        val data = r.data.firstDataNode()?.get("data")?.asObjectOrNull() ?: return null
        val context = data["channel"]?.asObjectOrNull() ?: data["user"]?.asObjectOrNull() ?: return null
        val poll = context["viewablePoll"]?.asObjectOrNull() ?: return null
        val id = poll["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = poll["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val choices = poll["choices"]?.asArrayOrNull()?.mapNotNull { c ->
            val co = c.asObjectOrNull() ?: return@mapNotNull null
            val cid = co["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val votes = co["votes"]?.asObjectOrNull()
            io.rudione.chatone.data.remote.dto.PollChoice(
                id = cid,
                title = co["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                votes = votes?.get("base")?.jsonPrimitive?.intOrNull ?: 0,
                channelPointsVotes = votes?.get("communityPoints")?.jsonPrimitive?.intOrNull ?: 0
            )
        }.orEmpty()
        if (choices.isEmpty()) return null
        val settings = poll["settings"]?.asObjectOrNull()
        val cpVotes = settings?.get("communityPointsVotes")?.asObjectOrNull()
        val remainingMs = poll["remainingDurationMilliseconds"]?.jsonPrimitive?.longOrNull
        val selfChoiceId = poll["self"]?.asObjectOrNull()
            ?.get("voter")?.asObjectOrNull()
            ?.get("choices")?.asArrayOrNull()?.firstOrNull()?.asObjectOrNull()
            ?.get("pollChoice")?.asObjectOrNull()
            ?.get("id")?.jsonPrimitive?.contentOrNull
        return io.rudione.chatone.data.remote.dto.PollData(
            id = id,
            title = title,
            choices = choices,
            channelPointsVotingEnabled = cpVotes?.get("isEnabled")?.jsonPrimitive?.booleanOrNull ?: false,
            channelPointsPerVote = cpVotes?.get("cost")?.jsonPrimitive?.intOrNull ?: 0,
            status = poll["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            duration = ((remainingMs ?: 0L) / 1000L).toInt(),
            selfVoteChoiceId = selfChoiceId,
            remainingMs = remainingMs
        )
    }

    suspend fun terminatePollGql(
        pollId: String,
        currentUserId: String?,
        token: String
    ): Result<Unit> {
        val entries = buildList {
            add(
                Triple(
                    "TerminatePoll",
                    TERMINATE_POLL_HASH,
                    buildJsonObject { putJsonObject("input") { put("pollID", pollId) } }
                )
            )
            if (!currentUserId.isNullOrBlank()) {
                add(
                    Triple(
                        "Core_Services_Spade_ChatEvent_User",
                        SPADE_CHAT_EVENT_HASH,
                        buildJsonObject { put("id", currentUserId) }
                    )
                )
            }
        }
        val r = persistedTvBatch(entries, token)
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.dataNodeForOperation("TerminatePoll")
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val returnedPollId = node?.get("data")?.jsonObject
            ?.get("terminatePoll")?.jsonObject
            ?.get("poll")?.jsonObject
            ?.get("id")?.jsonPrimitive?.contentOrNull
        if (returnedPollId.isNullOrEmpty() || returnedPollId != pollId) {
            return Result.Error(Exception("Failed to end poll"))
        }
        return Result.Success(Unit)
    }

    suspend fun archivePollGql(pollId: String, token: String): Result<Unit> {
        val r = persisted("ArchivePoll", ARCHIVE_POLL_HASH, token) {
            putJsonObject("input") { put("pollID", pollId) }
        }
        return finishPollMutation(r, "archivePoll", "Failed to delete poll")
    }

    suspend fun voteInPollGql(
        pollId: String,
        choiceId: String,
        userId: String,
        extraVotes: Int,
        pointsPerVote: Int?,
        token: String
    ): Result<Unit> {
        if (extraVotes > 0 && (pointsPerVote == null || pointsPerVote <= 0)) {
            return Result.Error(Exception("Paid voting is enabled, but the point cost is unknown"))
        }
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("pollID", pollId)
                put("choiceID", choiceId)
                put("userID", userId)
                put("voteID", randomUuid())
                if (extraVotes > 0) {
                    putJsonObject("tokens") { put("channelPoints", extraVotes * pointsPerVote!!) }
                }
            }
        }
        val r = inlineQuery(VOTE_IN_POLL_MUTATION, token, variables)
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstDataNode()
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val payload = node?.get("data")?.jsonObject?.get("voteInPoll")?.jsonObject
        val err = payload?.get("error")
        if (err != null && err !is kotlinx.serialization.json.JsonNull) {
            val code = err.jsonObject["code"]?.jsonPrimitive?.contentOrNull
            return Result.Error(Exception(code ?: "Failed to vote"))
        }
        return Result.Success(Unit)
    }

    private fun finishPollMutation(
        r: Result<JsonArray>,
        field: String,
        failMsg: String
    ): Result<Unit> {
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstDataNode()
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val payload = node?.get("data")?.jsonObject?.get(field)?.jsonObject
        val err = payload?.get("error")
        if (err != null && err !is kotlinx.serialization.json.JsonNull) {
            val msg = err.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: err.jsonObject["code"]?.jsonPrimitive?.contentOrNull
            return Result.Error(Exception(msg ?: failMsg))
        }
        return Result.Success(Unit)
    }

    suspend fun createPredictionGql(
        channelId: String,
        title: String,
        outcomes: List<String>,
        windowSeconds: Int,
        token: String
    ): Result<Unit> {
        val r = persisted("createPredictionEvent", CREATE_PREDICTION_HASH, token) {
            putJsonObject("input") {
                put("channelID", channelId)
                put("title", title)
                put("predictionWindowSeconds", windowSeconds)
                putJsonArray("outcomes") {
                    outcomes.forEachIndexed { i, o ->
                        addJsonObject {
                            put("title", o)
                            put("color", if (i % 2 == 0) "BLUE" else "PINK")
                        }
                    }
                }
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstDataNode()
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val payload = node?.get("data")?.jsonObject?.get("createPredictionEvent")?.jsonObject
        val err = payload?.get("error")
        if (err != null && err !is kotlinx.serialization.json.JsonNull) {
            val msg = err.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                ?: err.jsonObject["code"]?.jsonPrimitive?.contentOrNull
            return Result.Error(Exception(msg ?: "Failed to create prediction"))
        }
        return Result.Success(Unit)
    }

    suspend fun getActivePredictionGql(channelLogin: String, token: String): io.rudione.chatone.data.remote.dto.PredictionData? {
        val q = """
            query ChannelPointsPredictionContext(${'$'}channelLogin: String!) {
              channel(name: ${'$'}channelLogin) {
                id
                activePredictionEvents {
                  id title status predictionWindowSeconds createdAt lockedAt
                  outcomes { id title color totalUsers totalPoints }
                }
                lockedPredictionEvents {
                  id title status predictionWindowSeconds createdAt lockedAt
                  outcomes { id title color totalUsers totalPoints }
                }
              }
            }
        """.trimIndent()
        val r = inlineQuery(q, token, buildJsonObject { put("channelLogin", channelLogin) })
        if (r !is Result.Success) return null
        val channel = r.data.firstDataNode()?.get("data")?.asObjectOrNull()?.get("channel")?.asObjectOrNull()
            ?: return null
        val events = (channel["activePredictionEvents"]?.asArrayOrNull()?.toList().orEmpty()) +
                (channel["lockedPredictionEvents"]?.asArrayOrNull()?.toList().orEmpty())
        val ev = events.firstOrNull()?.asObjectOrNull() ?: return null
        val id = ev["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = ev["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val outcomes = ev["outcomes"]?.asArrayOrNull()?.mapNotNull { o ->
            val oo = o.asObjectOrNull() ?: return@mapNotNull null
            val oid = oo["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            io.rudione.chatone.data.remote.dto.PredictionOutcome(
                id = oid,
                title = oo["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                users = oo["totalUsers"]?.jsonPrimitive?.intOrNull ?: 0,
                channelPoints = oo["totalPoints"]?.jsonPrimitive?.intOrNull ?: 0,
                color = oo["color"]?.jsonPrimitive?.contentOrNull ?: "BLUE"
            )
        }.orEmpty()
        if (outcomes.isEmpty()) return null
        return io.rudione.chatone.data.remote.dto.PredictionData(
            id = id,
            title = title,
            outcomes = outcomes,
            predictionWindow = ev["predictionWindowSeconds"]?.jsonPrimitive?.intOrNull ?: 0,
            status = ev["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            createdAt = ev["createdAt"]?.jsonPrimitive?.contentOrNull,
            lockedAt = ev["lockedAt"]?.jsonPrimitive?.contentOrNull
        )
    }

    suspend fun lockPredictionGql(eventId: String, token: String): Result<Unit> {
        val r = persisted("LockPrediction", LOCK_PREDICTION_HASH, token) {
            putJsonObject("input") { put("id", eventId) }
        }
        if (r is Result.Error) return r
        (r as Result.Success).data.firstDataNode()?.gqlErrorOrNull()
            ?.let { return Result.Error(Exception(it)) }
        return Result.Success(Unit)
    }

    suspend fun cancelPredictionGql(eventId: String, token: String): Result<Unit> {
        val r = persisted("DeletePrediction", DELETE_PREDICTION_HASH, token) {
            putJsonObject("input") { put("id", eventId) }
        }
        if (r is Result.Error) return r
        (r as Result.Success).data.firstDataNode()?.gqlErrorOrNull()
            ?.let { return Result.Error(Exception(it)) }
        return Result.Success(Unit)
    }

    suspend fun resolvePredictionGql(
        eventId: String,
        outcomeId: String,
        token: String
    ): Result<Unit> {
        val r = persisted("ResolvePrediction", RESOLVE_PREDICTION_HASH, token) {
            putJsonObject("input") {
                put("eventID", eventId)
                put("outcomeID", outcomeId)
            }
        }
        if (r is Result.Error) return r
        (r as Result.Success).data.firstDataNode()?.gqlErrorOrNull()
            ?.let { return Result.Error(Exception(it)) }
        return Result.Success(Unit)
    }

    suspend fun makePredictionGql(
        eventId: String,
        outcomeId: String,
        points: Int,
        token: String
    ): Result<Unit> {
        val r = persisted("MakePrediction", MAKE_PREDICTION_HASH, token) {
            putJsonObject("input") {
                put("eventID", eventId)
                put("outcomeID", outcomeId)
                put("points", points)
                put("transactionID", randomUuid().replace("-", ""))
            }
        }
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstDataNode()
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val payload = node?.get("data")?.jsonObject?.get("makePrediction")?.jsonObject
        val err = payload?.get("error")
        if (err != null && err !is kotlinx.serialization.json.JsonNull) {
            val code = err.jsonObject["code"]?.jsonPrimitive?.contentOrNull
            return Result.Error(Exception(code ?: "Failed to place prediction"))
        }
        return Result.Success(Unit)
    }

    companion object {
        private const val TAG = "TwitchGql"
        private const val GQL_ENDPOINT = "https://gql.twitch.tv/gql"
        private const val WEB_CLIENT_ID = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        private const val CLIENT_VERSION = "ef928475-9403-42f2-8a34-55784bd08e16"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
        private const val TV_CLIENT_ID = "ue6666qo983tsx6so1t0vnawi233wa"
        private const val TV_ORIGIN = "https://android.tv.twitch.tv"
        private const val TV_REFERER = "https://android.tv.twitch.tv/"
        private const val TV_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 7.1; Smart Box C1) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36"
        private const val SPADE_CHAT_EVENT_HASH =
            "9cb0f182474382a0e72e817318460eeefc7c1cab0d163ac064a603d850b085ea"
        private const val PIN_HASH =
            "214191369c21f1ad67ac074795d53832329c70e4088c979040c9f86334a7d736"
        private const val UNPIN_HASH =
            "86409b9c86510bdc9f2c6d8e58fdc4041963c001de53577160ab649e03334511"
        private const val GET_PIN_HASH =
            "2d099d4c9b6af80a07d8440140c4f3dbb04d516b35c401aab7ce8f60765308d5"
        private const val MODLOG_HASH =
            "eb4e9869e1bb0b3ed553e1ed657fa09f8553781093569c3a5813ad09ee9c0776"
        private const val CLAIM_POINTS_HASH =
            "46aaeebe02c99afdf4fc97c7c0cba964124bf6b0af229395f1f6d1feed05b3d0"
        private const val CREATE_RAID_HASH =
            "f4fc7ac482599d81dfb6aa37100923c8c9edeea9ca2be854102a6339197f840a"
        private const val CANCEL_RAID_HASH =
            "42a2a699ac85256d72fff2471c75803f7ffbc767ba790725de5ad5d6e0163648"
        private const val GO_RAID_HASH =
            "878ca88bed0c5a5f0687ad07562cffc0bf6a3136f15e5015c0f5f5f7f367f70a"

        private const val CREATE_POLL_HASH =
            "4b1461a13fe166a59044961db192747d606f71a89abc3bfdecf79fe862d205cf"
        private const val GET_VIEWABLE_POLL_HASH =
            "e83188a3836c636393df3191665e543a03733d7c51d3ade3d85e42aa46c2bf55"
        private const val TERMINATE_POLL_HASH =
            "2701ef0594dae5f532ce68e58cc3036a6d020755eef49927f98c14017fd819b2"
        private const val ARCHIVE_POLL_HASH =
            "444ead3d68d94601cb66519e36c9f6c6fd9ba8b827a4299b8ed3604e57918d92"
        private const val CREATE_PREDICTION_HASH =
            "92268878ac4abe722bcdcba85a4e43acdd7a99d86b05851759e1d8f385cc32ea"
        private const val LOCK_PREDICTION_HASH =
            "1f2b1eb44af35f055308e78ffbe81c2f958408f9b32d076a759a84ab213285d4"
        private const val DELETE_PREDICTION_HASH =
            "35d375614e426624456ee7be4a2e0fbc0a410c0a91c21f6044cb3cd5c38c4e4d"
        private const val RESOLVE_PREDICTION_HASH =
            "10c803ec11bb8c2957d66bc6a47349dc3c5f51d694585b5ebc37ba656da413c1"
        private const val MAKE_PREDICTION_HASH =
            "b44682ecc88358817009f20e69d75081b1e58825bb40aa53d5dbadcc17c881d8"
        private val VOTE_IN_POLL_MUTATION = """
            mutation VoteInPoll(${'$'}input: VoteInPollInput!) {
                voteInPoll(input: ${'$'}input) {
                    error { code }
                }
            }
        """.trimIndent()
        private const val CHANNEL_POINTS_CONTEXT_HASH =
            "7fe050e3761eb2cf258d70ee1a21cbd76fa8cf3d7e7b12fc437e7029d446b5e3"
        private const val REDEEM_CUSTOM_REWARD_HASH =
            "d56249a7adb4978898ea3412e196688d4ac3cea1c0c2dfd65561d229ea5dcc42"
    }
}
