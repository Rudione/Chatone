package io.rudione.chatone.data.auth

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WebLoginSessionTest {

    @Test
    fun loginUrlCarriesASessionKey() {
        val url = WebLoginSession().begin("https://app.chatone.im/auth/")
        val key = url.substringAfter("#k=").substringBefore('&')
        assertTrue(Regex("^[A-Za-z0-9_-]{43}$").matches(key), url)
    }

    @Test
    fun everyLoginOpensAFreshPage() {
        val session = WebLoginSession()
        val first = session.begin("https://app.chatone.im/auth/")
        val second = session.begin("https://app.chatone.im/auth/")
        assertTrue(first.startsWith("https://app.chatone.im/auth/?s="), first)
        assertNotEquals(first.substringBefore('#'), second.substringBefore('#'))
    }
}
