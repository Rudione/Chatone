package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.data.remote.GqlTargetedModAction
import io.rudione.chatone.data.remote.TwitchGqlClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
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
    private val scope: CoroutineScope,
    private val gqlClient: TwitchGqlClient
) {

    companion object {
        private const val TAG = "ModerationHistory"

        const val ACTION_BAN = "ban"
        const val ACTION_TIMEOUT = "timeout"
        const val ACTION_WARN = "warn"
        const val ACTION_UNBAN = "unban"
        const val ACTION_UNTIMEOUT = "untimeout"
        const val ACTION_DELETE = "delete"

        private const val MATCH_WINDOW_MS = 15_000L
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

    suspend fun loadHistoryForUser(
        channelId: String,
        targetUserId: String,
        token: String
    ): List<ModerationHistoryEntry> {
        val local = getHistoryForUser(channelId, targetUserId)
        if (token.isBlank()) return local
        val remote = gqlClient.getTargetedModActions(channelId, targetUserId, token)
            ?.mapNotNull { it.toEntry() }
            ?: return local
        val unmatchedLocal = local.toMutableList()
        val merged = remote.map { entry ->
            val twin = unmatchedLocal.firstOrNull {
                it.action == entry.action && abs(it.timestamp - entry.timestamp) <= MATCH_WINDOW_MS
            }
            if (twin != null) {
                unmatchedLocal.remove(twin)
                twin
            } else entry
        }
        return (merged + unmatchedLocal).sortedByDescending { it.timestamp }
    }

    private fun GqlTargetedModAction.toEntry(): ModerationHistoryEntry? {
        val action = when (type.uppercase()) {
            "BAN" -> ACTION_BAN
            "TIMEOUT" -> ACTION_TIMEOUT
            "UNBAN" -> ACTION_UNBAN
            "UNTIMEOUT" -> ACTION_UNTIMEOUT
            "WARN" -> ACTION_WARN
            else -> return null
        }
        return ModerationHistoryEntry(
            id = "gql_$id",
            action = action,
            durationSeconds = null,
            reason = null,
            moderatorLogin = null,
            timestamp = timestampMs ?: return null
        )
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
