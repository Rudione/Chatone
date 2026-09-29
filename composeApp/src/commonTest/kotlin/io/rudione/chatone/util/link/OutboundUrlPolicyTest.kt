package io.rudione.chatone.util.link

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutboundUrlPolicyTest {

    private fun ipv4(vararg octets: Int): ByteArray = ByteArray(4) { octets[it].toByte() }

    private fun ipv6(vararg words: Int): ByteArray = ByteArray(16) { index ->
        val word = words[index / 2]
        (if (index % 2 == 0) word shr 8 else word and 0xFF).toByte()
    }

    @Test
    fun publicHostsStayAllowed() {
        listOf(
            "example.com", "i.imgur.com", "cdn.7tv.app", "123.example.com", "1e100.net", "0x0.st",
            "8.8.8.8", "1.1.1.1", "2606:4700:4700::1111", "[2001:4860:4860::8888]", "example.com."
        ).forEach { assertTrue(OutboundUrlPolicy.isPublicHost(it), it) }
    }

    @Test
    fun shorthandAndDisguisedIpv4IsRejected() {
        listOf(
            "127.1", "127.0.1", "10.1", "192.168.1", "0x7f.0.0.1", "0x7f000001", "2130706433",
            "127..1", "01.2.3.4", "8.8.8.08", "127.0.0.1.", "１２７.０.０.１", "127。0。0。1"
        ).forEach { assertFalse(OutboundUrlPolicy.isPublicHost(it), it) }
    }

    @Test
    fun privateIpv4LiteralsAreRejected() {
        listOf(
            "127.0.0.1", "10.0.0.5", "172.16.0.1", "172.31.255.255", "192.168.1.1", "169.254.169.254",
            "0.0.0.0", "100.64.0.1", "198.18.0.1", "224.0.0.1", "255.255.255.255"
        ).forEach { assertFalse(OutboundUrlPolicy.isPublicHost(it), it) }
    }

    @Test
    fun privateAndMalformedIpv6LiteralsAreRejected() {
        listOf(
            "::1", "[::1]", "0:0:0:0:0:0:0:1", "::", "::ffff:127.0.0.1", "::ffff:7f00:1", "::127.0.0.1",
            "fe80::1", "fe80::1%25en0", "fc00::1", "fd12:3456::1", "ff02::1", "64:ff9b::10.0.0.1",
            "1::2::3", "gggg::1", "12345::1", ":::1"
        ).forEach { assertFalse(OutboundUrlPolicy.isPublicHost(it), it) }
    }

    @Test
    fun reservedNamesAreRejected() {
        listOf(
            "localhost", "LOCALHOST", "api.localhost", "printer.local", "printer.local.",
            "metadata.google.internal", "router.home.arpa", "intranet", ""
        ).forEach { assertFalse(OutboundUrlPolicy.isPublicHost(it), it) }
    }

    @Test
    fun fetchPolicyAppliesToWholeUrls() {
        assertTrue(OutboundUrlPolicy.isFetchAllowed("https://example.com/a.png"))
        assertTrue(OutboundUrlPolicy.isFetchAllowed("http://8.8.8.8/"))
        assertFalse(OutboundUrlPolicy.isFetchAllowed("http://127.1:11434/api/tags"))
        assertFalse(OutboundUrlPolicy.isFetchAllowed("http://192.168.1/"))
        assertFalse(OutboundUrlPolicy.isFetchAllowed("http://127.0.0.1.:8080/"))
        assertFalse(OutboundUrlPolicy.isFetchAllowed("http://[::1]:8080/"))
    }

    @Test
    fun resolvedAddressesKeepFakeIpAndCgnatWorking() {
        listOf(
            ipv4(8, 8, 8, 8), ipv4(198, 18, 0, 1), ipv4(198, 19, 255, 254), ipv4(100, 64, 0, 1),
            ipv4(100, 100, 100, 100), ipv6(0x2606, 0x4700, 0x4700, 0, 0, 0, 0, 0x1111),
            ipv6(0xfc00, 0, 0, 0, 0, 0, 0, 1), ipv6(0x64, 0xff9b, 0, 0, 0, 0, 0x0808, 0x0808)
        ).forEach { assertTrue(OutboundUrlPolicy.isPublicResolvedAddress(it), it.joinToString()) }
    }

    @Test
    fun resolvedPrivateAddressesAreRejected() {
        listOf(
            ipv4(127, 0, 0, 1), ipv4(10, 0, 0, 1), ipv4(172, 16, 5, 4), ipv4(192, 168, 0, 1),
            ipv4(169, 254, 169, 254), ipv4(0, 0, 0, 0), ipv4(224, 0, 0, 251),
            ipv6(0, 0, 0, 0, 0, 0, 0, 1), ipv6(0, 0, 0, 0, 0, 0, 0, 0), ipv6(0xfe80, 0, 0, 0, 0, 0, 0, 1),
            ipv6(0, 0, 0, 0, 0, 0xffff, 0x7f00, 1), ipv6(0xff02, 0, 0, 0, 0, 0, 0, 1),
            ipv6(0x64, 0xff9b, 0, 0, 0, 0, 0xc0a8, 0x0001), ByteArray(0)
        ).forEach { assertFalse(OutboundUrlPolicy.isPublicResolvedAddress(it), it.joinToString()) }
    }
}
