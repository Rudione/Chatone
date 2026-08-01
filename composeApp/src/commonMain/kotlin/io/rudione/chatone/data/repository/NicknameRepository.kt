package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.ChatoneDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

class NicknameRepository(private val database: ChatoneDatabase) {

    private val _nicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val nicknames: StateFlow<Map<String, String>> = _nicknames

    init {
        reload()
    }

    private fun reload() {
        _nicknames.value = try {
            database.userNicknameQueries.getAllNicknames().executeAsList()
                .filter { it.nickname.isNotBlank() }
                .associate { it.twitchUserId to it.nickname }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun getNickname(twitchUserId: String): String? = _nicknames.value[twitchUserId]

    fun saveNickname(twitchUserId: String, nickname: String) {
        if (nickname.isBlank()) {
            deleteNickname(twitchUserId)
            return
        }
        database.userNicknameQueries.upsertNickname(
            twitchUserId = twitchUserId,
            nickname = nickname.trim(),
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        reload()
    }

    fun deleteNickname(twitchUserId: String) {
        database.userNicknameQueries.deleteNickname(twitchUserId)
        reload()
    }
}
