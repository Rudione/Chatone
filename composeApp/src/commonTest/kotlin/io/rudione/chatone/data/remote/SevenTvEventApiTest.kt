package io.rudione.chatone.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.websocket.WebSockets
import io.rudione.chatone.data.remote.emote.SevenTvEventApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SevenTvEventApiTest {
    @Test
    fun connectingPreservesCancellation() = runTest {
        val client = HttpClient(MockEngine { throw CancellationException("cancelled") }) {
            install(WebSockets)
        }
        try {
            assertFailsWith<CancellationException> {
                SevenTvEventApi(client, backgroundScope).connect()
            }
        } finally {
            client.close()
        }
    }
}
