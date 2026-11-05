package io.rudione.chatone.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoModMessagePartsTest {

    @Test
    fun pubSubEmoticonFragmentsBecomeTwitchEmotes() {
        val content = Json.parseToJsonElement(
            """
            {"text":"hi Kappa you Kappa","fragments":[
              {"text":"hi "},
              {"text":"Kappa","emoticon":{"emoticonID":"25","emoticonSetID":"0"}},
              {"text":" you "},
              {"text":"Kappa","emoticon":{"emoticonID":"25","emoticonSetID":"0"}}
            ]}
            """.trimIndent()
        ).jsonObject
        val text = "hi Kappa you Kappa"
        val emotes = AutoModMessageParts.twitchEmotes(AutoModMessageParts.pubSubFragments(content), text)
        assertEquals(1, emotes.size)
        assertEquals("25", emotes.single().id)
        assertEquals("Kappa", emotes.single().name)
        assertEquals(listOf(3 to 7, 13 to 17), emotes.single().positions.map { it.start to it.end })
    }

    @Test
    fun eventSubEmoteFragmentsAreRecognised() {
        val message = Json.parseToJsonElement(
            """
            {"text":"🙂 LUL","fragments":[
              {"type":"text","text":"🙂 "},
              {"type":"emote","text":"LUL","emote":{"id":"425618","emote_set_id":"0"}}
            ]}
            """.trimIndent()
        ).jsonObject
        val text = "🙂 LUL"
        val emote = AutoModMessageParts.twitchEmotes(AutoModMessageParts.eventSubFragments(message), text).single()
        assertEquals("425618", emote.id)
        assertEquals("LUL", text.substring(emote.positions.single().start, emote.positions.single().end + 1))
    }

    @Test
    fun senderBadgesAreParsed() {
        val sender = Json.parseToJsonElement(
            """{"login":"viewer","badges":[{"id":"subscriber","version":"12"},{"id":"vip","version":"1"}]}"""
        ).jsonObject
        assertEquals(listOf("subscriber/12", "vip/1"), AutoModMessageParts.pubSubBadges(sender).map { "${it.id}/${it.version}" })
    }
}
