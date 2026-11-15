package io.rudione.chatone.data.remote

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.cancel

internal interface IrcSocket {
    suspend fun receive(): String?
    suspend fun send(line: String)
    fun cancel()
}

internal class KtorIrcSocket(private val session: DefaultClientWebSocketSession) : IrcSocket {
    override suspend fun receive(): String? {
        for (frame in session.incoming) {
            if (frame is Frame.Text) return frame.readText()
        }
        return null
    }

    override suspend fun send(line: String) {
        session.send(Frame.Text(line))
    }

    override fun cancel() {
        session.cancel()
    }
}
