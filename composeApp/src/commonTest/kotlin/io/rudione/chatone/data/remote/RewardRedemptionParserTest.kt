package io.rudione.chatone.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RewardRedemptionParserTest {

    private val pubSubRedeemed = """
        {"type":"reward-redeemed","data":{"timestamp":"2026-11-05T20:00:00Z","redemption":{
          "id":"red-1","channel_id":"42","redeemed_at":"2026-11-05T20:00:00.123Z",
          "user":{"id":"77","login":"viewer","display_name":"Viewer"},
          "reward":{"id":"rw-1","channel_id":"42","title":"Hydrate","cost":500,"is_user_input_required":false,
            "image":null,"default_image":{"url_1x":"https://x/1.png","url_2x":"https://x/2.png","url_4x":"https://x/4.png"},
            "background_color":"#00C7AC"},
          "status":"UNFULFILLED"}}}
    """.trimIndent()

    @Test
    fun pubSubRedemptionWithoutTextIsParsed() {
        val r = assertNotNull(RewardRedemptionParser.fromPubSub(pubSubRedeemed))
        assertEquals("red-1", r.id)
        assertEquals("42", r.channelId)
        assertEquals("rw-1", r.rewardId)
        assertEquals("Hydrate", r.rewardTitle)
        assertEquals(500L, r.cost)
        assertEquals("viewer", r.login)
        assertEquals("Viewer", r.displayName)
        assertEquals("https://x/4.png", r.imageUrl)
        assertEquals(0xFF00C7ACL, r.backgroundColor)
        assertFalse(r.carriesChatMessage)
    }

    @Test
    fun pubSubRedemptionWithTextCarriesChatMessage() {
        val withText = pubSubRedeemed.replace("\"status\"", "\"user_input\":\"play song\",\"status\"")
        val r = assertNotNull(RewardRedemptionParser.fromPubSub(withText))
        assertEquals("play song", r.userInput)
        assertTrue(r.carriesChatMessage)
    }

    @Test
    fun otherPubSubTypesAreIgnored() {
        assertNull(RewardRedemptionParser.fromPubSub("""{"type":"custom-reward-updated","data":{}}"""))
        assertNull(RewardRedemptionParser.fromPubSub("not json"))
    }

    @Test
    fun eventSubRedemptionIsParsed() {
        val event = Json.parseToJsonElement(
            """
            {"id":"red-2","broadcaster_user_id":"42","broadcaster_user_login":"streamer","user_id":"77",
             "user_login":"viewer","user_name":"Viewer","user_input":"","status":"unfulfilled",
             "reward":{"id":"rw-2","title":"Stretch","cost":1000,"prompt":""},"redeemed_at":"2026-11-05T20:00:00Z"}
            """.trimIndent()
        ).jsonObject
        val r = assertNotNull(RewardRedemptionParser.fromEventSub(event))
        assertEquals("red-2", r.id)
        assertEquals("42", r.channelId)
        assertEquals("Stretch", r.rewardTitle)
        assertEquals(1000L, r.cost)
        assertNull(r.userInput)
        assertFalse(r.carriesChatMessage)
    }
}
