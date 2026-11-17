package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object CommunityPointsParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(message: String): IrcEvent? {
        val root = runCatching { json.parseToJsonElement(message) as? JsonObject }.getOrNull()
            ?: return null
        val type = root.string("type") ?: return null
        val data = root["data"] as? JsonObject ?: return null
        return when (type) {
            "claim-available" -> claimAvailable(data)
            "claim-claimed" -> claimClaimed(data)
            "points-earned" -> pointsEarned(data)
            "points-spent" -> pointsSpent(data)
            else -> null
        }
    }

    private fun claimAvailable(data: JsonObject): IrcEvent? {
        val claim = data["claim"] as? JsonObject ?: return null
        val claimId = claim.string("id") ?: return null
        val channelId = claim.string("channel_id") ?: return null
        val points = (claim["point_gain"] as? JsonObject)?.int("total_points") ?: 0
        return IrcEvent.PointsClaimAvailable(channelId, claimId, points)
    }

    private fun claimClaimed(data: JsonObject): IrcEvent? {
        val claim = data["claim"] as? JsonObject ?: return null
        val claimId = claim.string("id") ?: return null
        val channelId = claim.string("channel_id") ?: return null
        return IrcEvent.PointsClaimed(channelId, claimId)
    }

    private fun pointsEarned(data: JsonObject): IrcEvent? {
        val gain = data["point_gain"] as? JsonObject ?: return null
        val balanceObj = data["balance"] as? JsonObject
        val channelId = data.string("channel_id")
            ?: gain.string("channel_id")
            ?: balanceObj?.string("channel_id")
            ?: return null
        val amount = gain.int("total_points") ?: return null
        return IrcEvent.PointsEarned(
            channelId = channelId,
            amount = amount,
            reason = gain.string("reason_code").orEmpty(),
            balance = balanceObj?.long("balance")
        )
    }

    private fun pointsSpent(data: JsonObject): IrcEvent? {
        val balanceObj = data["balance"] as? JsonObject ?: return null
        val channelId = balanceObj.string("channel_id") ?: return null
        val balance = balanceObj.long("balance") ?: return null
        return IrcEvent.PointsBalanceChanged(channelId, balance)
    }

    private fun JsonObject.string(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull }.getOrNull()
            ?.takeIf { it.isNotBlank() }

    private fun JsonObject.int(key: String): Int? =
        runCatching { this[key]?.jsonPrimitive?.intOrNull }.getOrNull()

    private fun JsonObject.long(key: String): Long? =
        runCatching { this[key]?.jsonPrimitive?.contentOrNull?.toLongOrNull() }.getOrNull()
}
