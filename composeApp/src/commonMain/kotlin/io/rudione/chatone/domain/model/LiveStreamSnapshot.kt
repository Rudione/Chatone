package io.rudione.chatone.domain.model

data class LiveStreamSnapshot(
    val login: String,
    val displayName: String,
    val streamId: String,
    val startedAtMs: Long,
    val title: String,
    val gameName: String
)
