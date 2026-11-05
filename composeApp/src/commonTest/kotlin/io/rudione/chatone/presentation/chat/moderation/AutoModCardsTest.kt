package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.data.remote.AutoModProfile
import io.rudione.chatone.data.remote.GqlModLogSummary
import io.rudione.chatone.data.remote.ModLogActionType
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.DisplayMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutoModCardsTest {

    private fun preview(color: String? = null, badges: List<Badge> = emptyList()) = DisplayMessage.PrivMsg(
        id = "p", timestamp = 0L, channel = "streamer", userId = "77", username = "viewer", displayName = "Viewer",
        tokens = emptyList(), color = color, badges = badges, isModerator = false, isSubscriber = false,
        isVip = false, isBroadcaster = false, isMention = false, isAction = false
    )

    private fun card(preview: DisplayMessage.PrivMsg?, color: String? = null) = DisplayMessage.AutoModMsg(
        id = "automod_1", timestamp = 0L, channel = "#streamer", msgId = "1", userId = "77",
        username = "viewer", displayName = "Viewer", text = "hi", color = color, preview = preview
    )

    private val gqlBadge = Badge(id = "vip", version = "1", imageUrl = "https://b/2", setId = "vip")
    private val profile = AutoModProfile(
        chatColor = "#D2691E", badges = listOf(gqlBadge), createdAtMs = 10L,
        followedAtMs = 20L, followKnown = true, subscriptionTier = "2000"
    )

    @Test
    fun missingColorAndBadgesComeFromTwitch() {
        val enriched = AutoModCards.enrich(card(preview()), profile, null)
        assertEquals("#D2691E", enriched.color)
        assertEquals("#D2691E", enriched.preview?.color)
        assertEquals(listOf(gqlBadge), enriched.preview?.badges)
        assertEquals(2, enriched.context?.subscriptionTier)
        assertNull(enriched.context?.timeouts)
    }

    @Test
    fun chatDataWinsOverTwitchProfile() {
        val chatBadge = Badge(id = "subscriber", version = "12", imageUrl = "https://sub")
        val enriched = AutoModCards.enrich(card(preview("#FF0000", listOf(chatBadge)), color = "#FF0000"), profile, null)
        assertEquals("#FF0000", enriched.preview?.color)
        assertEquals(listOf(chatBadge), enriched.preview?.badges)
    }

    @Test
    fun modLogCountsFillTheContext() {
        val logs = GqlModLogSummary(
            counts = mapOf(ModLogActionType.TIMEOUT to 3, ModLogActionType.BAN to 1),
            messageCount = 0,
            actions = emptyList()
        )
        val context = AutoModCards.enrich(card(preview()), null, logs).context
        assertEquals(3, context?.timeouts)
        assertEquals(1, context?.bans)
        assertEquals(0, context?.messages)
    }
}
