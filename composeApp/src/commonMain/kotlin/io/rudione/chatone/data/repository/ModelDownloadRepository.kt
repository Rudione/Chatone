package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.AiProviders
import io.rudione.chatone.data.remote.OllamaClient
import io.rudione.chatone.domain.model.ModelDownload
import io.rudione.chatone.domain.model.ModelDownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock

class ModelDownloadRepository(
    private val ollama: OllamaClient,
    private val settings: Settings,
    private val scope: CoroutineScope
) {
    private val baseUrl = AiProviders.LOCAL_BASE_URL

    private val _downloads = MutableStateFlow<Map<String, ModelDownload>>(emptyMap())
    val downloads: StateFlow<Map<String, ModelDownload>> = _downloads.asStateFlow()

    private val jobs = mutableMapOf<String, Job>()

    init {
        pending().forEach { start(it) }
    }

    fun start(model: String) {
        if (model.isBlank()) return
        if (jobs[model]?.isActive == true) return
        addPending(model)
        setState(model, ModelDownloadState.Queued)

        jobs[model] = scope.launch {
            var attempt = 0
            var lastPercent = 0
            while (isActive) {
                var lastBytes = 0L
                var lastTimeMs = 0L
                var speedEma = 0.0
                try {
                    var sawSuccess = false
                    ollama.pull(baseUrl, model).collect { ev ->
                        ev.error?.let { throw IllegalStateException(it) }
                        if (ev.isSuccess) sawSuccess = true
                        val percent = if (ev.total > 0)
                            ((ev.completed * 100) / ev.total).toInt().coerceIn(0, 100)
                        else lastPercent
                        lastPercent = percent
                        val phase = if (ev.isVerifying) ModelDownloadState.Phase.VERIFYING
                        else ModelDownloadState.Phase.DOWNLOADING

                        val now = Clock.System.now().toEpochMilliseconds()
                        if (lastTimeMs == 0L) {
                            lastTimeMs = now
                            lastBytes = ev.completed
                        } else if (ev.completed > lastBytes) {
                            val dtMs = now - lastTimeMs
                            if (dtMs >= 500) {
                                val instant = (ev.completed - lastBytes) * 1000.0 / dtMs
                                speedEma = if (speedEma <= 0.0) instant else speedEma * 0.6 + instant * 0.4
                                lastBytes = ev.completed
                                lastTimeMs = now
                            }
                        }
                        val bytesPerSec = speedEma.toLong()
                        val eta = if (bytesPerSec > 0 && ev.total > ev.completed)
                            (ev.total - ev.completed) / bytesPerSec else -1L

                        setState(model, ModelDownloadState.Running(percent, ev.completed, ev.total, phase, bytesPerSec, eta))
                    }

                    val done = sawSuccess || ollama.installedModels(baseUrl).any { it.name == model }
                    if (done) {
                        setState(model, ModelDownloadState.Completed)
                        removePending(model)
                        return@launch
                    }
                    throw IllegalStateException("pull stream ended before completion")
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    attempt++
                    Napier.w("Model pull '$model' attempt $attempt failed: ${e.message}", tag = TAG)
                    if (attempt >= MAX_ATTEMPTS) {
                        setState(model, ModelDownloadState.Failed(e.message ?: "download failed"))
                        return@launch
                    }
                    setState(
                        model,
                        ModelDownloadState.Running(lastPercent, 0, 0, ModelDownloadState.Phase.RETRYING)
                    )
                    delay(minOf(30_000L, 2_000L * attempt))
                }
            }
        }
    }

    fun cancel(model: String) {
        jobs.remove(model)?.cancel()
        removePending(model)
        _downloads.update { it - model }
    }

    fun retry(model: String) = start(model)

    fun clear(model: String) {
        if (jobs[model]?.isActive == true) return
        _downloads.update { it - model }
    }

    private fun setState(model: String, state: ModelDownloadState) {
        _downloads.update { it + (model to ModelDownload(model, state)) }
    }

    private fun pending(): Set<String> =
        settings.getStringOrNull(KEY_PENDING)
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet()
            .orEmpty()

    private fun addPending(model: String) {
        settings.putString(KEY_PENDING, (pending() + model).joinToString(","))
    }

    private fun removePending(model: String) {
        settings.putString(KEY_PENDING, (pending() - model).joinToString(","))
    }

    companion object {
        private const val TAG = "ModelDownload"
        private const val KEY_PENDING = "ai_pending_pulls"
        private const val MAX_ATTEMPTS = 20
    }
}
