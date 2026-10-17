package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.domain.model.ChannelGifConfig
import io.rudione.chatone.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwitchGqlGifConfigTest {

    private val gqlBodies = mutableListOf<String>()
    private val gqlClientIds = mutableListOf<String?>()

    private fun client(config: String) = TwitchGqlClient(HttpClient(MockEngine { request ->
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        if (request.url.toString().startsWith("https://id.twitch.tv/oauth2/validate")) {
            respond("""{"client_id":"${TwitchFirstPartyClient.TV.clientId}"}""", HttpStatusCode.OK, json)
        } else {
            gqlBodies += (request.body as TextContent).text
            gqlClientIds += request.headers["Client-Id"]
            respond("""[{"data":{"gifPickerConfig":$config}}]""", HttpStatusCode.OK, json)
        }
    }))

    @Test
    fun allowlistedChannelGivesTheGiphyKeyAndRating() = runTest {
        val gql = client("""{"isEnabled":true,"isAllowlisted":true,"apiKey":"giphy-key","contentRating":"PG_13"}""")

        val result = withContext(Dispatchers.Default) { gql.channelGifConfig("71092938", "tv-token") }

        assertEquals(Result.Success(ChannelGifConfig(enabled = true, apiKey = "giphy-key", rating = "pg-13")), result)
        assertTrue(gqlBodies.single().contains("gifPickerConfig"))
        assertTrue(gqlBodies.single().contains("71092938"))
        assertEquals(TwitchFirstPartyClient.TV.clientId, gqlClientIds.single())
    }

    @Test
    fun channelOutsideTheAllowlistIsDisabled() = runTest {
        val gql = client("""{"isEnabled":true,"isAllowlisted":false,"apiKey":"giphy-key","contentRating":"G_PG"}""")

        val result = withContext(Dispatchers.Default) { gql.channelGifConfig("1", "tv-token") }

        assertEquals(Result.Success(ChannelGifConfig(enabled = false, apiKey = "giphy-key", rating = "pg")), result)
    }

    @Test
    fun missingConfigOrTokenIsAnError() = runTest {
        val gql = client("null")

        withContext(Dispatchers.Default) {
            assertIs<Result.Error>(gql.channelGifConfig("1", "tv-token"))
            assertIs<Result.Error>(gql.channelGifConfig("1", ""))
        }
    }
}
