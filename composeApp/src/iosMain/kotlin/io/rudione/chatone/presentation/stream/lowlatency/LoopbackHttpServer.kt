package io.rudione.chatone.presentation.stream.lowlatency

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import platform.posix.AF_INET
import platform.posix.EINTR
import platform.posix.POLLIN
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_NOSIGPIPE
import platform.posix.SO_RCVTIMEO
import platform.posix.SO_REUSEADDR
import platform.posix.SO_SNDTIMEO
import platform.posix.accept
import platform.posix.bind
import platform.posix.close
import platform.posix.errno
import platform.posix.getsockname
import platform.posix.listen
import platform.posix.pipe
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.socklen_tVar
import platform.posix.timeval

internal class LoopbackRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String>
)

internal class LoopbackResponse(
    val status: Int,
    val contentType: String? = null,
    val body: ByteArray = EMPTY_BODY
) {
    private companion object {
        val EMPTY_BODY = ByteArray(0)
    }
}

internal fun interface LoopbackRequestHandler {
    suspend fun handle(request: LoopbackRequest): LoopbackResponse
}

@OptIn(ExperimentalForeignApi::class)
internal class LoopbackHttpServer(
    private val handler: LoopbackRequestHandler,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private class Listener(val port: Int, val wakeFd: Int, val scope: CoroutineScope)

    private val lock = SynchronizedObject()
    private var listener: Listener? = null

    val port: Int get() = synchronized(lock) { listener?.port ?: 0 }

    fun start(): Int = synchronized(lock) {
        listener?.let { return@synchronized it.port }
        val socketFd = openLoopbackSocket() ?: return@synchronized 0
        val boundPort = boundPort(socketFd)
        val wake = openWakePipe()
        if (boundPort == 0 || wake == null) {
            close(socketFd)
            wake?.let { close(it.first); close(it.second) }
            return@synchronized 0
        }
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val created = Listener(boundPort, wake.second, scope)
        listener = created
        scope.launch {
            acceptConnections(socketFd, wake.first, scope)
            close(socketFd)
            close(wake.first)
            retire(created)
        }
        boundPort
    }

    fun stop() {
        val current = synchronized(lock) { listener.also { listener = null } } ?: return
        close(current.wakeFd)
    }

    private fun retire(finished: Listener) {
        val wasCurrent = synchronized(lock) {
            (listener === finished).also { if (it) listener = null }
        }
        if (wasCurrent) close(finished.wakeFd)
        finished.scope.cancel()
    }

    private fun openLoopbackSocket(): Int? {
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) return null
        val bound = memScoped {
            val enabled = alloc<IntVar>().apply { value = 1 }
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, enabled.ptr, sizeOf<IntVar>().convert())
            val address = alloc<sockaddr_in>().apply {
                sin_len = sizeOf<sockaddr_in>().convert()
                sin_family = AF_INET.convert()
                sin_port = 0u
                sin_addr.s_addr = LOOPBACK_ADDRESS
            }
            bind(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0 && listen(fd, BACKLOG) == 0
        }
        if (!bound) {
            close(fd)
            return null
        }
        return fd
    }

    private fun boundPort(fd: Int): Int = memScoped {
        val address = alloc<sockaddr_in>()
        val length = alloc<socklen_tVar>().apply { value = sizeOf<sockaddr_in>().convert() }
        if (getsockname(fd, address.ptr.reinterpret(), length.ptr) != 0) return@memScoped 0
        val networkOrder = address.sin_port.toInt()
        ((networkOrder and 0xFF) shl 8) or ((networkOrder shr 8) and 0xFF)
    }

    private fun openWakePipe(): Pair<Int, Int>? = memScoped {
        val ends = allocArray<IntVar>(2)
        if (pipe(ends) != 0) null else ends[0] to ends[1]
    }

    private fun acceptConnections(socketFd: Int, wakeFd: Int, scope: CoroutineScope) = memScoped {
        val watched = allocArray<pollfd>(2)
        watched[0].fd = socketFd
        watched[0].events = POLLIN.toShort()
        watched[1].fd = wakeFd
        watched[1].events = POLLIN.toShort()
        while (true) {
            watched[0].revents = 0
            watched[1].revents = 0
            if (poll(watched, 2u, -1) < 0) {
                if (errno == EINTR) continue
                break
            }
            if (watched[1].revents.toInt() != 0) break
            val events = watched[0].revents.toInt()
            if (events and POLLIN == 0) {
                if (events != 0) break
                continue
            }
            val client = accept(socketFd, null, null)
            if (client >= 0) scope.launch { serve(client) }
        }
    }

    private suspend fun serve(client: Int) {
        try {
            configure(client)
            val request = readRequest(client)
            val response = if (request == null) LoopbackResponse(HTTP_BAD_REQUEST) else respondTo(request)
            write(client, response)
        } finally {
            close(client)
        }
    }

    private suspend fun respondTo(request: LoopbackRequest): LoopbackResponse =
        try {
            handler.handle(request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            LoopbackResponse(HTTP_SERVER_ERROR)
        }

    private fun configure(client: Int) = memScoped {
        val enabled = alloc<IntVar>().apply { value = 1 }
        setsockopt(client, SOL_SOCKET, SO_NOSIGPIPE, enabled.ptr, sizeOf<IntVar>().convert())
        val timeout = alloc<timeval>().apply {
            tv_sec = IO_TIMEOUT_SECONDS
            tv_usec = 0
        }
        setsockopt(client, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())
        setsockopt(client, SOL_SOCKET, SO_SNDTIMEO, timeout.ptr, sizeOf<timeval>().convert())
    }

    private fun readRequest(client: Int): LoopbackRequest? {
        val buffer = ByteArray(MAX_REQUEST_BYTES)
        var length = 0
        while (true) {
            val headEnd = headEnd(buffer, length)
            if (headEnd >= 0) return parse(buffer.decodeToString(0, headEnd))
            if (length == buffer.size) return null
            val read = buffer.usePinned { pinned ->
                recv(client, pinned.addressOf(length), (buffer.size - length).convert(), 0)
            }.toInt()
            if (read <= 0) return null
            length += read
        }
    }

    private fun headEnd(buffer: ByteArray, length: Int): Int {
        var index = 0
        while (index + 3 < length) {
            if (buffer[index] == CR && buffer[index + 1] == LF && buffer[index + 2] == CR && buffer[index + 3] == LF) return index
            index++
        }
        return -1
    }

    private fun parse(head: String): LoopbackRequest? {
        val lines = head.split("\r\n")
        val requestLine = lines.firstOrNull()?.split(' ') ?: return null
        if (requestLine.size != 3 || !requestLine[2].startsWith(HTTP_VERSION_PREFIX)) return null
        val headers = HashMap<String, String>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf(':')
            if (separator <= 0) return null
            headers[line.substring(0, separator).trim().lowercase()] = line.substring(separator + 1).trim()
        }
        return LoopbackRequest(method = requestLine[0], path = requestLine[1], headers = headers)
    }

    private fun write(client: Int, response: LoopbackResponse) {
        val head = buildString {
            append("HTTP/1.1 ").append(response.status).append(' ').append(reasonOf(response.status)).append("\r\n")
            response.contentType?.let { append("Content-Type: ").append(it).append("\r\n") }
            append("Content-Length: ").append(response.body.size).append("\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("Connection: close\r\n\r\n")
        }.encodeToByteArray()
        val payload = head + response.body
        var sent = 0
        payload.usePinned { pinned ->
            while (sent < payload.size) {
                val written = send(client, pinned.addressOf(sent), (payload.size - sent).convert(), 0).toInt()
                if (written <= 0) return
                sent += written
            }
        }
    }

    private fun reasonOf(status: Int): String = when (status) {
        HTTP_OK -> "OK"
        HTTP_BAD_REQUEST -> "Bad Request"
        HTTP_FORBIDDEN -> "Forbidden"
        HTTP_NOT_FOUND -> "Not Found"
        HTTP_GONE -> "Gone"
        HTTP_SERVER_ERROR -> "Internal Server Error"
        HTTP_BAD_GATEWAY -> "Bad Gateway"
        else -> "Error"
    }

    companion object {
        const val HOST = "127.0.0.1"
        const val HTTP_OK = 200
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_GONE = 410
        const val HTTP_SERVER_ERROR = 500
        const val HTTP_BAD_GATEWAY = 502
        const val MAX_REQUEST_BYTES = 8_192
        private const val LOOPBACK_ADDRESS = 0x0100007Fu
        private const val BACKLOG = 16
        private const val IO_TIMEOUT_SECONDS = 5L
        private const val HTTP_VERSION_PREFIX = "HTTP/1."
        private const val CR: Byte = 13
        private const val LF: Byte = 10
    }
}
