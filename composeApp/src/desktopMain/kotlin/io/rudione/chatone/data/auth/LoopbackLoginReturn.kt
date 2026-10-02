package io.rudione.chatone.data.auth

import io.github.aakira.napier.Napier
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.rudione.chatone.util.settings.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LoopbackLoginReturn(
    private val allowedOrigins: Set<String> = AppConfig.LOGIN_SITE_ORIGINS,
    private val idleTimeoutMs: Long = IDLE_TIMEOUT_MS
) : LoginReturnChannel {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var server: EmbeddedServer<*, *>? = null
    private var expiry: Job? = null

    override suspend fun open(handler: LoginReturnHandler): LoginReturnTarget? {
        close()
        val port = AtomicInteger(0)
        val created = createServer(handler, port)
        withContext(Dispatchers.IO) { created.start(wait = false) }
        port.set(created.engine.resolvedConnectors().first().port)
        server = created
        expiry = scope.launch {
            delay(idleTimeoutMs)
            close()
        }
        return LoginReturnTarget.Loopback(port.get())
    }

    private fun createServer(handler: LoginReturnHandler, port: AtomicInteger): EmbeddedServer<*, *> {
        val used = AtomicBoolean(false)
        return embeddedServer(CIO, host = LOOPBACK_HOST, port = 0) {
            routing {
                post(RETURN_PATH) {
                    val origin = call.request.headers[HttpHeaders.Origin]
                    if (!call.isLoopbackHost(port.get()) || origin == null || origin !in allowedOrigins) {
                        return@post call.reject()
                    }
                    if ((call.request.contentLength() ?: Long.MAX_VALUE) > MAX_BODY_BYTES) return@post call.reject()
                    if (!call.request.contentType().match(ContentType.Application.FormUrlEncoded)) {
                        return@post call.reject()
                    }
                    val form = call.receiveParameters()
                    val lang = if (form["lang"] == "en") "en" else "ru"
                    if (used.get()) return@post call.redirect(resultUrl(origin, Result.Error, lang))
                    when (val outcome = handler.accept(form["line"].orEmpty())) {
                        LoginReturnOutcome.Done -> {
                            used.set(true)
                            call.redirect(resultUrl(origin, Result.Done, lang))
                            scheduleClose()
                        }

                        is LoginReturnOutcome.Continue -> {
                            used.set(true)
                            val next = outcome.url.takeIf(::isTwitchActivationUrl)
                                ?: resultUrl(origin, Result.Pending, lang)
                            call.redirect(next)
                            scheduleClose()
                        }

                        LoginReturnOutcome.Rejected -> call.redirect(resultUrl(origin, Result.Error, lang))
                    }
                }
            }
        }
    }

    override fun close() {
        expiry?.cancel()
        expiry = null
        val running = server ?: return
        server = null
        scope.launch {
            runCatching { running.stop(STOP_GRACE_MS, STOP_TIMEOUT_MS) }
                .onFailure { Napier.w("Login return server did not stop cleanly: ${it.message}", tag = TAG) }
        }
    }

    private fun scheduleClose() {
        expiry?.cancel()
        expiry = scope.launch {
            delay(CLOSE_AFTER_HANDOFF_MS)
            close()
        }
    }

    private fun resultUrl(origin: String, result: Result, lang: String): String =
        "$origin$RESULT_PATH?result=${result.value}&lang=$lang"

    private fun ApplicationCall.isLoopbackHost(port: Int): Boolean =
        port != 0 && request.headers[HttpHeaders.Host] == "$LOOPBACK_HOST:$port"

    private suspend fun ApplicationCall.reject() {
        respondText("", status = HttpStatusCode.Forbidden)
    }

    private suspend fun ApplicationCall.redirect(url: String) {
        response.header(HttpHeaders.Location, url)
        response.header("Referrer-Policy", "no-referrer")
        response.header("Cache-Control", "no-store")
        respondText("", status = HttpStatusCode.SeeOther)
    }

    private enum class Result(val value: String) {
        Done("done"),
        Pending("pending"),
        Error("error")
    }

    private companion object {
        const val TAG = "LoginReturn"
        const val LOOPBACK_HOST = "127.0.0.1"
        const val RETURN_PATH = "/chatone/login"
        const val RESULT_PATH = "/auth/done/"
        const val MAX_BODY_BYTES = 8_192L
        const val IDLE_TIMEOUT_MS = 15 * 60_000L
        const val CLOSE_AFTER_HANDOFF_MS = 5_000L
        const val STOP_GRACE_MS = 200L
        const val STOP_TIMEOUT_MS = 1_000L
    }
}
