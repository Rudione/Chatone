package io.rudione.chatone.data.repository

import com.russhwolf.settings.PreferencesSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.rudione.chatone.data.auth.LoginSiteResolver
import io.rudione.chatone.data.auth.LoginReturnChannel
import io.rudione.chatone.data.auth.LoginReturnHandler
import io.rudione.chatone.data.auth.LoginReturnOutcome
import io.rudione.chatone.data.auth.LoginReturnTarget
import io.rudione.chatone.data.auth.NoLoginReturn
import io.rudione.chatone.data.remote.TwitchDeviceAuthClient
import io.rudione.chatone.data.remote.TwitchFirstPartyClient
import io.rudione.chatone.data.remote.TwitchGqlClient
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.security.SecureRandom
import java.util.Base64
import java.util.Collections
import java.util.prefs.Preferences
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebLoginControllerRightsTest {

    private val node = Preferences.userRoot().node("chatone-test-weblogin-${System.nanoTime()}")
    private val settings = PreferencesSettings(node)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests: MutableList<String> = Collections.synchronizedList(mutableListOf())
    private lateinit var store: ModerationAuthStore

    private val account = TwitchAccount(
        userId = "42",
        login = "viewer",
        displayName = "Viewer",
        profileImageUrl = "",
        accessToken = "app-token",
        refreshToken = "",
        expiresAt = 0L,
        scopes = emptyList()
    )

    @AfterTest
    fun tearDown() {
        scope.cancel()
        node.removeNode()
    }

    private fun controller(
        deviceStatus: HttpStatusCode = HttpStatusCode.OK,
        approved: Boolean = false,
        rightsOwner: String = "42",
        returnChannel: LoginReturnChannel = NoLoginReturn
    ): WebLoginController {
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        val twitch = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            requests += url
            when {
                url.endsWith("/oauth2/device") && deviceStatus == HttpStatusCode.OK -> respond(
                    """{"device_code":"appdevicecode0000000000","expires_in":1800,"interval":5,"user_code":"ABCDEFGH","verification_uri":"https://www.twitch.tv/activate"}""",
                    HttpStatusCode.OK,
                    json
                )

                url.endsWith("/oauth2/device") -> respond("""{"status":400,"message":"invalid client"}""", deviceStatus, json)

                url.endsWith("/oauth2/token") && approved -> respond(
                    """{"access_token":"firstpartytoken00000000000000","token_type":"bearer"}""",
                    HttpStatusCode.OK,
                    json
                )

                url.endsWith("/oauth2/token") -> respond(
                    """{"status":400,"message":"authorization_pending"}""",
                    HttpStatusCode.BadRequest,
                    json
                )

                url.endsWith("/oauth2/validate") -> respond(
                    """{"client_id":"${TwitchFirstPartyClient.TV.clientId}","login":"viewer","user_id":"$rightsOwner","scopes":[]}""",
                    HttpStatusCode.OK,
                    json
                )

                url.contains("gql.twitch.tv/gql") -> respond(
                    """[{"data":{"currentUser":{"id":"$rightsOwner","login":"viewer","displayName":"Viewer"}}}]""",
                    HttpStatusCode.OK,
                    json
                )

                else -> respond("ok", HttpStatusCode.OK)
            }
        })
        val accountManager = AccountManager(settings)
        store = ModerationAuthStore(settings, TwitchGqlClient(twitch), accountManager, scope)
        val device = FirstPartyDeviceAuthController(TwitchDeviceAuthClient(twitch), store, scope)
        return WebLoginController(
            authRepository = FakeAuthRepository(account),
            deviceAuthController = device,
            moderationAuthStore = store,
            siteResolver = LoginSiteResolver(twitch, "https://app.example.test/auth/", "https://ru.example.test/auth/"),
            returnChannel = returnChannel,
            scope = scope
        )
    }

    private fun WebLoginController.openSite(): String = runBlocking {
        var opened = ""
        begin { opened = it }
        withTimeout(5_000) { stage.first { it is WebLoginStage.AwaitingPaste } }
        opened
    }

    private fun WebLoginController.paste(line: String): WebLoginStage = runBlocking {
        submit(line)
        withTimeout(5_000) {
            stage.first {
                it is WebLoginStage.AwaitingRights || it is WebLoginStage.Failure || it is WebLoginStage.Success
            }
        }
    }

    private fun WebLoginController.pasteExpectingFallback(line: String): WebLoginStage = runBlocking {
        submit(line)
        withTimeout(5_000) {
            stage.first {
                (it is WebLoginStage.AwaitingRights && it.activationUrl != null) || it is WebLoginStage.Failure
            }
        }
    }

    private fun sealedLine(loginUrl: String, fields: String): String {
        val key = Base64.getUrlDecoder().decode(loginUrl.substringAfter("#k=").substringBefore('&'))
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD("chatone-login-v1".encodeToByteArray())
        val sealed = cipher.doFinal(fields.encodeToByteArray())
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return "chatone1." + encoder.encodeToString(nonce) + "." + encoder.encodeToString(sealed)
    }

    private fun chatFields(extra: String = "") =
        "username=viewer;user_id=42;client_id=5ez3vtq4fbp8nvpgbkpxk15oga8o7l;oauth_token=abcdefghijklmnopqrstuvwxyz;scopes=chat:read$extra"

    private val siteDevice = ";device_code=sitedevicecode000000000000;device_client_id=${TwitchFirstPartyClient.TV.clientId}"

    @Test
    fun loginLinkCarriesOnlyTheSessionKey() {
        val login = controller()

        val url = login.openSite()

        assertTrue(url.startsWith("https://app.example.test/auth/#k="))
        assertFalse(url.contains("d="))
        assertTrue(requests.none { it.endsWith("/oauth2/device") })
    }

    @Test
    fun deviceCodeFromTheSiteGrantsRightsWithoutOpeningTwitch() {
        val login = controller(approved = true)
        val url = login.openSite()

        val first = login.paste(sealedLine(url, chatFields(siteDevice)))
        val done = runBlocking { withTimeout(10_000) { login.stage.first { it is WebLoginStage.Success } } }

        assertNull((first as? WebLoginStage.AwaitingRights)?.activationUrl)
        assertIs<WebLoginStage.Success>(done)
        assertTrue(store.hasTokenFor("42"))
        assertTrue(requests.none { it.endsWith("/oauth2/device") })
    }

    @Test
    fun pendingSiteApprovalWaitsOnTheLoginPage() {
        val login = controller(approved = false)
        val url = login.openSite()

        val stage = assertIs<WebLoginStage.AwaitingRights>(login.paste(sealedLine(url, chatFields(siteDevice))))

        assertNull(stage.activationUrl)
        assertEquals(DeviceAuthState.ConfirmingOnLoginPage, login.deviceAuthState.value)
    }

    @Test
    fun rightsApprovedForAnotherAccountAreRejected() {
        val login = controller(approved = true, rightsOwner = "99")
        val url = login.openSite()

        login.paste(sealedLine(url, chatFields(siteDevice)))
        val stage = runBlocking { withTimeout(10_000) { login.stage.first { it is WebLoginStage.Failure } } }

        assertEquals(LoginFailure.RightsNotGranted, assertIs<WebLoginStage.Failure>(stage).reason)
        assertFalse(store.hasTokenFor("42"))
        assertFalse(store.hasTokenFor("99"))
    }

    @Test
    fun unknownDeviceClientIsIgnored() {
        val login = controller(approved = true)
        val url = login.openSite()
        val foreign = ";device_code=sitedevicecode000000000000;device_client_id=abcdefghijklmnopqrstuvwxyz0123"

        val stage = assertIs<WebLoginStage.AwaitingRights>(login.pasteExpectingFallback(sealedLine(url, chatFields(foreign))))

        assertEquals("https://www.twitch.tv/activate?device-code=ABCDEFGH", stage.activationUrl)
    }

    @Test
    fun plainLineCannotSmuggleADeviceCode() {
        val login = controller(approved = true)
        login.openSite()

        val stage = assertIs<WebLoginStage.AwaitingRights>(login.pasteExpectingFallback(chatFields(siteDevice)))

        assertEquals("https://www.twitch.tv/activate?device-code=ABCDEFGH", stage.activationUrl)
    }

    @Test
    fun olderSiteFallsBackToAnActivationLink() {
        val login = controller()
        val url = login.openSite()

        val stage = assertIs<WebLoginStage.AwaitingRights>(login.pasteExpectingFallback(sealedLine(url, chatFields())))

        assertEquals("https://www.twitch.tv/activate?device-code=ABCDEFGH", stage.activationUrl)
        assertEquals(stage.activationUrl, login.activationUrl())
    }

    @Test
    fun rightsUnavailableNeverBlocksTheLogin() {
        val login = controller(deviceStatus = HttpStatusCode.BadRequest)
        val url = login.openSite()

        val stage = assertIs<WebLoginStage.Failure>(login.pasteExpectingFallback(sealedLine(url, chatFields())))

        assertEquals(LoginFailure.RightsNotGranted, stage.reason)
        login.continueWithoutRights()
        assertIs<WebLoginStage.Success>(login.stage.value)
    }

    @Test
    fun continuingWithoutRightsKeepsTheApprovalPending() {
        val login = controller(approved = false)
        val url = login.openSite()
        login.paste(sealedLine(url, chatFields(siteDevice)))

        login.continueWithoutRights()
        login.reset()

        assertIs<WebLoginStage.Idle>(login.stage.value)
        assertEquals(DeviceAuthState.ConfirmingOnLoginPage, login.deviceAuthState.value)
    }

    @Test
    fun loopbackLoginTellsTheSiteWhereToReturn() {
        val login = controller(returnChannel = CapturingReturn(LoginReturnTarget.Loopback(45123)))

        val url = login.openSite()

        assertTrue(url.contains("#k="))
        assertTrue(url.endsWith("&r=45123"))
        assertTrue(login.supportsAutomaticReturn)
    }

    @Test
    fun returnedLineContinuesToActivationInTheSameTab() {
        val channel = CapturingReturn(LoginReturnTarget.Loopback(45123))
        val login = controller(returnChannel = channel)
        val url = login.openSite()

        val outcome = runBlocking { channel.deliver(sealedLine(url, chatFields())) }

        assertEquals(LoginReturnOutcome.Continue("https://www.twitch.tv/activate?device-code=ABCDEFGH"), outcome)
        val stage = assertIs<WebLoginStage.AwaitingRights>(login.stage.value)
        assertEquals(RightsHandoff.InBrowser, stage.handoff)
    }

    @Test
    fun appLinkReturnAsksTheAppToOpenActivation() {
        val channel = CapturingReturn(LoginReturnTarget.AppLink)
        val login = controller(returnChannel = channel)
        val url = login.openSite()

        assertTrue(url.endsWith("&app=1"))
        runBlocking { channel.deliver(sealedLine(url, chatFields())) }
        val stage = assertIs<WebLoginStage.AwaitingRights>(login.stage.value)
        assertEquals(RightsHandoff.OpenNow, stage.handoff)

        login.markActivationOpened()
        assertEquals(RightsHandoff.InBrowser, assertIs<WebLoginStage.AwaitingRights>(login.stage.value).handoff)
    }

    @Test
    fun returnChannelAcceptsOnlySealedLines() {
        val channel = CapturingReturn(LoginReturnTarget.Loopback(45123))
        val login = controller(returnChannel = channel)
        login.openSite()

        val outcome = runBlocking { channel.deliver(chatFields()) }

        assertEquals(LoginReturnOutcome.Rejected, outcome)
        assertIs<WebLoginStage.AwaitingPaste>(login.stage.value)
    }

    @Test
    fun lineSealedForAnotherSessionIsDroppedSilently() {
        val channel = CapturingReturn(LoginReturnTarget.Loopback(45123))
        val login = controller(returnChannel = channel)
        login.openSite()
        val strangerKey = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also(SecureRandom()::nextBytes))

        val outcome = runBlocking { channel.deliver(sealedLine("https://x/#k=$strangerKey", chatFields())) }

        assertEquals(LoginReturnOutcome.Rejected, outcome)
        assertIs<WebLoginStage.AwaitingPaste>(login.stage.value)
    }

    @Test
    fun resetClosesTheReturnChannel() {
        val channel = CapturingReturn(LoginReturnTarget.Loopback(45123))
        val login = controller(returnChannel = channel)
        login.openSite()

        login.reset()

        assertTrue(channel.closed > 0)
    }

    private class CapturingReturn(private val target: LoginReturnTarget) : LoginReturnChannel {
        private var handler: LoginReturnHandler? = null
        var closed = 0
            private set

        override suspend fun open(handler: LoginReturnHandler): LoginReturnTarget {
            this.handler = handler
            return target
        }

        override fun close() {
            closed++
        }

        suspend fun deliver(line: String): LoginReturnOutcome =
            handler?.accept(line) ?: LoginReturnOutcome.Rejected
    }

    private class FakeAuthRepository(private val account: TwitchAccount) : AuthRepository {
        override suspend fun authenticateWithToken(accessToken: String): Result<TwitchAccount> = Result.Success(account)
        override suspend fun validateToken(account: TwitchAccount): Result<Boolean> = Result.Success(true)
        override suspend fun revokeToken(account: TwitchAccount): Result<Unit> = Result.Success(Unit)
        override suspend fun saveAccount(account: TwitchAccount) = Unit
        override suspend fun getAccounts(): Flow<List<TwitchAccount>> = flowOf(listOf(account))
        override suspend fun getAccountById(userId: String): TwitchAccount? = account
        override suspend fun deleteAccount(userId: String, revoke: Boolean) = Unit
        override suspend fun getFirstValidAccount(): TwitchAccount? = account
    }
}
