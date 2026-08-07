package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.data.remote.GqlModerationAction
import io.rudione.chatone.data.remote.TwitchGqlClient
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

        private const val DEFAULT_SYNC_PAGES = 8
        private const val SYNC_THROTTLE_MS = 60_000L
    }

    private val syncedAt = mutableMapOf<String, Long>()
    private val seenActionIds = mutableMapOf<String, MutableSet<String>>()

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

    private fun storeAction(channelId: String, action: GqlModerationAction) {
        val mapped = when (action.kind) {
            GqlModerationAction.KIND_BAN -> ACTION_BAN
            GqlModerationAction.KIND_TIMEOUT -> ACTION_TIMEOUT
            GqlModerationAction.KIND_WARN -> ACTION_WARN
            GqlModerationAction.KIND_UNBAN -> ACTION_UNBAN
            GqlModerationAction.KIND_UNTIMEOUT -> ACTION_UNTIMEOUT
            GqlModerationAction.KIND_DELETE -> ACTION_DELETE
            else -> return
        }
        if (action.targetUserId.isBlank()) return
        val reason = action.text
            .substringAfter("reason:", "")
            .ifBlank { action.text.substringAfter("Причина:", "") }
            .trim()
            .ifBlank { null }
        try {
            database.moderationEventQueries.insertEvent(
                id = "gql_${action.id}",
                channelId = channelId,
                targetUserId = action.targetUserId,
                targetLogin = action.targetLogin,
                action = mapped,
                durationSeconds = action.durationSeconds?.toLong(),
                reason = reason,
                moderatorLogin = action.moderatorLogin,
                timestamp = action.createdAtEpochMs ?: Clock.System.now().toEpochMilliseconds()
            )
        } catch (e: Exception) {
            Napier.w("Failed to store GQL moderation action: ${e.message}", tag = TAG)
        }
    }

    suspend fun syncFromTwitch(
        channelId: String,
        token: String,
        maxPages: Int = DEFAULT_SYNC_PAGES
    ): Boolean {
        if (channelId.isBlank() || token.isBlank()) return false
        val now = Clock.System.now().toEpochMilliseconds()
        val lastSync = syncedAt[channelId]
        if (lastSync != null && now - lastSync < SYNC_THROTTLE_MS) return false
        syncedAt[channelId] = now

        val seen = seenActionIds.getOrPut(channelId) { mutableSetOf() }
        var cursor: String? = null
        var pages = 0
        var stored = 0
        while (pages < maxPages) {
            val page = gqlClient.getModerationActionLogs(channelId, cursor, token) ?: break
            var newOnThisPage = 0
            page.actions.forEach { action ->
                if (seen.add(action.id)) {
                    storeAction(channelId, action)
                    stored++
                    newOnThisPage++
                }
            }
            pages++
            cursor = page.nextCursor
            if (newOnThisPage == 0 || !page.hasNextPage || cursor.isNullOrBlank()) break
        }
        if (pages > 0) {
            Napier.d("Synced $stored moderation actions over $pages page(s) for $channelId", tag = TAG)
        }
        return pages > 0
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
