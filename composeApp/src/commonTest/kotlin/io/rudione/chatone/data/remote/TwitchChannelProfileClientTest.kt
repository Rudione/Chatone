package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.domain.profile.ClipOrder
import io.rudione.chatone.domain.profile.ClipPeriod
import io.rudione.chatone.domain.profile.ClipQuery
import io.rudione.chatone.domain.profile.VideoKind
import io.rudione.chatone.domain.profile.VideoOrder
import io.rudione.chatone.domain.profile.VideoQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwitchChannelProfileClientTest {

    private val bodies = mutableListOf<String>()

    private fun client(body: String, status: HttpStatusCode = HttpStatusCode.OK) = TwitchChannelProfileClient(
        ViewerGqlTransport(HttpClient(MockEngine { request ->
            bodies += (request.body as TextContent).text
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }))
    )

    @Test
    fun profileMapsLiveStateLinksAndPanels() = runTest {
        val profile = withContext(Dispatchers.Default) {
            client(PROFILE).profile("LapShad")
        }!!
        assertEquals("495598047", profile.id)
        assertEquals("lapshad", profile.login)
        assertEquals(536, profile.followers)
        assertTrue(profile.isAffiliate)
        assertEquals("Minecraft", profile.live?.category)
        assertEquals(32, profile.live?.viewers)
        assertEquals("https://t.me/lapshadd", profile.links.single().url)
        assertEquals(1, profile.panels.size)
        assertEquals("0DD987", profile.accentHex)
        assertEquals(12, profile.pastBroadcastCount)
        assertTrue(bodies.single().contains("\"login\":\"lapshad\""))
    }

    @Test
    fun invalidLoginIsNeverRequested() = runTest {
        assertNull(client(PROFILE).profile("../etc"))
        assertTrue(bodies.isEmpty())
    }

    @Test
    fun missingUserIsNull() = runTest {
        val profile = withContext(Dispatchers.Default) { client("""{"data":{"user":null}}""").profile("ghost") }
        assertNull(profile)
    }

    @Test
    fun videosSendTypeAndSortAndDropPlaceholderThumbnails() = runTest {
        val page = withContext(Dispatchers.Default) {
            client(VIDEOS).videos("lapshad", VideoQuery(VideoKind.HIGHLIGHT, VideoOrder.POPULAR), null)
        }!!
        val sent = bodies.single()
        assertTrue(sent.contains("\"type\":\"HIGHLIGHT\""))
        assertTrue(sent.contains("\"sort\":\"VIEWS\""))
        assertEquals(2, page.items.size)
        assertEquals("", page.items.first().thumbnailUrl)
        assertEquals("cursor-2", page.nextCursor)
        assertEquals(VideoKind.ARCHIVE, page.items.last().kind)
    }

    @Test
    fun clipsTranslateCriteria() = runTest {
        val page = withContext(Dispatchers.Default) {
            client(CLIPS).clips("lapshad", ClipQuery(ClipPeriod.WEEK, ClipOrder.TRENDING, featuredOnly = true), null)
        }!!
        val sent = bodies.single()
        assertTrue(sent.contains("\"period\":\"LAST_WEEK\""))
        assertTrue(sent.contains("\"sort\":\"TRENDING\""))
        assertTrue(sent.contains("\"isFeatured\":true"))
        assertEquals("ExuberantDrab", page.items.single().slug)
        assertTrue(page.items.single().isFeatured)
        assertFalse(page.hasMore)
    }

    @Test
    fun serverFailureIsNull() = runTest {
        val page = withContext(Dispatchers.Default) {
            client("oops", HttpStatusCode.BadGateway).clips("lapshad", ClipQuery(), null)
        }
        assertNull(page)
    }

    private companion object {
        val PROFILE = """
            {"data":{"user":{"id":"495598047","login":"lapshad","displayName":"lapshad","description":"bio",
            "createdAt":"2020-02-27T10:28:31Z","bannerImageURL":"https://static-cdn.jtvnw.net/b.jpeg","offlineImageURL":null,
            "primaryColorHex":"0DD987","chatColor":"#008000","profileImageURL":"https://static-cdn.jtvnw.net/a.png",
            "followers":{"totalCount":536},"roles":{"isPartner":false,"isAffiliate":true,"isStaff":null},
            "primaryTeam":null,"channel":{"socialMedias":[{"name":"t","title":"tg","url":"https://t.me/lapshadd"}]},
            "broadcastSettings":{"language":"RU"},
            "stream":{"title":"t","viewersCount":32,"createdAt":"2026-10-27T20:40:42Z","previewImageURL":"https://static-cdn.jtvnw.net/p.jpg",
            "freeformTags":[{"name":"Русский"}],"game":{"displayName":"Minecraft","boxArtURL":"https://static-cdn.jtvnw.net/g.jpg"}},
            "lastBroadcast":{"startedAt":"2026-10-27T20:40:48Z","title":"t","game":{"displayName":"Minecraft"}},
            "videos":{"totalCount":12},
            "panels":[{"title":null,"description":null,"imageURL":"https://panels.twitch.tv/x","linkURL":"https://t.me/lapshadd"},{}]}}}
        """.trimIndent()

        val VIDEOS = """
            {"data":{"user":{"videos":{"edges":[
            {"cursor":"cursor-1","node":{"id":"1","title":"a","lengthSeconds":60,"viewCount":0,"createdAt":"2026-10-27T20:40:47Z",
            "broadcastType":"HIGHLIGHT","previewThumbnailURL":"https://vod-secure.twitch.tv/_404/404_processing_320x180.png","game":null}},
            {"cursor":"cursor-2","node":{"id":"2","title":"b","lengthSeconds":17325,"viewCount":2849,"createdAt":"2026-10-26T20:04:20Z",
            "broadcastType":"ARCHIVE","previewThumbnailURL":"https://static-cdn.jtvnw.net/t.jpg","game":{"displayName":"Counter-Strike"}}}
            ],"pageInfo":{"hasNextPage":true}}}}}
        """.trimIndent()

        val CLIPS = """
            {"data":{"user":{"clips":{"edges":[{"cursor":null,"node":{"id":"9","slug":"ExuberantDrab","title":"c","viewCount":141,
            "createdAt":"2025-08-17T23:20:00Z","durationSeconds":13,"isFeatured":true,"url":"https://www.twitch.tv/lapshad/clip/ExuberantDrab",
            "thumbnailURL":"https://static-cdn.jtvnw.net/c.jpg","curator":{"displayName":"lapshad"},"game":{"displayName":"Counter-Strike"}}}],
            "pageInfo":{"hasNextPage":false}}}}}
        """.trimIndent()
    }
}
