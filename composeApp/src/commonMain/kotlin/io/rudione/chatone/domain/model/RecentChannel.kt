package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecentChannel(
    val login: String,
    val displayName: String = login,
    val avatarUrl: String = "",
    val openedAtMs: Long = 0L
)
