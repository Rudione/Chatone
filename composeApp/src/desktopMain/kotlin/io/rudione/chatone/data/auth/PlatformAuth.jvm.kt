package io.rudione.chatone.data.auth

import io.github.aakira.napier.Napier
import io.rudione.chatone.util.settings.AppConfig
import io.ktor.http.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import kotlin.concurrent.Volatile

actual class PlatformAuthHandler actual constructor() {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var tokenDeferred = CompletableDeferred<String?>()
    private val secureRandom = java.security.SecureRandom()
    @Volatile
    private var expectedState: String = ""

    actual fun getClientId(): String = AppConfig.TWITCH_CLIENT_ID_DESKTOP

    actual fun getRedirectUri(): String = AppConfig.REDIRECT_URI_DESKTOP

    actual fun newAuthSession(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        val state = bytes.joinToString("") { ((it.toInt() and 0xFF) + 0x100).toString(16).substring(1) }
        expectedState = state
        return state
    }

    actual fun startAuth(authUrl: String) {

        tokenDeferred = CompletableDeferred()

        server?.stop(500, 1000)
        server = null

        Napier.d("Starting OAuth callback server on port ${AppConfig.OAUTH_CALLBACK_PORT}", tag = "PlatformAuth")
        Napier.d("Auth URL: $authUrl", tag = "PlatformAuth")

        startCallbackServer()

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(authUrl))
        } else {
            Napier.e("Desktop is not supported, cannot open browser", tag = "PlatformAuth")
        }
    }

    private fun startCallbackServer() {
        server = embeddedServer(CIO, host = "127.0.0.1", port = AppConfig.OAUTH_CALLBACK_PORT) {
            routing {

                get("/auth/callback") {
                    Napier.d("Callback page requested — serving token extractor HTML", tag = "PlatformAuth")
                    call.respondText(
                        contentType = ContentType.Text.Html,
                        text = """
                        <!DOCTYPE html>
                        <html><head><title>Chatone Auth</title>
                        <style>
                            body { background: #0E0E10; color: #EFEFF1; font-family: sans-serif;
                                   display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }
                            .container { text-align: center; }
                            h1 { color: #9146FF; }
                            .error { color: #FF6B6B; }
                            pre { text-align: left; background: #1a1a2e; padding: 12px; border-radius: 8px;
                                  font-size: 12px; max-width: 500px; overflow-wrap: break-word; }
                        </style>
                        </head><body>
                        <div class="container">
                            <h1>Chatone</h1>
                            <p id="status">Completing authentication...</p>
                            <pre id="debug"></pre>
                        </div>
                        <script>
                            var dbg = document.getElementById('debug');
                            var fragment = window.location.hash.substring(1);
                            dbg.textContent = 'Fragment: ' + (fragment || '(empty)') + '\nFull URL: ' + window.location.href;

                            if (fragment) {
                                var params = new URLSearchParams(fragment);
                                var token = params.get('access_token');
                                var state = params.get('state') || '';
                                if (token) {
                                    dbg.textContent += '\nToken found, sending to app...';
                                    fetch('/auth/token?access_token=' + encodeURIComponent(token) + '&state=' + encodeURIComponent(state))
                                        .then(function(r) { return r.text(); })
                                        .then(function(text) {
                                            document.getElementById('status').textContent =
                                                'Authentication successful! You can close this tab.';
                                            dbg.textContent += '\nServer response: ' + text;
                                        })
                                        .catch(function(err) {
                                            document.getElementById('status').textContent = 'Error sending token';
                                            document.getElementById('status').className = 'error';
                                            dbg.textContent += '\nFetch error: ' + err;
                                        });
                                } else {
                                    var error = params.get('error');
                                    var desc = params.get('error_description');
                                    document.getElementById('status').textContent =
                                        'No access token in response.';
                                    document.getElementById('status').className = 'error';
                                    dbg.textContent += '\nError: ' + (error || 'none') + '\nDescription: ' + (desc || 'none');
                                }
                            } else {

                                var qparams = new URLSearchParams(window.location.search);
                                var qtoken = qparams.get('access_token');
                                var qstate = qparams.get('state') || '';
                                if (qtoken) {
                                    fetch('/auth/token?access_token=' + encodeURIComponent(qtoken) + '&state=' + encodeURIComponent(qstate))
                                        .then(function() {
                                            document.getElementById('status').textContent =
                                                'Authentication successful! You can close this tab.';
                                        });
                                } else {
                                    document.getElementById('status').textContent =
                                        'Authentication failed. No token received.';
                                    document.getElementById('status').className = 'error';
                                }
                            }
                        </script>
                        </body></html>
                        """.trimIndent()
                    )
                }

                get("/auth/token") {
                    val token = call.parameters["access_token"]
                    val state = call.parameters["state"]
                    when {
                        token == null -> {
                            Napier.e("Callback /auth/token called without access_token param", tag = "PlatformAuth")
                            call.respondText("Missing token", status = HttpStatusCode.BadRequest)
                        }
                        expectedState.isEmpty() || state != expectedState -> {
                            Napier.e("Callback state mismatch — rejecting token (possible CSRF)", tag = "PlatformAuth")
                            call.respondText("State mismatch", status = HttpStatusCode.Forbidden)
                        }
                        else -> {
                            Napier.d("OAuth token received via callback server (${token.take(8)}...)", tag = "PlatformAuth")
                            tokenDeferred.complete(token)
                            call.respondText("OK")
                        }
                    }
                }

                get("{...}") {
                    val path = call.request.local.uri
                    Napier.w("Unexpected request to callback server: $path", tag = "PlatformAuth")
                    call.respondText("Not found: $path", status = HttpStatusCode.NotFound)
                }
            }
        }.start(wait = false)
    }

    actual suspend fun awaitToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                tokenDeferred.await()
            } catch (e: Exception) {
                Napier.e("Failed to await token: ${e.message}", e, tag = "PlatformAuth")
                null
            }
        }
    }

    actual fun cleanup() {
        server?.stop(1000, 2000)
        server = null
    }
}
