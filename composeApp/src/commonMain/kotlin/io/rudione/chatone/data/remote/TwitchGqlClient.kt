package io.rudione.chatone.data.remote

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
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

data class GqlUserDossier(
    val userId: String,
    val login: String,
    val displayName: String,
    val createdAtEpochMs: Long?,
    val bio: String?,
    val language: String?,
    val teamName: String?,
    val followerCount: Int?,
    val isPartner: Boolean,
    val isAffiliate: Boolean,
    val isStaff: Boolean,
    val hasVideos: Boolean,
    val liveViewers: Int?,
    val liveGame: String?,
    val liveSinceEpochMs: Long?,
    val lastBroadcastEpochMs: Long?,
    val lastBroadcastGame: String?,
    val followedChannelAtEpochMs: Long?,
    val subscriptionTier: String?
) {
    val isStreamer: Boolean
        get() = isPartner || isAffiliate || lastBroadcastEpochMs != null || hasVideos

    val isLive: Boolean get() = liveViewers != null
}

data class GqlDisplayBadge(
    val setId: String,
    val version: String,
    val title: String,
    val description: String?,
    val imageUrl: String
)

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

data class GqlTargetedModAction(
    val id: String,
    val type: String,
    val timestampMs: Long?
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
    val imageUrl: String? = null,
    val automaticType: TwitchAutomaticRewardType? = null
) {
    val isRedeemableInApp: Boolean
        get() = automaticType == null || automaticType.isRedeemableInApp
}

enum class TwitchAutomaticRewardType(val rawValue: String, val isRedeemableInApp: Boolean) {
    SEND_HIGHLIGHTED_MESSAGE("SEND_HIGHLIGHTED_MESSAGE", false),
    RANDOM_SUB_EMOTE_UNLOCK("RANDOM_SUB_EMOTE_UNLOCK", true),
    CHOSEN_SUB_EMOTE_UNLOCK("CHOSEN_SUB_EMOTE_UNLOCK", false),
    CHOSEN_MODIFIED_SUB_EMOTE_UNLOCK("CHOSEN_MODIFIED_SUB_EMOTE_UNLOCK", false),
    SINGLE_MESSAGE_BYPASS_SUB_MODE("SINGLE_MESSAGE_BYPASS_SUB_MODE", false),
    SEND_ANIMATED_MESSAGE("SEND_ANIMATED_MESSAGE", false),
    SEND_GIGANTIFIED_EMOTE("SEND_GIGANTIFIED_EMOTE", false),
    CELEBRATION("CELEBRATION", false);

