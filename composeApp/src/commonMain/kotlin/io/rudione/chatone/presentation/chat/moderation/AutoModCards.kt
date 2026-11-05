package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.data.remote.AutoModProfile
import io.rudione.chatone.data.remote.GqlModLogSummary
import io.rudione.chatone.data.remote.ModLogActionType
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.moderation.AutoModContext
import io.rudione.chatone.domain.moderation.AutoModInsights

object AutoModCards {

    fun enrich(
        card: DisplayMessage.AutoModMsg,
        profile: AutoModProfile?,
        logs: GqlModLogSummary?
    ): DisplayMessage.AutoModMsg {
        val color = card.color ?: profile?.chatColor
        val preview = card.preview?.let { shown ->
            shown.copy(
                color = shown.color ?: profile?.chatColor,
                badges = if (shown.badges.any { it.imageUrl.isNotEmpty() }) shown.badges else profile?.badges.orEmpty()
            )
        }
        val context = AutoModContext(
            accountCreatedAtMs = profile?.createdAtMs,
            followedAtMs = profile?.followedAtMs,
            followKnown = profile?.followKnown == true,
            subscriptionTier = AutoModInsights.tierOf(profile?.subscriptionTier),
            timeouts = logs?.counts?.get(ModLogActionType.TIMEOUT),
            bans = logs?.counts?.get(ModLogActionType.BAN),
            messages = logs?.messageCount
        )
        return card.copy(color = color, preview = preview, context = context)
    }
}
