package io.rudione.chatone.presentation.stream.lowlatency

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.posix.AF_INET
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_NOSIGPIPE
import platform.posix.SO_RCVTIMEO
import platform.posix.close
import platform.posix.connect
import platform.posix.recv
import platform.posix.send
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socket
import platform.posix.timeval
import platform.posix.usleep

internal class LoopbackReply(val status: Int, val headers: Map<String, String>, val body: String)

@OptIn(ExperimentalForeignApi::class)
internal object LoopbackClient {
    private const val LOOPBACK_ADDRESS = 0x0100007Fu
    private const val READ_TIMEOUT_SECONDS = 5L
    private const val REFUSAL_ATTEMPTS = 100
    private const val REFUSAL_RETRY_MICROS = 20_000u

    fun get(port: Int, path: String, headers: Map<String, String> = mapOf("Host" to "127.0.0.1:$port")): LoopbackReply? =
        exchange(port, request("GET", path, headers))

    fun request(method: String, path: String, headers: Map<String, String>): String = buildString {
        append(method).append(' ').append(path).append(" HTTP/1.1\r\n")
        headers.forEach { (name, value) -> append(name).append(": ").append(value).append("\r\n") }
        append("\r\n")
    }

    fun awaitRefused(port: Int): Boolean {
        repeat(REFUSAL_ATTEMPTS) {
            val fd = socket(AF_INET, SOCK_STREAM, 0)
            val connected = fd >= 0 && connect(fd, port)
            if (fd >= 0) close(fd)
            if (!connected) return true
            usleep(REFUSAL_RETRY_MICROS)
        }
        return false
    }

    fun exchange(port: Int, payload: String): LoopbackReply? {
        val fd = socket(AF_INET, SOCK_STREAM, 0)
        if (fd < 0) return null
        try {
            if (!connect(fd, port)) return null
            val bytes = payload.encodeToByteArray()
            var sent = 0
            bytes.usePinned { pinned ->
                while (sent < bytes.size) {
                    val written = send(fd, pinned.addressOf(sent), (bytes.size - sent).convert(), 0).toInt()
                    if (written <= 0) break
                    sent += written
                }
            }
            return parse(readAll(fd))
        } finally {
            close(fd)
        }
    }

    private fun connect(fd: Int, port: Int): Boolean = memScoped {
        val enabled = alloc<IntVar>().apply { value = 1 }
        setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, enabled.ptr, sizeOf<IntVar>().convert())
        val timeout = alloc<timeval>().apply {
            tv_sec = READ_TIMEOUT_SECONDS
            tv_usec = 0
        }
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, timeout.ptr, sizeOf<timeval>().convert())
        val address = alloc<sockaddr_in>().apply {
            sin_len = sizeOf<sockaddr_in>().convert()
            sin_family = AF_INET.convert()
            sin_port = (((port and 0xFF) shl 8) or ((port shr 8) and 0xFF)).toUShort()
            sin_addr.s_addr = LOOPBACK_ADDRESS
        }
        connect(fd, address.ptr.reinterpret(), sizeOf<sockaddr_in>().convert()) == 0
    }

    private fun readAll(fd: Int): String {
        val chunk = ByteArray(8_192)
        var received = ByteArray(0)
        while (true) {
            val read = chunk.usePinned { pinned -> recv(fd, pinned.addressOf(0), chunk.size.convert(), 0) }.toInt()
            if (read <= 0) break
            received += chunk.copyOf(read)
        }
        return received.decodeToString()
    }

    private fun parse(response: String): LoopbackReply? {
        val headEnd = response.indexOf("\r\n\r\n")
        if (headEnd < 0) return null
        val lines = response.substring(0, headEnd).split("\r\n")
        val status = lines.first().split(' ').getOrNull(1)?.toIntOrNull() ?: return null
        val headers = lines.drop(1).associate { line ->
            line.substringBefore(':').trim().lowercase() to line.substringAfter(':').trim()
        }
        return LoopbackReply(status, headers, response.substring(headEnd + 4))
    }
}
