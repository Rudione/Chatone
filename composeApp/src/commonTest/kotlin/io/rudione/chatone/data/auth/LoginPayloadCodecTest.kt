package io.rudione.chatone.data.auth

import io.rudione.chatone.util.security.AEAD_NONCE_BYTES
import io.rudione.chatone.util.security.AEAD_TAG_BYTES
import io.rudione.chatone.util.security.Base64Url
import io.rudione.chatone.util.security.PlatformAead
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginPayloadCodecTest {

    private val plainPayload = "username=someone;oauth_token=abcdefghijkfasdffd2lmnopqrstuvwxyz0123"

    private val sealedPayload = LoginPayloadCodec.ENCRYPTED_PREFIX +
            Base64Url.encode(ByteArray(AEAD_NONCE_BYTES) { 1 }) + "." +
            Base64Url.encode(ByteArray(AEAD_TAG_BYTES + 16) { 2 })

    @Test
    fun sealedPayloadIsSubmittedAutomatically() {
        assertTrue(LoginPayloadCodec.canAutoSubmit(sealedPayload))
    }

    @Test
    fun plainPayloadIsNotAutoSubmittedWhereEncryptionExists() {
        assertEquals(!PlatformAead.isSupported, LoginPayloadCodec.canAutoSubmit(plainPayload))
    }

    @Test
    fun plainPayloadStillWorksWhenSubmittedByHand() {
        val success = assertIs<LoginPayloadResult.Success>(
            LoginPayloadCodec.decode(plainPayload, key = ByteArray(32))
        )
        assertEquals("abcdefghijkfasdffd2lmnopqrstuvwxyz0123", success.credentials.oauthToken)
    }

    @Test
    fun unrelatedClipboardContentIsIgnored() {
        listOf("", "   ", "hello world", "chatone1.", "chatone1.abc.def", "https://twitch.tv/somebody")
            .forEach { assertFalse(LoginPayloadCodec.canAutoSubmit(it), it) }
    }

    @Test
    fun plainPayloadNeverCarriesADeviceCode() {
        val plain = "$plainPayload;device_code=sitedevicecode000000000000;device_client_id=ue6666qo983tsx6so1t0vnawi233wa"

        val success = assertIs<LoginPayloadResult.Success>(LoginPayloadCodec.decode(plain, key = ByteArray(32)))

        assertEquals("", success.credentials.deviceCode)
        assertEquals("", success.credentials.deviceClientId)
    }
}
