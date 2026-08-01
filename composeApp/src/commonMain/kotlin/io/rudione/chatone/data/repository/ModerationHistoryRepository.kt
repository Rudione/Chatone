package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.local.ChatoneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

data class ModerationHistoryEntry(
    val id: String,
    val action: String,
    val durationSeconds: Int?,
    val reason: String?,
    val moderatorLogin: String?,
    val timestamp: Long
)

class ModerationHistoryRepository(
    private val database: ChatoneDatabase,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "ModerationHistory"

        const val ACTION_BAN = "ban"
        const val ACTION_TIMEOUT = "timeout"
        const val ACTION_WARN = "warn"
    }

    fun recordEvent(
        channelId: String,
        targetUserId: String,
        targetLogin: String,
        action: String,
        durationSeconds: Int? = null,
        reason: String? = null,
        moderatorLogin: String? = null
    ) {
        if (channelId.isBlank() || targetUserId.isBlank()) return
        val timestamp = Clock.System.now().toEpochMilliseconds()
        scope.launch {
            try {
                database.moderationEventQueries.insertEvent(
                    id = "${channelId}_${targetUserId}_$timestamp",
                    channelId = channelId,
                    targetUserId = targetUserId,
                    targetLogin = targetLogin,
                    action = action,
                    durationSeconds = durationSeconds?.toLong(),
                    reason = reason,
                    moderatorLogin = moderatorLogin,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                Napier.w("Failed to record moderation event: ${e.message}", tag = TAG)
            }
        }
    }

    fun getHistoryForUser(channelId: String, targetUserId: String): List<ModerationHistoryEntry> {
        if (channelId.isBlank() || targetUserId.isBlank()) return emptyList()
        return try {
            database.moderationEventQueries.getEventsForUser(channelId, targetUserId)
                .executeAsList()
                .map {
                    ModerationHistoryEntry(
                        id = it.id,
                        action = it.action,
                        durationSeconds = it.durationSeconds?.toInt(),
                        reason = it.reason,
                        moderatorLogin = it.moderatorLogin,
                        timestamp = it.timestamp
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
