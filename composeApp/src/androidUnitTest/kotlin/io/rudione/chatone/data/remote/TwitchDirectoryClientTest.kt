package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.domain.browse.StreamFilter
import io.rudione.chatone.domain.browse.StreamSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class TwitchDirectoryClientTest {

    private val requests = mutableListOf<JsonObject>()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(600_000L)
    }

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK): TwitchDirectoryClient {
        val engine = MockEngine { request ->
            requests += Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return TwitchDirectoryClient(HttpClient(engine), fixedClock)
    }

    @Test
    fun topCategoriesAreParsedWithViewerCounts() = runTest {
        val directory = client(
            """{"data":{"games":{"edges":[
                {"cursor":"c1","node":{"id":"509658","name":"Just Chatting","displayName":"Just Chatting","viewersCount":126193,"broadcastersCount":2535,"boxArtURL":"https://static-cdn.jtvnw.net/ttv-boxart/509658-285x380.jpg"}},
                {"cursor":"c2","node":{"id":"bad id!","displayName":"Broken"}}
            ],"pageInfo":{"hasNextPage":true}}}}"""
        )

        val page = withContext(Dispatchers.Default) { directory.topCategories(null) }!!

        assertEquals(1, page.items.size)
        assertEquals("Just Chatting", page.items.single().name)
        assertEquals(126_193, page.items.single().viewers)
        assertEquals(2_535, page.items.single().broadcasters)
        assertEquals("c2", page.nextCursor)
    }

    @Test
    fun lastPageHasNoCursor() = runTest {
        val directory = client("""{"data":{"games":{"edges":[{"cursor":"c1","node":{"id":"1","displayName":"A"}}],"pageInfo":{"hasNextPage":false}}}}""")

        assertNull(withContext(Dispatchers.Default) { directory.topCategories("c0") }!!.nextCursor)
        assertEquals("c0", requests.single()["variables"]!!.jsonObject["after"]!!.jsonPrimitive.content)
    }

    @Test
    fun streamFiltersAreSentAsGraphQlVariables() = runTest {
        val directory = client("""{"data":{"game":{"streams":{"edges":[],"pageInfo":{"hasNextPage":false}}}}}""")

        withContext(Dispatchers.Default) {
            directory.categoryStreams("509658", StreamFilter(StreamSort.VIEWERS_LOW, language = "RU", tag = "Русский"), null)
        }

        val variables = requests.single()["variables"]!!.jsonObject
        val options = variables["options"]!!.jsonObject
        assertEquals("509658", variables["id"]!!.jsonPrimitive.content)
        assertEquals("VIEWER_COUNT_ASC", options["sort"]!!.jsonPrimitive.content)
        assertEquals("RU", options["broadcasterLanguages"]!!.jsonArray.single().jsonPrimitive.content)
        assertEquals("Русский", options["freeformTags"]!!.jsonArray.single().jsonPrimitive.content)
        assertTrue(requests.single()["query"]!!.jsonPrimitive.content.contains("\$options"))
    }

    @Test
    fun unknownLanguageIsNeverSent() = runTest {
        val directory = client("""{"data":{"game":{"streams":{"edges":[],"pageInfo":{"hasNextPage":false}}}}}""")

        withContext(Dispatchers.Default) { directory.categoryStreams("1", StreamFilter(language = "XX"), null) }

        assertNull(requests.single()["variables"]!!.jsonObject["options"]!!.jsonObject["broadcasterLanguages"])
    }

    @Test
    fun streamsAreParsedAndUnsafeValuesDropped() = runTest {
        val directory = client(
            """{"data":{"game":{"streams":{"edges":[
                {"cursor":"s1","node":{"id":"42","title":"Hello","viewersCount":6776,"createdAt":"2026-09-22T14:41:07Z","language":"RU",
                 "previewImageURL":"https://static-cdn.jtvnw.net/previews-ttv/live_user_enzzai-440x248.jpg",
                 "freeformTags":[{"name":"Русский"},{"name":"DropsВключены"}],
                 "broadcaster":{"login":"Enzzai","displayName":"enzzai","profileImageURL":"http://insecure.example/a.png"}}},
                {"cursor":"s2","node":{"id":"43","broadcaster":{"login":"../evil"}}}
            ],"pageInfo":{"hasNextPage":false}}}}}"""
        )

        val stream = withContext(Dispatchers.Default) { directory.categoryStreams("1", StreamFilter(), null) }!!.items.single()

        assertEquals("enzzai", stream.login)
        assertEquals("", stream.avatarUrl)
        assertEquals(listOf("Русский", "DropsВключены"), stream.tags)
        assertEquals(1_790_088_067_000L, stream.startedAtMs)
        assertEquals("https://static-cdn.jtvnw.net/previews-ttv/live_user_enzzai-440x248.jpg?b=2", stream.previewUrl)
    }

    @Test
    fun searchReturnsChannelsWithLiveState() = runTest {
        val directory = client(
            """{"data":{
                "searchCategories":{"edges":[{"node":{"id":"32399","displayName":"Counter-Strike","viewersCount":45365}}]},
                "searchUsers":{"edges":[
                    {"node":{"login":"shroud","displayName":"shroud","stream":{"viewersCount":15750,"game":{"displayName":"Rust"}}}},
                    {"node":{"login":"offline_one","displayName":"Offline","stream":null}}
                ]}}}"""
        )

        val result = withContext(Dispatchers.Default) { directory.search("  shroud ") }!!

        assertEquals("shroud", requests.single()["variables"]!!.jsonObject["query"]!!.jsonPrimitive.content)
        assertEquals(listOf("Counter-Strike"), result.categories.map { it.name })
        assertEquals(15_750, result.channels.first().liveViewers)
        assertEquals("Rust", result.channels.first().liveCategory)
        assertEquals(false, result.channels.last().isLive)
    }

    @Test
    fun failedRequestReturnsNull() = runTest {
        val directory = client("{}", HttpStatusCode.BadGateway)

        assertNull(withContext(Dispatchers.Default) { directory.topCategories(null) })
        assertNull(withContext(Dispatchers.Default) { directory.search("x") })
    }
}
