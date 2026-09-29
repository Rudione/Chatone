package io.rudione.chatone.data.remote.proxy

import com.sun.net.httpserver.HttpServer
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrivateNetworkGuardTest {

    private lateinit var server: HttpServer

    private val loopbackUrl: String
        get() = "http://127.0.0.1:${server.address.port}/"

    @BeforeTest
    fun startServer() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0).apply {
            createContext("/") { exchange ->
                val body = "ok".toByteArray()
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            start()
        }
    }

    @AfterTest
    fun stopServer() {
        server.stop(0)
    }

    @Test
    fun regularClientStillReachesLocalServices() {
        runBlocking {
            buildHttpClientWithProxy(proxy = null).use { client ->
                assertEquals(HttpStatusCode.OK, client.get(loopbackUrl).status)
            }
        }
    }

    @Test
    fun guardedClientRefusesLocalServices() {
        runBlocking {
            buildHttpClientWithProxy(proxy = null, blockPrivateNetworks = true).use { client ->
                assertFailsWith<IOException> { client.get(loopbackUrl) }
            }
        }
    }
}
