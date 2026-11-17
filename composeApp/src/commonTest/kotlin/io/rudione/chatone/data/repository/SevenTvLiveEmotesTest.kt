package io.rudione.chatone.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.data.remote.emote.BttvApiClient
import io.rudione.chatone.data.remote.emote.FfzApiClient
import io.rudione.chatone.data.remote.emote.SevenTvApiClient
import io.rudione.chatone.data.remote.emote.SevenTvEventSource
import io.rudione.chatone.data.remote.emote.SevenTvLiveEvent
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SevenTvLiveEmotesTest {

    private class FakeSource : SevenTvEventSource {
        override val dispatches = MutableSharedFlow<List<SevenTvLiveEvent>>()
        val subscribed = mutableListOf<String>()
        override fun subscribeToEmoteSet(emoteSetId: String) { subscribed += "set:$emoteSetId" }
        override fun subscribeToPersonalSet(emoteSetId: String) { subscribed += "personal:$emoteSetId" }
        override fun subscribeToUser(sevenTvUserId: String) { subscribed += "user:$sevenTvUserId" }
        override fun subscribeToTwitchChannel(twitchChannelId: String) { subscribed += "channel:$twitchChannelId" }
    }

    private class SevenTvBackend {
        val users = mutableMapOf(
            "111" to user("setA", "Alpha set", "userA", "a" to "KEKW", "b" to "PogU"),
            "222" to user("setB", "Beta set", "userB", "c" to "catJAM"),
            "333" to user("setA", "Alpha set", "userShared", "a" to "KEKW", "b" to "PogU")
        )
        val sets = mutableMapOf(
            "personal1" to set("personal1", "p1" to "wave", "p2" to "hug"),
            "personal2" to set("personal2", "p3" to "wave")
        )

        companion object {
            fun entry(id: String, name: String) =
                """{"id":"$id","name":"$name","flags":0,"data":{"id":"$id","name":"$name","flags":0,
                   "host":{"url":"//cdn.7tv.app/emote/$id","files":[{"name":"1x.webp","format":"WEBP","width":28,"height":28},
                   {"name":"2x.webp","format":"WEBP","width":56,"height":56}]}}}"""

            fun set(id: String, vararg emotes: Pair<String, String>, name: String = id) =
                """{"id":"$id","name":"$name","emotes":[${emotes.joinToString(",") { entry(it.first, it.second) }}]}"""

            fun user(setId: String, setName: String, userId: String, vararg emotes: Pair<String, String>) =
                """{"id":"x","platform":"TWITCH","emote_set_id":"$setId","emote_set":${set(setId, *emotes, name = setName)},
                   "user":{"id":"$userId","username":"name"}}"""
        }
    }

    private val backend = SevenTvBackend()
    private val source = FakeSource()

    private val repository = EmoteRepository(
        sevenTvApi = SevenTvApiClient(client { path ->
            when {
                path.startsWith("/v3/users/twitch/") -> backend.users[path.substringAfterLast('/')]
                path.startsWith("/v3/emote-sets/") -> backend.sets[path.substringAfterLast('/')]
                else -> null
            }
        }),
        bttvApi = BttvApiClient(client { null }),
        ffzApi = FfzApiClient(client { null })
    )

    private fun client(route: (String) -> String?) = HttpClient(MockEngine { request ->
        val body = route(request.url.encodedPath)
        respond(
            content = body ?: """{"error":"not found"}""",
            status = if (body == null) HttpStatusCode.NotFound else HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json")
        )
    }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    }

    private fun emote(id: String, code: String) = GenericEmote(id, code, "1", "2", "3", EmoteProvider.SEVEN_TV)

    private fun codes(channel: String) = repository.getChannelEmotes(channel).value.sevenTvChannel.map { it.code }

    private fun TestScope.live() = SevenTvLiveEmotes(source, repository, backgroundScope)

    private suspend fun loadChannels() {
        repository.loadChannelEmotes("Alpha", "111")
        repository.loadChannelEmotes("beta", "222")
    }

    @Test
    fun watchingAChannelSubscribesItsSetUserAndEntitlements() = runTest {
        loadChannels()
        live().watchChannel("ALPHA", "111")
        assertEquals(listOf("set:setA", "user:userA", "channel:111"), source.subscribed)
    }

    @Test
    fun anEmoteAddedToOneChannelNeverLeaksIntoAnother() = runTest {
        loadChannels()
        val changes = live().handle(listOf(SevenTvLiveEvent.EmoteAdded("setB", emote("w", "Wave"), "Mod")))
        assertEquals(listOf("catJAM", "Wave"), codes("beta"))
        assertEquals(listOf("KEKW", "PogU"), codes("alpha"))
        assertEquals(
            listOf<SevenTvEmoteChange>(
                SevenTvEmoteChange.Channel("beta", listOf(SevenTvEmoteNotice.Added("Mod", "Wave")), setOf("Wave"))
            ),
            changes
        )
    }

    @Test
    fun eventsForSetsNoChannelUsesAreIgnored() = runTest {
        loadChannels()
        val changes = live().handle(
            listOf(
                SevenTvLiveEvent.EmoteAdded("someoneElse", emote("w", "Wave"), "Mod"),
                SevenTvLiveEvent.EmoteRemoved("someoneElse", "a", "KEKW", "Mod")
            )
        )
        assertTrue(changes.isEmpty())
        assertEquals(listOf("KEKW", "PogU"), codes("alpha"))
        assertEquals(listOf("catJAM"), codes("beta"))
    }

    @Test
    fun channelsSharingASetAreAllUpdated() = runTest {
        loadChannels()
        repository.loadChannelEmotes("gamma", "333")
        val changes = live().handle(listOf(SevenTvLiveEvent.EmoteRemoved("setA", "a", "KEKW", "Mod")))
        assertEquals(setOf("alpha", "gamma"), changes.map { (it as SevenTvEmoteChange.Channel).channelLogin }.toSet())
        assertEquals(listOf("PogU"), codes("alpha"))
        assertEquals(listOf("PogU"), codes("gamma"))
    }

    @Test
    fun aWholeDispatchIsAppliedAsOneChange() = runTest {
        loadChannels()
        val changes = live().handle(
            listOf(
                SevenTvLiveEvent.EmoteRemoved("setA", "a", "KEKW", "Mod"),
                SevenTvLiveEvent.EmoteRenamed("setA", "b", "PogU", "PogChamp", "Mod"),
                SevenTvLiveEvent.EmoteAdded("setA", emote("w", "Wave"), "")
            )
        )
        assertEquals(listOf("PogChamp", "Wave"), codes("alpha"))
        val change = changes.single() as SevenTvEmoteChange.Channel
        assertEquals(setOf("KEKW", "PogU", "PogChamp", "Wave"), change.touchedCodes)
        assertEquals(
            listOf(SevenTvEmoteNotice.Removed("Mod", "KEKW"), SevenTvEmoteNotice.Renamed("Mod", "PogU", "PogChamp")),
            change.notices
        )
    }

    @Test
    fun personalSetsUpdateTheirOwnersOnly() = runTest {
        loadChannels()
        val live = live()
        val granted = live.grant(SevenTvLiveEvent.PersonalEmoteSetGranted("1001", "personal1"))
        assertEquals(SevenTvEmoteChange.Personal(setOf("1001")), granted)
        assertTrue("personal:personal1" in source.subscribed)
        assertEquals(listOf("wave", "hug"), repository.getCachedPersonalEmotes("1001").map { it.code })

        val changes = live.handle(listOf(SevenTvLiveEvent.EmoteAdded("personal1", emote("h", "Hype"), "Owner")))
        assertEquals(listOf<SevenTvEmoteChange>(SevenTvEmoteChange.Personal(setOf("1001"))), changes)
        assertEquals(listOf("wave", "hug", "Hype"), repository.getCachedPersonalEmotes("1001").map { it.code })
        assertEquals(listOf("KEKW", "PogU"), codes("alpha"))

        val revoked = live.handle(listOf(SevenTvLiveEvent.PersonalEmoteSetRevoked("1001", "personal1")))
        assertEquals(listOf<SevenTvEmoteChange>(SevenTvEmoteChange.Personal(setOf("1001"))), revoked)
        assertTrue(repository.getCachedPersonalEmotes("1001").isEmpty())
    }

    @Test
    fun laterPersonalSetsWinOnTheSameCodeAndTheCacheFollowsEdits() = runTest {
        val live = live()
        live.grant(SevenTvLiveEvent.PersonalEmoteSetGranted("7", "personal1"))
        live.grant(SevenTvLiveEvent.PersonalEmoteSetGranted("7", "personal2"))
        val merged = repository.getCachedPersonalEmotes("7")
        assertEquals(listOf("wave" to "p3", "hug" to "p2"), merged.map { it.code to it.id })
        assertTrue(merged === repository.getCachedPersonalEmotes("7"))

        live.handle(listOf(SevenTvLiveEvent.EmoteRemoved("personal2", "p3", "wave", "")))
        assertEquals(listOf("wave" to "p1", "hug" to "p2"), repository.getCachedPersonalEmotes("7").map { it.code to it.id })
    }

    @Test
    fun switchingTheActiveSetReloadsAndReroutes() = runTest {
        loadChannels()
        val live = live()
        backend.users["222"] = SevenTvBackend.user("setC", "Event set", "userB", "n" to "NewOne")

        val change = live.switchSet("beta", "Streamer")

        assertEquals(
            SevenTvEmoteChange.Channel("beta", listOf(SevenTvEmoteNotice.SetSwitched("Streamer", "Event set")), null),
            change
        )
        assertEquals(listOf("NewOne"), codes("beta"))
        assertEquals("setC", repository.getSevenTvEmoteSetId("beta"))
        assertTrue("set:setC" in source.subscribed)
        assertTrue(repository.channelsUsingSevenTvSet("setB").isEmpty())
        assertTrue(live.handle(listOf(SevenTvLiveEvent.EmoteAdded("setB", emote("x", "Old"), "Mod"))).isEmpty())
        assertEquals(1, live.handle(listOf(SevenTvLiveEvent.EmoteAdded("setC", emote("y", "Fresh"), "Mod"))).size)
        assertEquals(listOf("NewOne", "Fresh"), codes("beta"))
    }

    @Test
    fun anUnchangedOrUnreachableSetIsNotReported() = runTest {
        loadChannels()
        val live = live()
        assertNull(live.switchSet("beta", "Streamer"))
        backend.users.remove("222")
        assertNull(live.switchSet("beta", "Streamer"))
        assertEquals(listOf("catJAM"), codes("beta"))
        assertNull(live.switchSet("unknown", "Streamer"))
    }
}
