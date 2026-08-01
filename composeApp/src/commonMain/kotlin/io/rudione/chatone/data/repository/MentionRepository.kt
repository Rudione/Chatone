package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.domain.model.MentionEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class MentionRepository(private val database: ChatoneDatabase) {

    suspend fun saveMention(entry: MentionEntry) = withContext(Dispatchers.IO) {
        try {
            database.mentionQueries.insertMention(
                id = entry.messageId,
                channelLogin = entry.channelLogin,
                fromUsername = entry.fromUsername,
                fromDisplayName = entry.fromDisplayName,
                fromColor = entry.fromColor,
                text = entry.text,
                timestamp = entry.timestamp,
                isRead = if (entry.isRead) 1L else 0L
            )
        } catch (_: Exception) {}
    }

    suspend fun loadMentions(): List<MentionEntry> = withContext(Dispatchers.IO) {
        try {
            database.mentionQueries.getMentions().executeAsList().map { row ->
                MentionEntry(
                    messageId = row.id,
                    channelLogin = row.channelLogin,
                    fromUsername = row.fromUsername,
                    fromDisplayName = row.fromDisplayName,
                    fromColor = row.fromColor,
                    text = row.text,
                    timestamp = row.timestamp,
                    isRead = row.isRead == 1L
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    suspend fun markAsRead(messageId: String) = withContext(Dispatchers.IO) {
        try { database.mentionQueries.markAsRead(messageId) } catch (_: Exception) {}
    }

    suspend fun markAllAsRead() = withContext(Dispatchers.IO) {
        try { database.mentionQueries.markAllAsRead() } catch (_: Exception) {}
    }

    suspend fun pruneOld(keepMs: Long = 7 * 24 * 60 * 60 * 1000L) = withContext(Dispatchers.IO) {
        try {
            val cutoff = Clock.System.now().toEpochMilliseconds() - keepMs
            database.mentionQueries.deleteMentionsOlderThan(cutoff)
        } catch (_: Exception) {}
    }

    suspend fun countUnread(): Long = withContext(Dispatchers.IO) {
        try { database.mentionQueries.countUnread().executeAsOne() } catch (_: Exception) { 0L }
    }
}
