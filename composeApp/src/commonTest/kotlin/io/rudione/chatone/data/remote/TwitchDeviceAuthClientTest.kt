package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwitchDeviceAuthClientTest {

    private val requests = mutableListOf<Map<String, String>>()

    private fun client(
        handler: suspend MockRequestHandleScope.(clientId: String?, request: HttpRequestData) -> HttpResponseData
    ): TwitchDeviceAuthClient {
        val engine = MockEngine { request ->
            val form = (request.body as FormDataContent).formData
            requests += form.names().associateWith { form[it].orEmpty() }
            handler(form["client_id"], request)
        }
        return TwitchDeviceAuthClient(HttpClient(engine))
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.deviceCode(userCode: String) = json(
        """{"device_code":"dev-$userCode","expires_in":1800,"interval":5,"user_code":"$userCode","verification_uri":"https://www.twitch.tv/activate"}"""
    )

    private fun MockRequestHandleScope.invalidClient() =
        json("""{"status":400,"message":"invalid client"}""", HttpStatusCode.BadRequest)

    @Test
    fun rejectedClientFallsBackToTheNextOne() = runTest {
        val auth = client { clientId, _ ->
            if (clientId == TwitchFirstPartyClient.TV.clientId) invalidClient() else deviceCode("WEBCODE")
        }

        val issued = assertIs<DeviceCodeResult.Issued>(withContext(Dispatchers.Default) { auth.requestDeviceCode() })

        assertEquals(TwitchFirstPartyClient.WEB, issued.info.client)
        assertEquals(
            listOf(TwitchFirstPartyClient.TV.clientId, TwitchFirstPartyClient.WEB.clientId),
            requests.map { it["client_id"] }
        )
    }

    @Test
    fun tvClientIsPreferredWhenTwitchAcceptsIt() = runTest {
        val auth = client { _, _ -> deviceCode("TVCODE") }

        val issued = assertIs<DeviceCodeResult.Issued>(withContext(Dispatchers.Default) { auth.requestDeviceCode() })

        assertEquals(TwitchFirstPartyClient.TV, issued.info.client)
        assertEquals(1, requests.size)
    }

    @Test
    fun everyClientRejectedIsNotRetryable() = runTest {
        val auth = client { _, _ -> invalidClient() }

        val failed = assertIs<DeviceCodeResult.Failed>(withContext(Dispatchers.Default) { auth.requestDeviceCode() })

        assertEquals(false, failed.retryable)
        assertEquals("invalid client", failed.message)
        assertEquals(TwitchFirstPartyClient.deviceFlowOrder.size, requests.size)
    }

    @Test
    fun serverFailureIsRetryableWithoutTryingOtherClients() = runTest {
        val auth = client { _, _ -> json("{}", HttpStatusCode.ServiceUnavailable) }

        val failed = assertIs<DeviceCodeResult.Failed>(withContext(Dispatchers.Default) { auth.requestDeviceCode() })

        assertTrue(failed.retryable)
        assertEquals(1, requests.size)
    }

    @Test
    fun acceptedClientIsRequestedFirstNextTime() = runTest {
        val auth = client { clientId, _ ->
            if (clientId == TwitchFirstPartyClient.TV.clientId) invalidClient() else deviceCode("AGAIN")
        }

        withContext(Dispatchers.Default) { auth.requestDeviceCode() }
        requests.clear()
        withContext(Dispatchers.Default) { auth.requestDeviceCode() }

        assertEquals(listOf(TwitchFirstPartyClient.WEB.clientId), requests.map { it["client_id"] })
    }

    @Test
    fun pollingUsesTheClientThatIssuedTheCode() = runTest {
        val auth = client { _, _ -> json("""{"access_token":"fresh-token"}""") }
        val info = DeviceCodeInfo(
            deviceCode = "dev",
            userCode = "ABCD",
            verificationUri = "https://www.twitch.tv/activate",
            expiresInSeconds = 60,
            intervalSeconds = 5,
            client = TwitchFirstPartyClient.TV
        )

        val result = withContext(Dispatchers.Default) { auth.pollToken(info) }

        assertEquals(DevicePollResult.Success("fresh-token"), result)
        assertEquals(TwitchFirstPartyClient.TV.clientId, requests.single()["client_id"])
        assertEquals("dev", requests.single()["device_code"])
    }

    @Test
    fun pendingAuthorizationKeepsPolling() = runTest {
        val auth = client { _, _ ->
            json("""{"status":400,"message":"authorization_pending"}""", HttpStatusCode.BadRequest)
        }
        val info = DeviceCodeInfo("dev", "ABCD", "https://www.twitch.tv/activate", 60, 5, TwitchFirstPartyClient.TV)

        assertEquals(DevicePollResult.Pending, withContext(Dispatchers.Default) { auth.pollToken(info) })
    }

    @Test
    fun activationLinkCarriesTheUserCode() {
        val plain = DeviceCodeInfo("d", "QWERTY12", "https://www.twitch.tv/activate", 60, 5, TwitchFirstPartyClient.TV)
        val prefilled = plain.copy(verificationUri = "https://www.twitch.tv/activate?device-code=QWERTY12")

        assertEquals("https://www.twitch.tv/activate?device-code=QWERTY12", plain.activationUri)
        assertEquals(prefilled.verificationUri, prefilled.activationUri)
    }
}
