package io.rudione.chatone.domain.model

data class ChatViewerRelationship(
    val followedAtEpochMs: Long?,
    val subscriptionTier: Int
)
