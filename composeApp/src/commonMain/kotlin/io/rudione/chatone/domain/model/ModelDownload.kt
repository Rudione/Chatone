package io.rudione.chatone.domain.model

sealed interface ModelDownloadState {
    data object Queued : ModelDownloadState
    data class Running(
        val percent: Int,
        val completedBytes: Long,
        val totalBytes: Long,
        val phase: Phase,
        val bytesPerSec: Long = 0,
        val etaSeconds: Long = -1
    ) : ModelDownloadState
    data object Completed : ModelDownloadState
    data class Failed(val message: String) : ModelDownloadState

    enum class Phase { DOWNLOADING, VERIFYING, RETRYING }
}

data class ModelDownload(
    val model: String,
    val state: ModelDownloadState
)
