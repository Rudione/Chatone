package io.rudione.chatone.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.domain.moderation.UnbanTarget
import io.rudione.chatone.domain.moderation.UnbannedUser
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChannelUnbanRepositoryTest {
    private val actor = ModerationActor("stale-token", "channel", "moderator")
    private val account = TwitchAccount("moderator", "mod", "Mod", "", "saved-token", "refresh", Long.MAX_VALUE, emptyList())
    private val userJson = """{"data":[{"id":"444785648","login":"alienlovepeople","display_name":"alienlovepeople","type":"","broadcaster_type":"","description":"","profile_image_url":"","offline_image_url":"","created_at":""}]}"""
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private class FakeAuth(var account: TwitchAccount?) : AuthRepository {
        var refreshes = 0
        override suspend fun getAccountById(userId: String): TwitchAccount? = account?.takeIf { it.userId == userId }
        override suspend fun refreshAccount(account: TwitchAccount): TokenRefreshOutcome {
            refreshes++
            val updated = account.copy(accessToken = "refreshed-token", expiresAt = Long.MAX_VALUE)
            this.account = updated
            return TokenRefreshOutcome.Refreshed(updated)
        }
        override suspend fun getAccounts(): Flow<List<TwitchAccount>> = flowOf(listOfNotNull(account))
        override suspend fun authenticateWithToken(accessToken: String, refreshToken: String): Result<TwitchAccount> = error("unused")
        override suspend fun validateToken(account: TwitchAccount): Result<Boolean> = error("unused")
        override suspend fun revokeToken(account: TwitchAccount): Result<Unit> = error("unused")
        override suspend fun saveAccount(account: TwitchAccount) { this.account = account }
        override suspend fun deleteAccount(userId: String, revoke: Boolean) = error("unused")
        override suspend fun getFirstValidAccount(): TwitchAccount? = account
    }

    @Test
    fun loginOnlyTargetIsResolvedBeforeUnbanning() = runTest {
        val paths = mutableListOf<String>()
        val client = HttpClient(MockEngine { request ->
            paths += request.url.encodedPath
            assertEquals("Bearer saved-token", request.headers[HttpHeaders.Authorization])
            if (request.method == HttpMethod.Get) {
                assertEquals("alienlovepeople", request.url.parameters["login"])
                assertEquals(null, request.url.parameters["id"])
                respond(userJson, HttpStatusCode.OK, jsonHeaders)
            } else {
                assertEquals("444785648", request.url.parameters["user_id"])
                respond("", HttpStatusCode.NoContent)
            }
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        try {
            val repository = ChannelUnbanRepository(TwitchApiClient(client, "client"), FakeAuth(account))
            assertEquals(Result.Success(UnbannedUser("444785648", "alienlovepeople")), repository.unban(actor, UnbanTarget(login = "alienlovepeople")))
            assertEquals(listOf("/helix/users", "/helix/moderation/bans"), paths)
        } finally { client.close() }
    }

    @Test
    fun idOnlyTargetGetsACanonicalLoginForTheLocalNotice() = runTest {
        val client = HttpClient(MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                assertEquals("444785648", request.url.parameters["id"])
                respond(userJson, HttpStatusCode.OK, jsonHeaders)
            } else respond("", HttpStatusCode.NoContent)
        }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        try {
            val repository = ChannelUnbanRepository(TwitchApiClient(client, "client"), FakeAuth(account))
            assertEquals("alienlovepeople", assertIs<Result.Success<UnbannedUser>>(repository.unban(actor, UnbanTarget(userId = "444785648"))).data.login)
        } finally { client.close() }
    }

    @Test
    fun expiredSavedSessionRefreshesBeforeSendingAnyRequest() = runTest {
        val auth = FakeAuth(account.copy(expiresAt = 0))
        val client = HttpClient(MockEngine { request ->
            assertEquals("Bearer refreshed-token", request.headers[HttpHeaders.Authorization])
            assertEquals(HttpMethod.Delete, request.method)
            respond("", HttpStatusCode.NoContent)
        })
        try {
            val repository = ChannelUnbanRepository(TwitchApiClient(client, "client"), auth)
            assertTrue(repository.unban(actor, UnbanTarget("444785648", "alienlovepeople")).isSuccess)
            assertEquals(1, auth.refreshes)
        } finally { client.close() }
    }

    @Test
    fun unauthorizedUnbanRetriesOnceWithRefreshedCredentials() = runTest {
        val auth = FakeAuth(account)
        val tokens = mutableListOf<String?>()
        val client = HttpClient(MockEngine { request ->
            tokens += request.headers[HttpHeaders.Authorization]
            if (tokens.size == 1) respond("""{"message":"Invalid OAuth token"}""", HttpStatusCode.Unauthorized, jsonHeaders)
            else respond("", HttpStatusCode.NoContent)
        })
        try {
            val repository = ChannelUnbanRepository(TwitchApiClient(client, "client"), auth)
            assertTrue(repository.unban(actor, UnbanTarget("444785648", "alienlovepeople")).isSuccess)
            assertEquals(listOf("Bearer saved-token", "Bearer refreshed-token"), tokens.filterNotNull())
            assertEquals(1, auth.refreshes)
        } finally { client.close() }
    }

    @Test
    fun missingUserNeverReachesTheModerationEndpoint() = runTest {
        var lookups = 0
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            lookups++
            respond("""{"data":[]}""", HttpStatusCode.OK, jsonHeaders)
        }) { install(ContentNegotiation) { json() } }
        try {
            val repository = ChannelUnbanRepository(TwitchApiClient(client, "client"), FakeAuth(account))
            assertTrue(repository.unban(actor, UnbanTarget(login = "unknown")).isError)
            assertEquals(1, lookups)
        } finally { client.close() }
    }
}
