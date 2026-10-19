package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwitchGqlFollowTest {

    private val gqlBodies = mutableListOf<String>()
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun client(gqlAnswer: String) = TwitchGqlClient(HttpClient(MockEngine { request ->
        val url = request.url.toString()
        when {
            url.startsWith("https://id.twitch.tv/oauth2/validate") ->
                respond("""{"client_id":"${TwitchFirstPartyClient.TV.clientId}"}""", HttpStatusCode.OK, json)
            url.contains("integrity") -> respond("""{"token":"integrity"}""", HttpStatusCode.OK, json)
            else -> {
                gqlBodies += (request.body as TextContent).text
                respond(gqlAnswer, HttpStatusCode.OK, json)
            }
        }
    }))

    @Test
    fun followerEdgeMeansFollowing() = runTest {
        val gql = client("""[{"data":{"user":{"self":{"follower":{"followedAt":"2024-01-01T00:00:00Z"}}}}}]""")

        val result = withContext(Dispatchers.Default) { gql.isFollowing("71092938", "token") }

        assertEquals(Result.Success(true), result)
        assertTrue(gqlBodies.single().contains("71092938"))
    }

    @Test
    fun missingFollowerEdgeMeansNotFollowing() = runTest {
        val gql = client("""[{"data":{"user":{"self":{"follower":null}}}}]""")

        assertEquals(Result.Success(false), withContext(Dispatchers.Default) { gql.isFollowing("1", "token") })
    }

    @Test
    fun followSendsTheTargetAndSucceeds() = runTest {
        val gql = client("""[{"data":{"followUser":{"follow":{"followedAt":"now"},"error":null}}}]""")

        val result = withContext(Dispatchers.Default) { gql.setFollowing("71092938", follow = true, token = "token") }

        assertEquals(Result.Success(Unit), result)
        assertTrue(gqlBodies.first().contains("followUser"))
        assertTrue(gqlBodies.first().contains("\"targetID\":\"71092938\""))
    }

    @Test
    fun integrityGateOnEveryClientIsReportedAsBlocked() = runTest {
        val gql = client("""[{"errors":[{"message":"failed integrity check","extensions":{"code":"IntegrityCheckFailed"}}]}]""")

        val result = withContext(Dispatchers.Default) { gql.setFollowing("1", follow = false, token = "token") }

        assertIs<GqlIntegrityRejectedException>(assertIs<Result.Error>(result).exception)
    }

    @Test
    fun followErrorCodeIsAFailure() = runTest {
        val gql = client("""[{"data":{"followUser":{"follow":null,"error":{"code":"TOO_MANY_FOLLOWS"}}}}]""")

        val result = withContext(Dispatchers.Default) { gql.setFollowing("1", follow = true, token = "token") }

        assertEquals("TOO_MANY_FOLLOWS", assertIs<Result.Error>(result).exception.message)
    }
}
