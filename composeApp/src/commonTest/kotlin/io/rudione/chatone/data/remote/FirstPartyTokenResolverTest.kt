package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FirstPartyTokenResolverTest {

    private val validateCalls = mutableListOf<String>()
    private val gqlClientIds = mutableListOf<String?>()
    private val gqlOrigins = mutableListOf<String?>()

    private fun engine(validateStatus: HttpStatusCode, clientId: String) = MockEngine { request ->
        val url = request.url.toString()
        if (url.startsWith("https://id.twitch.tv/oauth2/validate")) {
            validateCalls += request.headers[HttpHeaders.Authorization].orEmpty()
            respond(
                """{"client_id":"$clientId","login":"viewer","scopes":[],"user_id":"42","expires_in":1000}""",
                validateStatus,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        } else {
            gqlClientIds += request.headers["Client-Id"]
            gqlOrigins += request.headers["Origin"]
            assertTrue((request.body as TextContent).text.contains("currentUser"))
            respond(
                """[{"data":{"currentUser":{"id":"42","login":"viewer","displayName":"Viewer"}}}]""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    }

    @Test
    fun tvTokenIsResolvedToTheTvClient() = runTest {
        val resolver = FirstPartyTokenResolver(HttpClient(engine(HttpStatusCode.OK, TwitchFirstPartyClient.TV.clientId)))

        val client = withContext(Dispatchers.Default) { resolver.clientFor("tv-token") }

        assertEquals(TwitchFirstPartyClient.TV, client)
        assertEquals(listOf("OAuth tv-token"), validateCalls)
    }

    @Test
    fun resolutionIsCachedPerToken() = runTest {
        val resolver = FirstPartyTokenResolver(HttpClient(engine(HttpStatusCode.OK, TwitchFirstPartyClient.TV.clientId)))

        withContext(Dispatchers.Default) {
            repeat(3) { resolver.clientFor("tv-token") }
        }

        assertEquals(1, validateCalls.size)
    }

    @Test
    fun unknownOrRejectedTokensKeepTheWebClient() = runTest {
        val thirdParty = FirstPartyTokenResolver(HttpClient(engine(HttpStatusCode.OK, "5ez3vtq4fbp8nvpgbkpxk15oga8o7l")))
        val rejected = FirstPartyTokenResolver(HttpClient(engine(HttpStatusCode.Unauthorized, "")))

        withContext(Dispatchers.Default) {
            assertEquals(TwitchFirstPartyClient.WEB, thirdParty.clientFor("app-token"))
            assertEquals(TwitchFirstPartyClient.WEB, rejected.clientFor("dead-token"))
            assertEquals(TwitchFirstPartyClient.WEB, rejected.clientFor(""))
        }
    }

    @Test
    fun gqlRequestsWithTvTokenCarryTvClientHeaders() = runTest {
        val gql = TwitchGqlClient(HttpClient(engine(HttpStatusCode.OK, TwitchFirstPartyClient.TV.clientId)))

        val identity = withContext(Dispatchers.Default) { gql.validateCustomToken("OAuth tv-token") }

        assertNotNull(identity)
        assertEquals(listOf<String?>(TwitchFirstPartyClient.TV.clientId), gqlClientIds)
        assertEquals(listOf<String?>(TwitchFirstPartyClient.TV.origin), gqlOrigins)
        assertEquals(listOf("OAuth tv-token"), validateCalls)
    }

    @Test
    fun gqlRequestsWithWebTokenKeepWebHeaders() = runTest {
        val gql = TwitchGqlClient(HttpClient(engine(HttpStatusCode.OK, TwitchFirstPartyClient.WEB.clientId)))

        withContext(Dispatchers.Default) { gql.validateCustomToken("web-token") }

        assertEquals(listOf<String?>(TwitchFirstPartyClient.WEB.clientId), gqlClientIds)
        assertEquals(listOf<String?>(null), gqlOrigins)
    }
}
