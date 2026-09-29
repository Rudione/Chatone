package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.data.remote.TwitchPubSubClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

class ModerationActionStream(private val pubSubClient: TwitchPubSubClient) {

    fun actions(): Flow<IrcEvent.ModeratorAction> =
        pubSubClient.events.filterIsInstance<IrcEvent.ModeratorAction>()

    fun byAction(action: String): Flow<IrcEvent.ModeratorAction> =
        actions().map { if (it.action == action) it else null }
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { if (it != null) emit(it) }
                }
            }

    fun byModerator(login: String): Flow<IrcEvent.ModeratorAction> =
        actions().map { if (it.moderator.equals(login, ignoreCase = true)) it else null }
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { if (it != null) emit(it) }
                }
            }
}
