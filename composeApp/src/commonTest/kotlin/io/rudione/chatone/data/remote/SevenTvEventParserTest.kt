package io.rudione.chatone.data.remote

import io.rudione.chatone.data.remote.emote.SevenTvEventParser
import io.rudione.chatone.data.remote.emote.SevenTvFrame
import io.rudione.chatone.data.remote.emote.SevenTvLiveEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SevenTvEventParserTest {

    private val parser = SevenTvEventParser()

    private fun dispatch(text: String): List<SevenTvLiveEvent> = assertIs<SevenTvFrame.Dispatch>(parser.parse(text)).events

    private fun emoteEntry(id: String, alias: String, original: String, entryFlags: Int = 0, dataFlags: Int = 0) = """
        {"id":"$id","name":"$alias","flags":$entryFlags,"timestamp":1700000000000,"actor_id":"actor",
         "data":{"id":"$id","name":"$original","flags":$dataFlags,"lifecycle":3,"listed":true,"animated":true,
           "owner":{"id":"owner","username":"maker","display_name":"Maker"},
           "host":{"url":"//cdn.7tv.app/emote/$id","files":[
             {"name":"1x.avif","format":"AVIF","width":32,"height":32},
             {"name":"1x.webp","format":"WEBP","width":32,"height":32},
             {"name":"2x.webp","format":"WEBP","width":64,"height":64},
             {"name":"4x.webp","format":"WEBP","width":128,"height":128}]}},
         "origin_id":null}
    """.trimIndent()

    @Test
    fun helloCarriesHeartbeatAndSubscriptionLimit() {
        val frame = parser.parse(
            """{"d":{"heartbeat_interval":46415,"session_id":"01M7GDEJ8QPWD7ZBVF9ZYAPW7K","subscription_limit":500,"instance":{"name":"event-api-5","population":140758}},"op":1,"t":1794773567771,"s":0}"""
        )
        assertEquals(SevenTvFrame.Hello(heartbeatIntervalMs = 46_415, subscriptionLimit = 500), frame)
    }

    @Test
    fun serverFramesAreRecognised() {
        assertEquals(SevenTvFrame.Heartbeat, parser.parse("""{"d":{"count":3},"op":2,"t":1,"s":1}"""))
        assertEquals(SevenTvFrame.Reconnect, parser.parse("""{"op":4,"d":{}}"""))
        assertEquals(
            SevenTvFrame.EndOfStream(4002, "Invalid Payload"),
            parser.parse("""{"d":{"code":4002,"message":"Invalid Payload"},"op":7,"t":1794773569921,"s":2}""")
        )
        assertEquals(
            SevenTvFrame.Failure("Not subscribed to this event"),
            parser.parse("""{"d":{"message":"Not subscribed to this event","fields":{}},"op":6,"t":1,"s":4}""")
        )
        assertEquals(
            SevenTvFrame.Ignored,
            parser.parse("""{"d":{"command":"SUBSCRIBE","data":{"id":2,"type":"emote_set.update","condition":{"object_id":"x"}}},"op":5,"t":1,"s":2}""")
        )
        assertNull(parser.parse("not json"))
    }

    @Test
    fun addedEmoteKeepsTheChannelAliasAndRealImageData() {
        val events = dispatch(
            """{"op":0,"t":1,"d":{"type":"emote_set.update","body":{"id":"setA","kind":3,
               "actor":{"id":"a1","username":"modguy","display_name":"ModGuy"},
               "pushed":[{"key":"emotes","index":10,"type":"","value":${emoteEntry("e1", "jam", "catJAM", entryFlags = 1 shl 8)}}]}}}"""
        )
        val added = assertIs<SevenTvLiveEvent.EmoteAdded>(events.single())
        assertEquals("setA", added.emoteSetId)
        assertEquals("ModGuy", added.actorName)
        assertEquals("jam", added.emote.code)
        assertEquals("catJAM", added.emote.originalName)
        assertTrue(added.emote.isZeroWidth)
        assertEquals(64, added.emote.width)
        assertEquals("https://cdn.7tv.app/emote/e1/2x.webp", added.emote.url2x)
        assertEquals("https://cdn.7tv.app/emote/e1/4x.webp", added.emote.url3x)
    }

    @Test
    fun addedEmoteWithoutImageDataFallsBackToTheCdn() {
        val events = dispatch(
            """{"op":0,"d":{"type":"emote_set.update","body":{"id":"setA",
               "pushed":[{"key":"emotes","value":{"id":"e9","name":"Short","flags":0}}]}}}"""
        )
        val added = assertIs<SevenTvLiveEvent.EmoteAdded>(events.single())
        assertEquals("Short", added.emote.code)
        assertEquals("https://cdn.7tv.app/emote/e9/1x.webp", added.emote.url1x)
        assertEquals("", added.actorName)
    }

    @Test
    fun removalsComeBeforeRenamesAndAdditions() {
        val events = dispatch(
            """{"op":0,"d":{"type":"emote_set.update","body":{"id":"setA",
               "actor":{"username":"owner","display_name":""},
               "pushed":[{"key":"emotes","value":${emoteEntry("new", "PogU", "PogU")}}],
               "updated":[{"key":"emotes","old_value":{"id":"r1","name":"old"},"value":{"id":"r1","name":"fresh"}},
                          {"key":"emotes","old_value":{"id":"r2","name":"same"},"value":{"id":"r2","name":"same"}}],
               "pulled":[{"key":"emotes","old_value":{"id":"gone","name":"PogU"}}]}}}"""
        )
        assertEquals(3, events.size)
        assertEquals(SevenTvLiveEvent.EmoteRemoved("setA", "gone", "PogU", "owner"), events[0])
        assertEquals(SevenTvLiveEvent.EmoteRenamed("setA", "r1", "old", "fresh", "owner"), events[1])
        assertIs<SevenTvLiveEvent.EmoteAdded>(events[2])
    }

    @Test
    fun nonEmoteFieldsDoNotBreakTheDispatch() {
        val events = dispatch(
            """{"op":0,"d":{"type":"emote_set.update","body":{"id":"setA",
               "updated":[{"key":"name","old_value":"Old set","value":"New set"},
                          {"key":"capacity","old_value":600,"value":1000},
                          {"key":"emotes","old_value":{"id":"r1","name":"a"},"value":{"id":"r1","name":"b"}}],
               "pushed":[{"key":"flags","value":4}]}}}"""
        )
        assertEquals(listOf<SevenTvLiveEvent>(SevenTvLiveEvent.EmoteRenamed("setA", "r1", "a", "b", "")), events)
    }

    @Test
    fun switchingTheActiveSetIsReportedForTheUser() {
        val events = dispatch(
            """{"op":0,"d":{"type":"user.update","body":{"id":"user7tv","kind":1,
               "actor":{"username":"streamer","display_name":"Streamer"},
               "updated":[{"key":"connections","index":0,"nested":true,"value":[
                 {"key":"emote_set","old_value":{"id":"setA","name":"Old"},"value":{"id":"setB","name":"Event"}},
                 {"key":"emote_set_id","old_value":"setA","value":"setB"}]}]}}}"""
        )
        assertEquals(listOf<SevenTvLiveEvent>(SevenTvLiveEvent.UserEmoteSetChanged("user7tv", "Streamer")), events)
    }

    @Test
    fun otherUserChangesAreIgnored() {
        val events = dispatch(
            """{"op":0,"d":{"type":"user.update","body":{"id":"user7tv",
               "updated":[{"key":"connections","index":0,"value":[{"key":"display_name","old_value":"a","value":"b"}]},
                          {"key":"avatar_url","old_value":"x","value":"y"}]}}}"""
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun personalSetEntitlementsTargetTwitchConnections() {
        val body = """"body":{"id":"ent","object":{"id":"ent","kind":"EMOTE_SET","ref_id":"personalSet",
            "user":{"id":"u7","username":"viewer","connections":[
              {"id":"1001","platform":"TWITCH","username":"viewer"},
              {"id":"55","platform":"KICK","username":"viewer"}]}}}"""
        assertEquals(
            listOf<SevenTvLiveEvent>(SevenTvLiveEvent.PersonalEmoteSetGranted("1001", "personalSet")),
            dispatch("""{"op":0,"d":{"type":"entitlement.create",$body}}""")
        )
        assertEquals(
            listOf<SevenTvLiveEvent>(SevenTvLiveEvent.PersonalEmoteSetRevoked("1001", "personalSet")),
            dispatch("""{"op":0,"d":{"type":"entitlement.delete",$body}}""")
        )
        assertTrue(
            dispatch("""{"op":0,"d":{"type":"entitlement.create","body":{"object":{"kind":"BADGE","ref_id":"b"}}}}""").isEmpty()
        )
    }
}
