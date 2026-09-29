package io.rudione.chatone.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginSiteResolverTest {

    private val primary = "https://app.example.test/auth/"
    private val mirror = "https://ru-app.example.test/auth/"

    private fun resolver(reachable: Set<String>) = LoginSiteResolver(
        httpClient = HttpClient(MockEngine { request ->
            val url = request.url.toString()
            if (url in reachable) respond("ok", HttpStatusCode.OK)
            else throw IllegalStateException("unreachable $url")
        }),
        primaryUrl = primary,
        mirrorUrl = mirror
    )

    @Test
    fun primarySiteWinsWhenReachable() = runTest {
        assertEquals(primary, withContext(Dispatchers.Default) { resolver(setOf(primary, mirror)).resolve() })
    }

    @Test
    fun mirrorIsUsedWhenPrimaryIsBlocked() = runTest {
        assertEquals(mirror, withContext(Dispatchers.Default) { resolver(setOf(mirror)).resolve() })
    }

    @Test
    fun primaryStaysWhenNothingAnswers() = runTest {
        assertEquals(primary, withContext(Dispatchers.Default) { resolver(emptySet()).resolve() })
    }

    @Test
    fun alternateSwapsBetweenSitesKeepingTheFragment() {
        val resolver = resolver(emptySet())

        assertEquals("${mirror}#k=abc&d=CODE", resolver.alternateOf("${primary}#k=abc&d=CODE"))
        assertEquals("${primary}#k=abc", resolver.alternateOf("${mirror}#k=abc"))
        assertNull(resolver.alternateOf("https://elsewhere.test/#k=abc"))
    }
}
