package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.ChatoneDatabase
import kotlinx.datetime.Clock

class UserNoteRepository(private val database: ChatoneDatabase) {

    fun getNote(twitchUserId: String): String? {
        return try {
            database.userNoteQueries.getNote(twitchUserId).executeAsOneOrNull()?.note
        } catch (_: Exception) {
            null
        }
    }

    fun saveNote(twitchUserId: String, note: String) {
        database.userNoteQueries.upsertNote(
            twitchUserId = twitchUserId,
            note = note,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }

    fun deleteNote(twitchUserId: String) {
        database.userNoteQueries.deleteNote(twitchUserId)
    }
}
