package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UploadHistoryEntry(
    val link: String,
    val fileName: String,
    val uploadedAtMs: Long
)

@Serializable
data class UploaderPreset(
    val id: String,
    val name: String,
    val config: ImageUploaderConfig
)

object UploadRetention {
    const val BUTTON_MS = 60_000L
    const val HISTORY_MS = 24 * 60 * 60_000L
    const val MAX_ENTRIES = 100

    fun retained(entries: List<UploadHistoryEntry>, nowMs: Long): List<UploadHistoryEntry> =
        entries.filter { it.uploadedAtMs <= nowMs && nowMs - it.uploadedAtMs < HISTORY_MS }
            .sortedByDescending { it.uploadedAtMs }
            .distinctBy { it.link }
            .take(MAX_ENTRIES)
}
