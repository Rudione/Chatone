package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.emote.SevenTvEventSource
import io.rudione.chatone.data.remote.emote.SevenTvLiveEvent
import io.rudione.chatone.domain.emote.SevenTvRemovedEmote
import io.rudione.chatone.domain.emote.SevenTvRenamedEmote
import io.rudione.chatone.domain.emote.SevenTvSetEdits
import io.rudione.chatone.domain.model.GenericEmote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SevenTvEmoteNotice {
    val actorName: String

    data class Added(override val actorName: String, val emoteName: String) : SevenTvEmoteNotice
    data class Removed(override val actorName: String, val emoteName: String) : SevenTvEmoteNotice
    data class Renamed(override val actorName: String, val oldName: String, val newName: String) : SevenTvEmoteNotice
    data class SetSwitched(override val actorName: String, val setName: String) : SevenTvEmoteNotice
}

sealed interface SevenTvEmoteChange {
    data class Channel(
        val channelLogin: String,
        val notices: List<SevenTvEmoteNotice>,
        val touchedCodes: Set<String>?
    ) : SevenTvEmoteChange

    data class Personal(val twitchUserIds: Set<String>) : SevenTvEmoteChange
}

@OptIn(ExperimentalCoroutinesApi::class)
class SevenTvLiveEmotes(
    private val source: SevenTvEventSource,
    private val emotes: EmoteRepository,
    scope: CoroutineScope
) {
    private val worker = scope + Dispatchers.Default.limitedParallelism(1)
    private val reloads = Mutex()
    private val _changes = MutableSharedFlow<SevenTvEmoteChange>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val changes: SharedFlow<SevenTvEmoteChange> = _changes.asSharedFlow()

    init {
        worker.launch {
            source.dispatches.collect { batch ->
                handle(batch).forEach { _changes.tryEmit(it) }
            }
        }
    }

    fun watchChannel(channelLogin: String, twitchChannelId: String) {
        emotes.sevenTvLink(channelLogin)?.let { link ->
            link.emoteSetId?.let(source::subscribeToEmoteSet)
            link.sevenTvUserId?.let(source::subscribeToUser)
        }
        source.subscribeToTwitchChannel(twitchChannelId)
    }

    fun watchPersonalSet(emoteSetId: String) = source.subscribeToPersonalSet(emoteSetId)

    internal fun handle(batch: List<SevenTvLiveEvent>): List<SevenTvEmoteChange> {
        val changes = mutableListOf<SevenTvEmoteChange>()
        val setEdits = LinkedHashMap<String, SetEditBuilder>()
        fun edits(setId: String) = setEdits.getOrPut(setId) { SetEditBuilder() }

        for (event in batch) {
            when (event) {
                is SevenTvLiveEvent.EmoteRemoved -> edits(event.emoteSetId).remove(event)
                is SevenTvLiveEvent.EmoteRenamed -> edits(event.emoteSetId).rename(event)
                is SevenTvLiveEvent.EmoteAdded -> edits(event.emoteSetId).add(event)
                is SevenTvLiveEvent.PersonalEmoteSetRevoked -> {
                    emotes.revokePersonalEmoteSet(event.twitchUserId, event.emoteSetId)
                    changes += SevenTvEmoteChange.Personal(setOf(event.twitchUserId))
                }
                is SevenTvLiveEvent.PersonalEmoteSetGranted -> worker.launch { grant(event) }
                is SevenTvLiveEvent.UserEmoteSetChanged ->
                    emotes.channelsForSevenTvUser(event.sevenTvUserId).forEach { login ->
                        worker.launch { switchSet(login, event.actorName) }
                    }
            }
        }

        for ((setId, builder) in setEdits) {
            val edits = builder.build()
            if (emotes.isPersonalEmoteSet(setId)) {
                val users = emotes.applyPersonalSetEdits(setId, edits)
                if (users.isNotEmpty()) changes += SevenTvEmoteChange.Personal(users)
                continue
            }
            val notices = builder.notices.toList()
            for (login in emotes.channelsUsingSevenTvSet(setId)) {
                val touched = emotes.applySevenTvChannelEdits(login, edits) ?: continue
                if (touched.isEmpty() && notices.isEmpty()) continue
                changes += SevenTvEmoteChange.Channel(login, notices, touched)
            }
        }
        return changes
    }

    internal suspend fun grant(event: SevenTvLiveEvent.PersonalEmoteSetGranted): SevenTvEmoteChange? {
        val granted = try {
            emotes.grantPersonalEmoteSet(event.twitchUserId, event.emoteSetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Failed to grant 7TV personal set ${event.emoteSetId}: ${e.message}", tag = TAG)
            null
        }
        source.subscribeToPersonalSet(event.emoteSetId)
        if (granted.isNullOrEmpty()) return null
        return SevenTvEmoteChange.Personal(setOf(event.twitchUserId)).also { _changes.tryEmit(it) }
    }

    internal suspend fun switchSet(channelLogin: String, actorName: String): SevenTvEmoteChange? {
        val switched = reloads.withLock { emotes.reloadSevenTvChannel(channelLogin) } ?: return null
        switched.emoteSetId?.let(source::subscribeToEmoteSet)
        val notices = listOfNotNull(
            SevenTvEmoteNotice.SetSwitched(actorName, switched.setName)
                .takeIf { actorName.isNotBlank() && switched.setName.isNotBlank() }
        )
        return SevenTvEmoteChange.Channel(channelLogin, notices, touchedCodes = null).also { _changes.tryEmit(it) }
    }

    private class SetEditBuilder {
        private val removed = mutableListOf<SevenTvRemovedEmote>()
        private val renamed = mutableListOf<SevenTvRenamedEmote>()
        private val added = mutableListOf<GenericEmote>()
        val notices = mutableListOf<SevenTvEmoteNotice>()

        fun remove(event: SevenTvLiveEvent.EmoteRemoved) {
            removed += SevenTvRemovedEmote(event.emoteId, event.emoteName)
            notice(SevenTvEmoteNotice.Removed(event.actorName, event.emoteName))
        }

        fun rename(event: SevenTvLiveEvent.EmoteRenamed) {
            renamed += SevenTvRenamedEmote(event.emoteId, event.newName)
            notice(SevenTvEmoteNotice.Renamed(event.actorName, event.oldName, event.newName))
        }

        fun add(event: SevenTvLiveEvent.EmoteAdded) {
            added += event.emote
            notice(SevenTvEmoteNotice.Added(event.actorName, event.emote.code))
        }

        fun build() = SevenTvSetEdits(removed.toList(), renamed.toList(), added.toList())

        private fun notice(notice: SevenTvEmoteNotice) {
            if (notice.actorName.isNotBlank()) notices += notice
        }
    }

    private companion object {
        const val TAG = "7TV-Live"
    }
}
