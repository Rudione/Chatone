package io.rudione.chatone.data.auth

import io.github.aakira.napier.Napier
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

internal class LoopbackAuthServer(
    private val port: Int,
    private val onToken: (token: String, state: String) -> Boolean
) {
    private var serverSocket: ServerSocket? = null
    private var worker: Thread? = null

    fun start(): Boolean {
        stop()
        return try {
            val socket = ServerSocket(port, 4, InetAddress.getByName("127.0.0.1"))
            serverSocket = socket
            worker = Thread({ acceptLoop(socket) }, "chatone-oauth-loopback").apply {
                isDaemon = true
                start()
            }
            Napier.d("Loopback auth server listening on 127.0.0.1:$port", tag = TAG)
            true
        } catch (e: Exception) {
            Napier.e("Loopback auth server failed to bind port $port: ${e.message}", e, tag = TAG)
            serverSocket = null
            false
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        worker = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (_: Exception) {
                return
            }
            runCatching { handle(client) }
                .onFailure { Napier.w("Loopback request failed: ${it.message}", tag = TAG) }
            runCatching { client.close() }
        }
    }

    private fun handle(client: Socket) {
        client.soTimeout = 5000
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
        val requestLine = reader.readLine() ?: return
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
        }

        val target = requestLine.split(' ').getOrNull(1) ?: "/"
        val path = target.substringBefore('?')
        val query = target.substringAfter('?', "")
        val output = client.getOutputStream()

        when (path) {
            "/auth/callback" -> respond(output, 200, "text/html; charset=utf-8", CALLBACK_HTML)
            "/auth/token" -> {
                val params = parseQuery(query)
                val token = params["access_token"].orEmpty()
                val state = params["state"].orEmpty()
                when {
                    token.isBlank() ->
                        respond(output, 400, "text/plain; charset=utf-8", "Missing token")

                    !onToken(token, state) ->
                        respond(output, 403, "text/plain; charset=utf-8", "State mismatch")

                    else -> respond(output, 200, "text/plain; charset=utf-8", "OK")
                }
            }

            else -> respond(output, 404, "text/plain; charset=utf-8", "Not found")
        }
    }

    private fun respond(output: OutputStream, status: Int, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ").append(status).append(' ').append(statusText(status)).append("\r\n")
            append("Content-Type: ").append(contentType).append("\r\n")
            append("Content-Length: ").append(bytes.size).append("\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun statusText(status: Int): String = when (status) {
        200 -> "OK"
        400 -> "Bad Request"
        403 -> "Forbidden"
        else -> "Not Found"
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) return@mapNotNull null
            val key = part.substring(0, idx)
            val value = runCatching {
                URLDecoder.decode(part.substring(idx + 1), "UTF-8")
            }.getOrDefault("")
            key to value
        }.toMap()
    }

    companion object {
        private const val TAG = "LoopbackAuth"

        private val CALLBACK_HTML = """
            <!DOCTYPE html>
            <html><head><title>Chatone Auth</title>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
                body { background: #0E0E10; color: #EFEFF1; font-family: sans-serif;
                       display: flex; justify-content: center; align-items: center;
                       height: 100vh; margin: 0; }
                .container { text-align: center; padding: 0 24px; }
                h1 { color: #9146FF; }
                .error { color: #FF6B6B; }
            </style>
            </head><body>
            <div class="container">
                <h1>Chatone</h1>
                <p id="status">Completing authentication...</p>
            </div>
            <script>
                function finish(text, isError) {
                    var el = document.getElementById('status');
                    el.textContent = text;
                    if (isError) el.className = 'error';
                }
                function backToApp() {
                    setTimeout(function () {
                        window.location.href = 'chatone://auth/callback?done=1';
                    }, 400);
                }
                var raw = window.location.hash.substring(1);
                if (!raw) raw = window.location.search.substring(1);
                if (raw) {
                    var params = new URLSearchParams(raw);
                    var token = params.get('access_token');
                    var state = params.get('state') || '';
                    if (token) {
                        fetch('/auth/token?access_token=' + encodeURIComponent(token) +
                              '&state=' + encodeURIComponent(state))
                            .then(function (r) { return r.text(); })
                            .then(function (text) {
                                if (text === 'OK') {
                                    finish('Authentication successful! Returning to Chatone...', false);
                                    backToApp();
                                } else {
                                    finish('Chatone rejected the response: ' + text, true);
                                }
                            })
                            .catch(function (err) {
                                finish('Could not reach Chatone: ' + err, true);
                            });
                    } else {
                        finish('No access token in response: ' +
                               (params.get('error_description') || params.get('error') || 'unknown'), true);
                    }
                } else {
                    finish('Authentication failed. No token received.', true);
                }
            </script>
            </body></html>
        """.trimIndent()
    }
}
