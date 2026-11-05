package io.rudione.chatone.domain.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VanityChangesTest {

    private val twitch = TwitchVanity(
        userId = "1", login = "rudionee", displayName = "Rudionee", bio = "old", chatColorHex = "1E90FF",
        accentHex = "9147FF", links = listOf(SocialLink("a", "tg", "https://t.me/x")),
        stream = StreamSettings("title", StreamCategory("1", "Dota 2", ""), listOf("ru"), "ru"),
        globalBadges = listOf(VanityBadge("glhf-pledge", "1", "GLHF", "https://x")), selectedGlobal = null,
        channelId = null, channelName = null, channelBadges = emptyList(), selectedChannel = null
    )
    private val base = VanityDraft("1E90FF", null, null, null, null, "Rudionee", "old", "9147FF", twitch.links, twitch.stream)

    @Test
    fun noChangesForAnUntouchedDraft() {
        assertTrue(VanityChanges.of(twitch, null, base).isEmpty())
    }

    @Test
    fun describesEveryEditedField() {
        val draft = base.copy(
            colorHex = "FF0000",
            globalBadge = "glhf-pledge/1",
            bio = "new",
            links = listOf(SocialLink(null, "YouTube", "https://y")),
            stream = twitch.stream!!.copy(title = "new title", tags = listOf("ru", "dota"))
        )
        val changes = VanityChanges.of(twitch, null, draft)
        assertEquals(
            listOf(
                VanityField.NAME_COLOR, VanityField.GLOBAL_BADGE, VanityField.BIO,
                VanityField.LINK_ADDED, VanityField.LINK_REMOVED, VanityField.STREAM_TITLE, VanityField.TAGS
            ),
            changes.map { it.field }
        )
        assertEquals("GLHF", changes[1].after)
        assertEquals(
            setOf(VanityPart.COLOR, VanityPart.BADGE, VanityPart.PROFILE, VanityPart.LINKS, VanityPart.STREAM),
            VanityChanges.parts(changes)
        )
    }
}
