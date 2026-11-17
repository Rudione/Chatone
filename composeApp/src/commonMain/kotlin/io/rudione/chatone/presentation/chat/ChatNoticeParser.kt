package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.ChatGiftBundle
import io.rudione.chatone.domain.model.ChatNoticeDetails
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.ChatSubTier
import io.rudione.chatone.domain.model.ChatSubscription
import io.rudione.chatone.domain.model.IrcEvent

internal object ChatNoticeParser {
    fun parse(event: IrcEvent.UserNotice, timestamp: Long): ChatNoticeDetails {
        val tags = event.tags
        val type = event.msgId.orEmpty().lowercase()
        val raid = type == "raid"
        val streak = type == "viewermilestone" && tags["msg-param-category"] == "watch-streak"
        val login = (if (raid) tags["msg-param-login"] else null)
            ?.takeIf(String::isNotBlank) ?: tags["login"].orEmpty()
        val displayName = (if (raid) tags["msg-param-displayName"] else null)
            ?.takeIf(String::isNotBlank) ?: tags["display-name"]?.takeIf(String::isNotBlank) ?: login
        val author = if (login.isNotBlank() || displayName.isNotBlank()) ChatNoticeUser(
            tags["user-id"].orEmpty(), login, displayName, tags["color"]?.takeIf(String::isNotBlank)
        ) else null
        val summary = type == "submysterygift" || type == "anonsubmysterygift"
        val gift = summary || type == "subgift" || type == "anonsubgift"
        val recipients = if (gift && !summary) {
            val recipientLogin = tags["msg-param-recipient-user-name"]
                ?: tags["msg-param-recipient-name"].orEmpty()
            val recipientId = tags["msg-param-recipient-id"].orEmpty()
            if (recipientLogin.isNotBlank() || recipientId.isNotBlank()) listOf(
                ChatNoticeUser(
                    recipientId,
                    recipientLogin,
                    tags["msg-param-recipient-display-name"]?.takeIf(String::isNotBlank) ?: recipientLogin
                )
            ) else emptyList()
        } else emptyList()
        return ChatNoticeDetails(
            author = author,
            raidViewers = tags["msg-param-viewerCount"]?.toIntOrNull()?.coerceAtLeast(0).takeIf { raid },
            watchStreak = tags["msg-param-value"]?.toIntOrNull()?.takeIf { streak && it > 0 },
            watchStreakReward = tags["msg-param-copoReward"]?.toIntOrNull()?.takeIf { streak && it > 0 },
            gifts = if (gift) ChatGiftBundle(
                total = if (summary) tags["msg-param-mass-gift-count"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1 else 1,
                recipients = recipients,
                plan = tags["msg-param-sub-plan"].orEmpty(),
                communityId = tags["msg-param-community-gift-id"]?.takeIf { it.isNotBlank() && it != "0" },
                anonymous = type.startsWith("anon"),
                hasSummary = summary,
                eventIds = setOfNotNull(tags["id"]?.takeIf(String::isNotBlank)),
                lastTimestamp = timestamp
            ) else null,
            subscription = if (type == "sub" || type == "resub") ChatSubscription(
                tier = subTier(tags["msg-param-sub-plan"]),
                months = tags["msg-param-cumulative-months"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            ) else null
        )
    }

    private fun subTier(plan: String?): ChatSubTier = when (plan?.trim()?.lowercase()) {
        "prime" -> ChatSubTier.Prime
        "2000" -> ChatSubTier.Tier2
        "3000" -> ChatSubTier.Tier3
        else -> ChatSubTier.Tier1
    }
}
