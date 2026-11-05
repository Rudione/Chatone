package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwitchFollowsClientTest {

    private val hosts = mutableListOf<String>()

    private fun client(respond: (String) -> String): TwitchFollowsClient {
        val http = HttpClient(MockEngine { request ->
            hosts += request.url.host + request.url.encodedPath
            val body = when {
                request.url.host == "id.twitch.tv" -> """{"client_id":"ue6666qo983tsx6so1t0vnawi233wa"}"""
                else -> respond(request.url.host)
            }
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        return TwitchFollowsClient(http, ViewerGqlTransport(http), helixClientId = "app")
    }

    @Test
    fun viewerFollowsReadLiveStateAndCursor() = runTest {
        val page = withContext(Dispatchers.Default) {
            client {
                """{"data":{"currentUser":{"follows":{"edges":[
                {"cursor":"c1","followedAt":"2024-01-02T00:00:00Z","node":{"id":"1","login":"buster","displayName":"Buster",
                "profileImageURL":"https://static-cdn.jtvnw.net/a.png","stream":{"viewersCount":12000,"game":{"displayName":"Dota 2"}}}},
                {"cursor":"c2","followedAt":"2023-01-02T00:00:00Z","node":{"id":"2","login":"lapshad","displayName":"lapshad",
                "profileImageURL":"https://static-cdn.jtvnw.net/b.png","stream":null}}
                ],"pageInfo":{"hasNextPage":true}}}}}"""
            }.viewerFollows("token", null)
        }!!
        assertEquals(listOf("buster", "lapshad"), page.channels.map { it.login })
        assertEquals(12000, page.channels.first().liveViewers)
        assertEquals("Dota 2", page.channels.first().liveCategory)
        assertTrue(!page.channels.last().isLive)
        assertEquals("c2", page.nextCursor)
    }

    @Test
    fun signedOutViewerHasNoFollows() = runTest {
        val page = withContext(Dispatchers.Default) {
            client { """{"data":{"currentUser":null}}""" }.viewerFollows("token", null)
        }
        assertNull(page)
    }

    @Test
    fun helixFollowsAreEnrichedWithAvatarsAndLiveState() = runTest {
        val page = withContext(Dispatchers.Default) {
            client { host ->
                if (host == "api.twitch.tv") {
                    """{"data":[{"broadcaster_id":"7","broadcaster_login":"emiru","broadcaster_name":"Emiru","followed_at":"2022-05-01T00:00:00Z"}],"pagination":{}}"""
                } else {
                    """{"data":{"users":[{"id":"7","login":"emiru","displayName":"Emiru","profileImageURL":"https://static-cdn.jtvnw.net/e.png",
                    "stream":{"viewersCount":7400,"game":{"displayName":"Just Chatting"}}}]}}"""
                }
            }.helixFollows("42", "token", null)
        }!!
        val emiru = page.channels.single()
        assertEquals("https://static-cdn.jtvnw.net/e.png", emiru.avatarUrl)
        assertEquals(7400, emiru.liveViewers)
        assertTrue(emiru.followedAtMs != null)
        assertNull(page.nextCursor)
        assertTrue(hosts.any { it.startsWith("api.twitch.tv/helix/channels/followed") })
    }
}