    companion object {
        fun fromRaw(raw: String?): TwitchAutomaticRewardType? =
            entries.firstOrNull { it.rawValue == raw }
    }
}

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
    private val httpClient: HttpClient,
    private val tokenResolver: FirstPartyTokenResolver = FirstPartyTokenResolver(httpClient)
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
            val client = tokenResolver.clientFor(normalizeGqlToken(token))
            val response = httpClient.post(GQL_ENDPOINT) {
                sessionHeaders(client, token)
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
                sessionHeaders(TwitchFirstPartyClient.TV, token)
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

    suspend fun getTargetedModActions(
        channelId: String,
        targetId: String,
        token: String,
        maxPages: Int = TARGETED_ACTIONS_MAX_PAGES
    ): List<GqlTargetedModAction>? {
        if (channelId.isBlank() || targetId.isBlank()) return null
        val actions = mutableListOf<GqlTargetedModAction>()
        var cursor: String? = null
        repeat(maxPages) {
            val variables = buildJsonObject {
                put("channelID", channelId)
                put("targetID", targetId)
                if (!cursor.isNullOrBlank()) put("after", cursor)
            }
            val r = inlineQuery(TARGETED_MOD_ACTIONS_QUERY, token, variables)
            if (r !is Result.Success) return actions.takeIf { it.isNotEmpty() }
            val node = r.data.firstDataNode()
            val result = node
                ?.get("data")?.asObjectOrNull()
                ?.get("viewerCardModLogs")?.asObjectOrNull()
                ?.get("targetedActions")?.asObjectOrNull()
            if (result == null) {
                Napier.w("Targeted mod actions unavailable: ${node?.gqlErrorOrNull()}", tag = TAG)
                return actions.takeIf { it.isNotEmpty() }
            }
            val edges = result["edges"]?.asArrayOrNull()
            if (edges == null) {
                Napier.w("Targeted mod actions denied: ${result["code"]}", tag = TAG)
                return actions.takeIf { it.isNotEmpty() }
            }
            edges.forEach { edge ->
                val node = edge.asObjectOrNull()?.get("node")?.asObjectOrNull() ?: return@forEach
                val id = node["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                val type = node["type"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                actions += GqlTargetedModAction(
                    id = id,
                    type = type,
                    timestampMs = parseIsoInstantOrNull(node["timestamp"]?.jsonPrimitive?.contentOrNull)
                )
            }
            val hasNext = result["pageInfo"]?.asObjectOrNull()
                ?.get("hasNextPage")?.jsonPrimitive?.booleanOrNull == true
            cursor = edges.lastOrNull()?.asObjectOrNull()?.get("cursor")?.jsonPrimitive?.contentOrNull
            if (!hasNext || cursor.isNullOrBlank()) return actions
        }
        return actions
    }

    suspend fun getChannelRewardTitles(channelLogin: String): Map<String, String>? {
        if (channelLogin.isBlank()) return null
        return try {
            val body = buildJsonObject {
                put("query", CHANNEL_REWARD_TITLES_QUERY)
                putJsonObject("variables") { put("login", channelLogin.lowercase()) }
            }
            val response = httpClient.post(GQL_ENDPOINT) {
                header("Client-Id", TwitchFirstPartyClient.WEB.clientId)
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
            if (!response.status.isSuccess()) return null
            val rewards = json.parseToJsonElement(response.bodyAsText()).asObjectOrNull()
                ?.get("data")?.asObjectOrNull()
                ?.get("channel")?.asObjectOrNull()
                ?.get("communityPointsSettings")?.asObjectOrNull()
                ?.get("customRewards")?.asArrayOrNull() ?: return null
            rewards.mapNotNull { element ->
                val reward = element.asObjectOrNull() ?: return@mapNotNull null
                val id = reward["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val title = reward["title"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                id to title
            }.toMap()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Reward titles request failed: ${e.message}", tag = TAG)
            null
        }
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

        val customRewards = settings["customRewards"]?.jsonArray.orEmpty()
            .mapNotNull { customRewardFromJson(it.jsonObject) }
        val automaticRewards = settings["automaticRewards"]?.jsonArray.orEmpty()
            .mapNotNull { automaticRewardFromJson(it.jsonObject) }
        val rewards = customRewards + automaticRewards
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

    private fun customRewardFromJson(obj: JsonObject): GqlChannelPointReward? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val pricingType = obj["pricingType"]?.jsonPrimitive?.contentOrNull ?: "POINTS"
        val cost = obj["cost"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        if ((pricingType != "POINTS" && pricingType != "BITS") || cost <= 0) return null
        return GqlChannelPointReward(
            id = id,
            title = obj["title"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            prompt = obj["prompt"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            cost = cost,
            pricingType = pricingType,
            isEnabled = obj.boolOrNull("isEnabled") == true && obj.boolOrNull("isPaused") != true,
            isInStock = obj.boolOrNull("isInStock") ?: true,
            isUserInputRequired = obj.boolOrNull("isUserInputRequired") ?: false,
            imageUrl = rewardImageUrl(obj)
        )
    }

    private fun automaticRewardFromJson(obj: JsonObject): GqlChannelPointReward? {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val type = TwitchAutomaticRewardType.fromRaw(
            obj["type"]?.jsonPrimitive?.contentOrNull
        ) ?: return null
        val pricingType = obj["pricingType"]?.jsonPrimitive?.contentOrNull ?: "POINTS"
        val costKey = if (pricingType == "BITS") "bitsCost" else "cost"
        val cost = obj[costKey]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
        if (cost <= 0) return null
        return GqlChannelPointReward(
            id = id,
            title = "",
            prompt = "",
            cost = cost,
            pricingType = pricingType,
            isEnabled = obj.boolOrNull("isEnabled") ?: false,
            isInStock = obj.boolOrNull("isInStock") ?: true,
            isUserInputRequired = false,
            imageUrl = rewardImageUrl(obj),
            automaticType = type
        )
    }

    private fun rewardImageUrl(obj: JsonObject): String? {
        val image = obj["image"] as? JsonObject
        val defaultImage = obj["defaultImage"] as? JsonObject
        return image?.get("url4x")?.jsonPrimitive?.contentOrNull
            ?: image?.get("url2x")?.jsonPrimitive?.contentOrNull
            ?: defaultImage?.get("url4x")?.jsonPrimitive?.contentOrNull
            ?: defaultImage?.get("url2x")?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.boolOrNull(key: String): Boolean? =
        this[key]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()

    suspend fun unlockRandomSubEmoteGql(
        channelId: String,
        cost: Long,
        token: String
    ): Result<Unit> {
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("channelID", channelId)
                put("cost", cost)
                put("transactionID", randomHexId())
            }
        }
        val r = inlineQuery(UNLOCK_RANDOM_SUB_EMOTE_MUTATION, token, variables)
        if (r is Result.Error) return r
        val node = (r as Result.Success).data.firstDataNode()
        node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
        val payload = node?.get("data")?.jsonObject
            ?.get("unlockRandomSubscriberEmote")?.jsonObject
            ?: return Result.Error(Exception("Failed to redeem reward"))
        val error = payload["error"]
        if (error != null && error !is kotlinx.serialization.json.JsonNull) {
            val message = error.jsonObject["code"]?.jsonPrimitive?.contentOrNull
                ?: error.jsonObject["message"]?.jsonPrimitive?.contentOrNull
            return Result.Error(Exception(message ?: "Failed to redeem reward"))
        }
        return Result.Success(Unit)
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
            val client = tokenResolver.clientFor(normalizeGqlToken(token))
            val response = httpClient.post(GQL_ENDPOINT) {
                sessionHeaders(client, token)
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

    private fun HttpRequestBuilder.sessionHeaders(client: TwitchFirstPartyClient, token: String) {
        header("Client-Id", client.clientId)
        header("Client-Session-Id", sessionId)
        header("Client-Version", CLIENT_VERSION)
        client.origin?.let { header("Origin", it) }
        client.referer?.let { header("Referer", it) }
        header("User-Agent", client.userAgent)
        header("X-Device-Id", deviceId)
        header("Authorization", "OAuth ${normalizeGqlToken(token)}")
    }

    private fun HttpRequestBuilder.firstPartyHeaders(
        profile: GqlClientProfile,
        token: String,
        integrityToken: String?
    ) {
        header("Client-Id", profile.clientId)
        header("User-Agent", profile.userAgent)
        header("X-Device-Id", deviceId)
        header("Authorization", "OAuth ${normalizeGqlToken(token)}")
        if (profile.webHeaders) {
            header("Client-Session-Id", sessionId)
            header("Client-Version", CLIENT_VERSION)
        }
        integrityToken?.let { header("Client-Integrity", it) }
    }

    private suspend fun inlineProfileQuery(
        query: String,
        token: String,
        variables: kotlinx.serialization.json.JsonObject,
        profile: GqlClientProfile,
        integrityToken: String? = null
    ): Result<JsonArray> {
        return try {
            val body = buildJsonArray {
                addJsonObject {
                    put("query", query)
                    put("variables", variables)
                }
            }
            val response = httpClient.post(GQL_ENDPOINT) {
                firstPartyHeaders(profile, token, integrityToken)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(JsonArray.serializer(), body))
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Napier.w(
                    "GQL ${profile.label} HTTP ${response.status.value}: $text",
                    tag = TAG
                )
                return Result.Error(Exception("HTTP ${response.status.value}"))
            }
            Result.Success(json.parseToJsonElement(text).jsonArray)
        } catch (e: Exception) {
            Napier.w("GQL ${profile.label} query failed: ${e.message}", tag = TAG)
            Result.Error(e)
        }
    }

    private suspend fun integrityToken(profile: GqlClientProfile, token: String): String? {
        return try {
            val response = httpClient.post(INTEGRITY_ENDPOINT) {
                firstPartyHeaders(profile, token, null)
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                Napier.w("GQL integrity HTTP ${response.status.value}: $text", tag = TAG)
                return null
            }
            json.parseToJsonElement(text).jsonObject["token"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Napier.w("GQL integrity request failed: ${e.message}", tag = TAG)
            null
        }
    }

    suspend fun sendGifMessage(
        channelId: String,
        gifId: String,
        gifUrl: String,
        token: String
    ): Result<Unit> {
        if (channelId.isBlank() || gifId.isBlank() || gifUrl.isBlank()) {
            return Result.Error(Exception("Invalid GIF payload"))
        }
        val variables = buildJsonObject {
            putJsonObject("input") {
                put("channelID", channelId)
                put("gifID", gifId)
                put("gifURL", gifUrl)
            }
        }

        var lastError: Result.Error? = null
        for (profile in GIF_SEND_PROFILES) {
            val integrity = if (profile.useIntegrity) {
                integrityToken(profile, token) ?: continue
            } else {
                null
            }
            val r = inlineProfileQuery(
                query = SEND_GIF_MUTATION,
                token = token,
                variables = variables,
                profile = profile,
                integrityToken = integrity
            )
            if (r is Result.Error) {
                lastError = r
                continue
            }
            val node = (r as Result.Success).data.firstDataNode()
            if (node.isIntegrityRejection()) {
                Napier.w("sendGifMessage: ${profile.label} rejected by the integrity gate", tag = TAG)
                lastError = Result.Error(SendGifException(INTEGRITY_FAILED_CODE))
                continue
            }
            node?.gqlErrorOrNull()?.let { return Result.Error(Exception(it)) }
            val payload = node?.get("data")?.jsonObject?.get("sendGifMessage")?.jsonObject
                ?: return Result.Error(Exception("Empty sendGifMessage response"))
            Napier.d("sendGifMessage: ${profile.label} passed the integrity gate", tag = TAG)
            val error = payload["error"]
            if (error != null && error !is kotlinx.serialization.json.JsonNull) {
                val code = error.jsonPrimitive.contentOrNull ?: "SEND_FAILED"
                return Result.Error(SendGifException(code))
            }
            return Result.Success(Unit)
        }
        return lastError ?: Result.Error(SendGifException(INTEGRITY_FAILED_CODE))
    }

    private fun kotlinx.serialization.json.JsonObject?.isIntegrityRejection(): Boolean {
        val errors = this?.get("errors")?.jsonArray ?: return false
        return errors.any { entry ->
            val obj = entry as? JsonObject ?: return@any false
            val code = obj["extensions"]?.jsonObject?.get("code")?.jsonPrimitive?.contentOrNull
            val message = obj["message"]?.jsonPrimitive?.contentOrNull
            code == "IntegrityCheckFailed" || message?.contains("integrity", ignoreCase = true) == true
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

    suspend fun getUserDossier(
        login: String,
        channelId: String,
        token: String
    ): GqlUserDossier? {
        if (login.isBlank()) return null
        val relationship = if (channelId.isNotBlank()) {
            "relationship(targetUserID: \$channelID) { followedAt subscriptionBenefit { tier } }"
        } else ""

        val query = """
            query ChatoneUserDossier(${'$'}login: String!${if (relationship.isNotEmpty()) ", \$channelID: ID!" else ""}) {
              user(login: ${'$'}login) {
                id login displayName createdAt description language
                primaryTeam { name }
                roles { isPartner isAffiliate isStaff }
                followers { totalCount }
                videos(first: 1) { edges { node { id } } }
                stream { viewersCount createdAt game { name } }
                lastBroadcast { startedAt game { name } }
                $relationship
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("login", login.lowercase())
            if (relationship.isNotEmpty()) put("channelID", channelId)
        }

        val r = inlineQuery(query, token, variables)
        if (r !is Result.Success) return null
        val user = r.data.firstDataNode()
            ?.get("data")?.asObjectOrNull()
            ?.get("user")?.asObjectOrNull() ?: return null

        val id = user["id"]?.jsonPrimitive?.contentOrNull ?: return null
        val roles = user["roles"]?.asObjectOrNull()
        val stream = user["stream"]?.asObjectOrNull()
        val lastBroadcast = user["lastBroadcast"]?.asObjectOrNull()
        val rel = user["relationship"]?.asObjectOrNull()

        return GqlUserDossier(
            userId = id,
            login = user["login"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            displayName = user["displayName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            createdAtEpochMs = parseIsoInstantOrNull(user["createdAt"]?.jsonPrimitive?.contentOrNull),
            bio = user["description"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            language = user["language"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            teamName = user["primaryTeam"]?.asObjectOrNull()
                ?.get("name")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() },
            followerCount = user["followers"]?.asObjectOrNull()
                ?.get("totalCount")?.jsonPrimitive?.intOrNull,
            isPartner = roles?.get("isPartner")?.jsonPrimitive?.booleanOrNull == true,
            isAffiliate = roles?.get("isAffiliate")?.jsonPrimitive?.booleanOrNull == true,
            isStaff = roles?.get("isStaff")?.jsonPrimitive?.booleanOrNull == true,
            hasVideos = user["videos"]?.asObjectOrNull()
                ?.get("edges")?.asArrayOrNull()?.isNotEmpty() == true,
            liveViewers = stream?.get("viewersCount")?.jsonPrimitive?.intOrNull,
            liveGame = stream?.get("game")?.asObjectOrNull()
                ?.get("name")?.jsonPrimitive?.contentOrNull,
            liveSinceEpochMs = parseIsoInstantOrNull(stream?.get("createdAt")?.jsonPrimitive?.contentOrNull),
            lastBroadcastEpochMs = parseIsoInstantOrNull(
                lastBroadcast?.get("startedAt")?.jsonPrimitive?.contentOrNull
            ),
            lastBroadcastGame = lastBroadcast?.get("game")?.asObjectOrNull()
                ?.get("name")?.jsonPrimitive?.contentOrNull,
            followedChannelAtEpochMs = parseIsoInstantOrNull(
                rel?.get("followedAt")?.jsonPrimitive?.contentOrNull
            ),
            subscriptionTier = rel?.get("subscriptionBenefit")?.asObjectOrNull()
                ?.get("tier")?.jsonPrimitive?.contentOrNull
        )
    }

    suspend fun getUserDisplayBadges(
        login: String,
        channelLogin: String,
        token: String
    ): List<GqlDisplayBadge> {
        if (login.isBlank()) return emptyList()
        val scoped = channelLogin.isNotBlank()
        val badgesField =
            if (scoped) "displayBadges(channelLogin: \$channelLogin)" else "displayBadges"

        val query = """
            query ChatoneUserBadges(${'$'}login: String!${if (scoped) ", \$channelLogin: String!" else ""}) {
              user(login: ${'$'}login) {
                id
                $badgesField { setID version title description imageURL }
              }
            }
        """.trimIndent()

        val variables = buildJsonObject {
            put("login", login.lowercase())
            if (scoped) put("channelLogin", channelLogin.lowercase())
        }

        val r = inlineQuery(query, token, variables)
        if (r !is Result.Success) return emptyList()
        val badges = r.data.firstDataNode()
            ?.get("data")?.asObjectOrNull()
            ?.get("user")?.asObjectOrNull()
            ?.get("displayBadges")?.asArrayOrNull() ?: return emptyList()

        return badges.mapNotNull { element ->
            val node = element.asObjectOrNull() ?: return@mapNotNull null
            val imageUrl = node["imageURL"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val setId = node["setID"]?.jsonPrimitive?.contentOrNull.orEmpty()
            GqlDisplayBadge(
                setId = setId,
                version = node["version"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                title = node["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: setId,
                description = node["description"]?.jsonPrimitive?.contentOrNull
                    ?.takeIf { it.isNotBlank() },
                imageUrl = upscaleBadgeUrl(imageUrl)
            )
        }
    }

    private fun upscaleBadgeUrl(url: String): String =
        if (url.endsWith("/1")) url.dropLast(1) + "2" else url

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
        private const val CLIENT_VERSION = "ef928475-9403-42f2-8a34-55784bd08e16"
        private const val INTEGRITY_ENDPOINT = "https://gql.twitch.tv/integrity"
        private const val INTEGRITY_FAILED_CODE = "INTEGRITY_CHECK_FAILED"
        private const val ANDROID_CLIENT_ID = "kd1unb4b3q4t58fwlpcbzcbnm76a8fp"
        private const val IOS_CLIENT_ID = "851cqzxpb9bqu9z6galvkkzsc5dy24"
        private const val ANDROID_USER_AGENT = "tv.twitch.android.app/24.2.0/2402000 (Linux; U; Android 13)"
        private const val IOS_USER_AGENT = "Twitch/1400 CFNetwork/1494.0.7 Darwin/23.4.0"
        private val GIF_SEND_PROFILES = listOf(
            GqlClientProfile("android", ANDROID_CLIENT_ID, ANDROID_USER_AGENT),
            GqlClientProfile("ios", IOS_CLIENT_ID, IOS_USER_AGENT),
            GqlClientProfile("tv", TwitchFirstPartyClient.TV.clientId, TwitchFirstPartyClient.TV.userAgent),
            GqlClientProfile(
                "web",
                TwitchFirstPartyClient.WEB.clientId,
                TwitchFirstPartyClient.WEB.userAgent,
                webHeaders = true,
                useIntegrity = true
            )
        )
        private const val SEND_GIF_MUTATION =
            "mutation ChatoneSendGif(\$input: SendGifMessageInput!) { " +
                    "sendGifMessage(input: \$input) { error } }"
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
        private const val TARGETED_ACTIONS_MAX_PAGES = 5
        private const val TARGETED_MOD_ACTIONS_QUERY =
            "query ChatoneTargetedModActions(\$channelID: ID!, \$targetID: ID!, \$after: Cursor) { " +
                    "viewerCardModLogs(channelID: \$channelID, targetID: \$targetID) { " +
                    "targetedActions(first: 100, after: \$after) { " +
                    "... on ModLogsTargetedActionsConnection { " +
                    "edges { cursor node { id type timestamp } } pageInfo { hasNextPage } } " +
                    "... on ModLogsTargetedActionsError { code } } } }"
        private const val CHANNEL_REWARD_TITLES_QUERY =
            "query ChatoneRewardTitles(\$login: String!) { channel(name: \$login) { " +
                    "communityPointsSettings { customRewards { id title } } } }"
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
        private val UNLOCK_RANDOM_SUB_EMOTE_MUTATION = """
            mutation ChatoneUnlockRandomSubEmote(${'$'}input: UnlockRandomSubscriberEmoteInput!) {
              unlockRandomSubscriberEmote(input: ${'$'}input) {
                error { code }
                emote { id token }
              }
            }
        """.trimIndent()
        private const val CHANNEL_POINTS_CONTEXT_HASH =
            "7fe050e3761eb2cf258d70ee1a21cbd76fa8cf3d7e7b12fc437e7029d446b5e3"
        private const val REDEEM_CUSTOM_REWARD_HASH =
            "d56249a7adb4978898ea3412e196688d4ac3cea1c0c2dfd65561d229ea5dcc42"
    }
}

private data class GqlClientProfile(
    val label: String,
    val clientId: String,
    val userAgent: String,
    val webHeaders: Boolean = false,
    val useIntegrity: Boolean = false
)

class SendGifException(val code: String) : Exception(code)
