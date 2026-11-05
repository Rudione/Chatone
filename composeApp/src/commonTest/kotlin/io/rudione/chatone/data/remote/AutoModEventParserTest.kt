package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutoModEventParserTest {

    private fun json(raw: String): JsonObject = Json.parseToJsonElement(raw).jsonObject

    @Test
    fun automodHoldCarriesCategoryLevelAndFlaggedWords() {
        val held = assertNotNull(
            AutoModEventParser.hold(
                json(
                    """
                    {"broadcaster_user_id":"1337","broadcaster_user_login":"blah","broadcaster_user_name":"Blah",
                     "user_id":"4242","user_login":"baduser","user_name":"BadUser","message_id":"m1",
                     "message":{"text":"you are an idiot","fragments":[{"type":"text","text":"you are an idiot"}]},
                     "reason":"automod","automod":{"category":"aggressive","level":2,"boundaries":[{"start_pos":11,"end_pos":15}]},
                     "blocked_term":null,"held_at":"2022-12-02T15:00:00.00Z"}
                    """.trimIndent()
                )
            )
        )
        assertEquals("1337", held.channel)
        assertEquals("blah", held.channelLogin)
        assertEquals("baduser", held.username)
        assertEquals("BadUser", held.displayName)
        assertEquals("aggressive", held.reasonCategory)
        assertEquals(2, held.reasonLevel)
        assertEquals(listOf("idiot"), held.flaggedFragments)
        assertEquals(1669993200000L, held.heldAtMs)
    }

    @Test
    fun blockedTermHoldIsShownWithItsTerm() {
        val held = assertNotNull(
            AutoModEventParser.hold(
                json(
                    """
                    {"broadcaster_user_id":"1","broadcaster_user_login":"rudionee","user_id":"2","user_login":"7w3lw3",
                     "user_name":"7w3lw3","message_id":"m2","message":{"text":"Lorem ipsum dolor","fragments":[]},
                     "reason":"blocked_term","automod":null,
                     "blocked_term":{"terms_found":[{"term_id":"t","boundary":{"start_pos":6,"end_pos":10},
                       "owner_broadcaster_user_id":"1","owner_broadcaster_user_login":"rudionee","owner_broadcaster_user_name":"Rudionee"}]},
                     "held_at":"2026-11-06T00:02:00Z"}
                    """.trimIndent()
                )
            )
        )
        assertEquals("blocked_term", held.reasonCategory)
        assertNull(held.reasonLevel)
        assertEquals(listOf("ipsum"), held.flaggedFragments)
    }

    @Test
    fun updatesMapEveryStatus() {
        fun update(status: String) = AutoModEventParser.update(
            json(
                """{"broadcaster_user_id":"1","broadcaster_user_login":"rudionee","user_login":"7w3lw3",
                   "moderator_user_login":"mod","message_id":"m2","status":"$status"}"""
            )
        )
        assertEquals(IrcEvent.AutoModResolved.ALLOWED, update("Approved")?.action)
        assertEquals(IrcEvent.AutoModResolved.DENIED, update("denied")?.action)
        assertEquals(IrcEvent.AutoModResolved.EXPIRED, update("Expired")?.action)
        assertEquals("7w3lw3", update("approved")?.authorLogin)
        assertEquals("mod", update("approved")?.resolvedBy)
        assertNull(update("pending"))
    }

    @Test
    fun boundariesCountCodePoints() {
        assertEquals("bad", AutoModEventParser.codePointSlice("🙂 bad", 2, 4))
        assertNull(AutoModEventParser.codePointSlice("short", 9, 12))
    }
}
