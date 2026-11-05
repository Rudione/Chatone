package io.rudione.chatone.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import io.rudione.chatone.data.remote.TokenRefreshResult
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeviceAccountAuthTest {

    private val seen = mutableListOf<Parameters>()

    private fun client(status: HttpStatusCode, body: String) = TwitchDeviceAuthClient(
        httpClient = HttpClient(
            MockEngine { request ->
                seen += (request.body as FormDataContent).formData
                respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
            }
        ),
        accountClientId = "own-client",
        accountScopes = listOf("chat:read", "user:read:follows")
    )

    @Test
    fun accountCodeIsIssuedForOurOwnClient() = runTest {
        val auth = client(
            HttpStatusCode.OK,
            """{"device_code":"d","user_code":"ABCD1234","verification_uri":"https://www.twitch.tv/activate?device-code=ABCD1234","expires_in":1800,"interval":5}"""
        )

        val issued = withContext(Dispatchers.Default) { auth.requestAccountCode() }

        assertEquals("own-client", assertIs<io.rudione.chatone.data.remote.DeviceCodeResult.Issued>(issued).info.clientId)
        assertEquals("chat:read user:read:follows", seen.single()["scopes"])
    }

    @Test
    fun refreshKeepsTheOldTokenWhenTwitchSendsNoNewOne() = runTest {
        val auth = client(HttpStatusCode.OK, """{"access_token":"new","expires_in":14000}""")

        val result = withContext(Dispatchers.Default) { auth.refreshAccountToken("old-refresh") }

        val refreshed = assertIs<TokenRefreshResult.Refreshed>(result).token
        assertEquals("new", refreshed.accessToken)
        assertEquals("old-refresh", refreshed.refreshToken)
        assertEquals(14000, refreshed.expiresInSeconds)
        assertEquals("refresh_token", seen.single()["grant_type"])
        assertEquals("own-client", seen.single()["client_id"])
    }

    @Test
    fun rejectedRefreshIsNotRetriedForever() = runTest {
        val auth = client(HttpStatusCode.BadRequest, """{"status":400,"message":"Invalid refresh token"}""")

        val result = withContext(Dispatchers.Default) { auth.refreshAccountToken("bad") }

        assertEquals(TokenRefreshResult.Rejected, result)
    }
}
