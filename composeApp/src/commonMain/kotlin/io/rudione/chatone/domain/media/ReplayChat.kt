package io.rudione.chatone.domain.media

import io.rudione.chatone.domain.model.DisplayMessage

data class ReplayChannel(val id: String, val login: String, val startedAtMs: Long)

interface ReplayChatRenderer {
    suspend fun prepare(channel: ReplayChannel)
    suspend fun render(channel: ReplayChannel, lines: List<ReplayLine>): List<DisplayMessage.PrivMsg>
    suspend fun loadCosmetics(userIds: Collection<String>): Boolean
}
