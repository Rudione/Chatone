package io.rudione.chatone.domain.moderation

data class UnbanTarget(val userId: String = "", val login: String = "")

data class UnbannedUser(val userId: String, val login: String)
