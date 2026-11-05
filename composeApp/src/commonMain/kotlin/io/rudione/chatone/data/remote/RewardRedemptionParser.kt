package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.points.RewardRedemption
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock
import kotlin.time.Instant

internal object RewardRedemptionParser {

    const val PUBSUB_REDEEMED = "reward-redeemed"
    const val EVENTSUB_REDEMPTION_ADD = "channel.channel_points_custom_reward_redemption.add"

    private val json = Json { ignoreUnknownKeys = true }

    fun fromPubSub(message: String): RewardRedemption? {
        val root = runCatching { json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return null
        if (root.text("type") != PUBSUB_REDEEMED) return null
        val redemption = root.obj("data")?.obj("redemption") ?: return null
        val user = redemption.obj("user") ?: return null
        val reward = redemption.obj("reward") ?: return null
        val login = user.text("login") ?: return null
        return RewardRedemption(
            id = redemption.text("id") ?: return null,
            channelId = redemption.text("channel_id") ?: reward.text("channel_id").orEmpty(),
            rewardId = reward.text("id").orEmpty(),
            rewardTitle = reward.text("title").orEmpty(),
            cost = reward.text("cost")?.toLongOrNull() ?: 0L,
            userId = user.text("id").orEmpty(),
            login = login,
            displayName = user.text("display_name") ?: login,
            userInput = redemption.text("user_input"),
            redeemedAtMs = epochMs(redemption.text("redeemed_at")),
            imageUrl = imageUrl(reward.obj("image")) ?: imageUrl(reward.obj("default_image")),
            backgroundColor = hexColor(reward.text("background_color"))
        )
    }

    fun fromEventSub(event: JsonObject): RewardRedemption? {
        val reward = event.obj("reward") ?: return null
        val login = event.text("user_login") ?: return null
        return RewardRedemption(
            id = event.text("id") ?: return null,
            channelId = event.text("broadcaster_user_id").orEmpty(),
            rewardId = reward.text("id").orEmpty(),
            rewardTitle = reward.text("title").orEmpty(),
            cost = reward.text("cost")?.toLongOrNull() ?: 0L,
            userId = event.text("user_id").orEmpty(),
            login = login,
            displayName = event.text("user_name") ?: login,
            userInput = event.text("user_input"),
            redeemedAtMs = epochMs(event.text("redeemed_at"))
        )
    }

    private fun imageUrl(image: JsonObject?): String? =
        image?.text("url_4x") ?: image?.text("url_2x") ?: image?.text("url_1x")

    private fun hexColor(raw: String?): Long? {
        val hex = raw?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
        return hex.toLongOrNull(16)?.let { 0xFF000000 or it }
    }

    private fun epochMs(iso: String?): Long =
        iso?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
            ?: Clock.System.now().toEpochMilliseconds()

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
