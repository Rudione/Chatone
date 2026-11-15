package io.rudione.chatone.presentation.stream.lowlatency

import io.rudione.chatone.presentation.stream.MediaPlaylistSource
import io.rudione.chatone.presentation.stream.MediaPlaylistTransform
import io.rudione.chatone.presentation.stream.UrlSessionPlaylistSource
import io.rudione.chatone.presentation.stream.absolutizeMediaPlaylist
import io.rudione.chatone.util.link.OutboundUrlPolicy
import io.rudione.chatone.util.security.randomHex
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException

internal class PlaylistSessionSpec(
    val masterPlaylist: String,
    val serverTimeMs: () -> Long,
    val transform: MediaPlaylistTransform,
    val onMediaStatus: (Int) -> Unit
)

internal class PlaylistSession(
    val masterUrl: String,
    private val serving: () -> Boolean,
    private val onClose: () -> Unit
) {
    val isServing: Boolean get() = serving()

    fun close() = onClose()
}

internal class StreamPlaylistGateway(
    private val source: MediaPlaylistSource = UrlSessionPlaylistSource,
    private val newToken: () -> String = { randomHex(TOKEN_BYTES) },
    serverFactory: (LoopbackRequestHandler) -> LoopbackHttpServer = { LoopbackHttpServer(it) }
) {
    private class Session(val token: String, val port: Int, val spec: PlaylistSessionSpec) {
        val authority = "${LoopbackHttpServer.HOST}:$port"
        val remotes = ArrayList<String>()
        private val masterLines: List<String>

        init {
            val base = "http://$authority/$token/"
            masterLines = spec.masterPlaylist.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith(SERVER_INFO_TAG) }
                .map { line ->
                    if (line.startsWith("#") || remotes.size >= MAX_VARIANTS || !isPlayable(line)) {
                        line
                    } else {
                        remotes += line
                        "$base${remotes.lastIndex}$PLAYLIST_SUFFIX"
                    }
                }
                .toList()
        }

        val masterUrl = "http://$authority/$token/$MASTER_NAME$PLAYLIST_SUFFIX"

        fun master(): String {
            val millis = spec.serverTimeMs()
            val fraction = ((millis % 1000) / 10).toString().padStart(2, '0')
            val serverInfo = "$SERVER_INFO_TAG$SERVER_TIME_ATTRIBUTE=\"${millis / 1000}.$fraction\""
            return buildString {
                masterLines.forEachIndexed { index, line ->
                    append(line).append('\n')
                    if (index == 0) append(serverInfo).append('\n')
                }
            }
        }

        private fun isPlayable(uri: String): Boolean = uri.startsWith("https://") && OutboundUrlPolicy.isFetchAllowed(uri)
    }

    private val lock = SynchronizedObject()
    private val sessions = HashMap<String, Session>()
    private val server = serverFactory(LoopbackRequestHandler(::handle))

    fun open(spec: PlaylistSessionSpec): PlaylistSession? {
        val session = synchronized(lock) {
            val port = server.start()
            if (port == 0) return@synchronized null
            Session(newToken(), port, spec).also { sessions[it.token] = it }
        } ?: return null
        return PlaylistSession(
            masterUrl = session.masterUrl,
            serving = { server.port == session.port },
            onClose = { close(session.token) }
        )
    }

    private fun close(token: String) {
        synchronized(lock) {
            if (sessions.remove(token) != null && sessions.isEmpty()) server.stop()
        }
    }

    private suspend fun handle(request: LoopbackRequest): LoopbackResponse {
        if (request.method != METHOD_GET || request.headers.containsKey(HEADER_ORIGIN)) return NOT_FOUND
        val match = RESOURCE_PATH.matchEntire(request.path) ?: return NOT_FOUND
        val session = synchronized(lock) { sessions[match.groupValues[1]] } ?: return NOT_FOUND
        if (request.headers[HEADER_HOST] != session.authority) return NOT_FOUND
        val resource = match.groupValues[2]
        if (resource == MASTER_NAME) return playlist(session.master())
        val remote = resource.toIntOrNull()?.let(session.remotes::getOrNull) ?: return NOT_FOUND
        return media(session, remote)
    }

    private suspend fun media(session: Session, remote: String): LoopbackResponse {
        val fetched = source.fetch(remote)
        session.spec.onMediaStatus(fetched.status)
        val body = fetched.body ?: return LoopbackResponse(upstreamFailure(fetched.status))
        val absolute = absolutizeMediaPlaylist(body, remote)
        val transformed = try {
            session.spec.transform.transform(remote, absolute)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            absolute
        }
        return playlist(transformed)
    }

    private fun playlist(text: String): LoopbackResponse =
        LoopbackResponse(LoopbackHttpServer.HTTP_OK, PLAYLIST_CONTENT_TYPE, text.encodeToByteArray())

    private fun upstreamFailure(status: Int): Int =
        if (status in CLIENT_AND_SERVER_ERRORS) status else LoopbackHttpServer.HTTP_BAD_GATEWAY

    private companion object {
        const val TOKEN_BYTES = 16
        const val MAX_VARIANTS = 64
        const val METHOD_GET = "GET"
        const val HEADER_HOST = "host"
        const val HEADER_ORIGIN = "origin"
        const val MASTER_NAME = "master"
        const val PLAYLIST_SUFFIX = ".m3u8"
        const val PLAYLIST_CONTENT_TYPE = "application/vnd.apple.mpegurl"
        const val SERVER_INFO_TAG = "#EXT-X-TWITCH-INFO:"
        const val SERVER_TIME_ATTRIBUTE = "SERVER-TIME"
        val RESOURCE_PATH = Regex("^/([0-9a-f]{32})/(master|[0-9]{1,2})\\.m3u8$")
        val CLIENT_AND_SERVER_ERRORS = 400..599
        val NOT_FOUND = LoopbackResponse(LoopbackHttpServer.HTTP_NOT_FOUND)
    }
}
