package io.rudione.chatone.data.remote.emote

import io.rudione.chatone.domain.model.GenericEmote
import kotlinx.coroutines.flow.SharedFlow

sealed interface SevenTvLiveEvent {
    data class EmoteAdded(
        val emoteSetId: String,
        val emote: GenericEmote,
        val actorName: String
    ) : SevenTvLiveEvent

    data class EmoteRemoved(
        val emoteSetId: String,
        val emoteId: String,
        val emoteName: String,
        val actorName: String
    ) : SevenTvLiveEvent

    data class EmoteRenamed(
        val emoteSetId: String,
        val emoteId: String,
        val oldName: String,
        val newName: String,
        val actorName: String
    ) : SevenTvLiveEvent

    data class UserEmoteSetChanged(
        val sevenTvUserId: String,
        val actorName: String
    ) : SevenTvLiveEvent

    data class PersonalEmoteSetGranted(
        val twitchUserId: String,
        val emoteSetId: String
    ) : SevenTvLiveEvent

    data class PersonalEmoteSetRevoked(
        val twitchUserId: String,
        val emoteSetId: String
    ) : SevenTvLiveEvent
}

interface SevenTvEventSource {
    val dispatches: SharedFlow<List<SevenTvLiveEvent>>
    fun subscribeToEmoteSet(emoteSetId: String)
    fun subscribeToPersonalSet(emoteSetId: String)
    fun subscribeToUser(sevenTvUserId: String)
    fun subscribeToTwitchChannel(twitchChannelId: String)
}
