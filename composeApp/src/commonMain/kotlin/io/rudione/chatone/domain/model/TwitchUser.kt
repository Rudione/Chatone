package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TwitchUser(
    val id: String,
    val login: String,
    val displayName: String,
    val profileImageUrl: String,
    val broadcasterType: String = "",
    val description: String = "",
    val createdAt: String = ""
)

@Serializable
data class TwitchAccount(
    val userId: String,
    val login: String,
    val displayName: String,
    val profileImageUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val scopes: List<String>
)
