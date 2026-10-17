package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.GqlModLogAction
import io.rudione.chatone.data.remote.ModLogActionType
import io.rudione.chatone.data.remote.TwitchGqlClient

data class ModerationHistoryEntry(
    val id: String,
    val action: ModLogActionType,
    val label: String?,
    val moderatorLogin: String?,
    val timestamp: Long
)

data class ModerationHistory(
    val entries: List<ModerationHistoryEntry>,
    val counts: Map<ModLogActionType, Int>,
    val messageCount: Int?
) {
    fun count(type: ModLogActionType): Int = counts[type] ?: 0

    val totalActions: Int get() = counts.values.sum()

    companion object {
        val Empty = ModerationHistory(emptyList(), emptyMap(), null)
    }
}

class ModerationHistoryRepository(
    private val gqlClient: TwitchGqlClient,
    private val authStore: ModerationAuthStore
) {

    suspend fun loadHistoryForUser(channelId: String, targetUserId: String, accountToken: String): ModerationHistory {
        if (channelId.isBlank() || targetUserId.isBlank()) return ModerationHistory.Empty
        val token = runCatching { authStore.resolveToken(accountToken) }.getOrDefault(accountToken)
        if (token.isBlank()) return ModerationHistory.Empty
        val summary = gqlClient.getModLogSummary(channelId, targetUserId, token) ?: return ModerationHistory.Empty
        return ModerationHistory(
            entries = summary.actions.map { it.toEntry() },
            counts = summary.counts,
            messageCount = summary.messageCount
        )
    }

    private fun GqlModLogAction.toEntry() = ModerationHistoryEntry(
        id = id,
        action = type,
        label = label,
        moderatorLogin = moderatorLogin,
        timestamp = timestampMs
    )
}
