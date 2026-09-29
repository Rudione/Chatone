package io.rudione.chatone.util.system

import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateSignatureTest {

    private val keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val digest = "0123456789abcdef".repeat(4)
    private val trusted = listOf(keyPair.public)

    private fun sign(version: String, assetName: String, sha256Hex: String): ByteArray =
        Signature.getInstance("Ed25519").apply {
            initSign(keyPair.private)
            update(UpdateSignature.message(version, assetName, sha256Hex))
        }.sign()

    @Test
    fun acceptsSignatureOfTheSameRelease() {
        val signature = sign("1.2.3", "Chatone-1.2.3-setup.exe", digest)
        assertTrue(UpdateSignature.verify("1.2.3", "Chatone-1.2.3-setup.exe", digest, signature, trusted))
        assertTrue(UpdateSignature.verify("1.2.3", "Chatone-1.2.3-setup.exe", digest.uppercase(), signature, trusted))
    }

    @Test
    fun rejectsSignatureReusedForAnotherVersion() {
        val signature = sign("1.2.3", "Chatone-1.2.3-setup.exe", digest)
        assertFalse(UpdateSignature.verify("9.9.9", "Chatone-1.2.3-setup.exe", digest, signature, trusted))
    }

    @Test
    fun rejectsSignatureReusedForAnotherAsset() {
        val signature = sign("1.2.3", "Chatone-1.2.3-setup.exe", digest)
        assertFalse(UpdateSignature.verify("1.2.3", "Chatone-1.2.3.msi", digest, signature, trusted))
    }

    @Test
    fun rejectsTamperedFile() {
        val signature = sign("1.2.3", "Chatone-1.2.3.dmg", digest)
        assertFalse(UpdateSignature.verify("1.2.3", "Chatone-1.2.3.dmg", digest.replaceFirst('0', '1'), signature, trusted))
    }

    @Test
    fun rejectsUntrustedSigner() {
        val signature = sign("1.2.3", "Chatone-1.2.3.dmg", digest)
        val stranger = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
        assertFalse(UpdateSignature.verify("1.2.3", "Chatone-1.2.3.dmg", digest, signature, listOf(stranger.public)))
    }

    @Test
    fun rejectsMalformedSignatureAndEmptyTrustList() {
        val signature = sign("1.2.3", "Chatone-1.2.3.dmg", digest)
        assertFalse(UpdateSignature.verify("1.2.3", "Chatone-1.2.3.dmg", digest, ByteArray(10), trusted))
        assertFalse(UpdateSignature.verify("1.2.3", "Chatone-1.2.3.dmg", digest, signature, emptyList()))
    }

    @Test
    fun understandsSignaturesMadeByTheReleaseScript() {
        val scriptMessage = "chatone-update-v1\n1.2.3\nChatone-1.2.3.dmg\n$digest\n".toByteArray(Charsets.UTF_8)
        val signature = Signature.getInstance("Ed25519").apply {
            initSign(keyPair.private)
            update(scriptMessage)
        }.sign()
        assertTrue(UpdateSignature.verify("1.2.3", "Chatone-1.2.3.dmg", digest, signature, trusted))
    }

    @Test
    fun bundledTrustedKeysAreOnTheClasspath() {
        assertTrue(UpdateSignature.trustedKeys.isNotEmpty())
    }

    @Test
    fun publicKeyListIgnoresBlankLinesAndGarbage() {
        val encoded = Base64.getEncoder().encodeToString(keyPair.public.encoded)
        assertEquals(1, UpdateSignature.parsePublicKeys("\n  $encoded  \n\nnot-a-key\n").size)
    }
}
