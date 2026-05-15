package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.emote.BttvApiClient
import io.rudione.chatone.data.remote.emote.FfzApiClient
import io.rudione.chatone.data.remote.emote.SevenTvApiClient
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.GenericEmote
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EmoteRepository(
    private val sevenTvApi: SevenTvApiClient,
    private val bttvApi: BttvApiClient,
    private val ffzApi: FfzApiClient
) {
    companion object {
        private const val TAG = "EmoteRepository"

        val OVERLAY_EMOTES = setOf(
            "SoSnowy", "IceCold", "SantaHat", "TopHat",
            "ReinDeer", "CandyCane", "cvMask", "cvHazmat"
        )
    }

    private val channelEmotesMap = mutableMapOf<String, MutableStateFlow<ChannelEmotes>>()
    private val channelEmoteSetIds = mutableMapOf<String, String>()

    private val personalEmoteSetsByUser = mutableMapOf<String, MutableSet<String>>()
    private val personalEmoteSetContents = mutableMapOf<String, List<GenericEmote>>()
    private val personalEmotesMutex = Mutex()
    private val personalEmoteSetsInflight = mutableSetOf<String>()
    private val personalEmotesFetched = mutableSetOf<String>()

    private val _globalEmotes = MutableStateFlow(ChannelEmotes())
    val globalEmotes: StateFlow<ChannelEmotes> = _globalEmotes

    private var globalLoaded = false

    fun getSevenTvEmoteSetId(channelName: String): String? {
        return channelEmoteSetIds[channelName.lowercase()]
    }

    fun getChannelEmotes(channelName: String): StateFlow<ChannelEmotes> {
        return channelEmotesMap.getOrPut(channelName.lowercase()) {
            MutableStateFlow(ChannelEmotes())
        }
    }

    fun getResolvedEmotes(channelName: String): ChannelEmotes {
        val channel = channelEmotesMap[channelName.lowercase()]?.value ?: ChannelEmotes()
        val global = _globalEmotes.value
        return ChannelEmotes(
            twitchEmotes = channel.twitchEmotes,
            sevenTvChannel = channel.sevenTvChannel,
            sevenTvGlobal = global.sevenTvGlobal,
            bttvChannel = channel.bttvChannel,
            bttvGlobal = global.bttvGlobal,
            ffzChannel = channel.ffzChannel,
            ffzGlobal = global.ffzGlobal
        )
    }

    suspend fun loadGlobalEmotes() {
        if (globalLoaded) return
        globalLoaded = true

        coroutineScope {
            val sevenTv = async { sevenTvApi.getGlobalEmotes() }
            val bttv = async { bttvApi.getGlobalEmotes() }
            val ffz = async { ffzApi.getGlobalEmotes() }

            val sevenTvEmotes = sevenTv.await()
            val bttvEmotes = bttv.await()
            val ffzEmotes = ffz.await()

            _globalEmotes.value = ChannelEmotes(
                sevenTvGlobal = sevenTvEmotes,
                bttvGlobal = bttvEmotes,
                ffzGlobal = ffzEmotes
            )

            Napier.d("Global emotes loaded: 7TV=${sevenTvEmotes.size}, BTTV=${bttvEmotes.size}, FFZ=${ffzEmotes.size}", tag = TAG)
        }
    }

    suspend fun loadChannelEmotes(channelName: String, channelId: String) {
        val flow = channelEmotesMap.getOrPut(channelName.lowercase()) {
            MutableStateFlow(ChannelEmotes())
        }

        coroutineScope {
            val sevenTv = async { sevenTvApi.getChannelEmotesWithSetId(channelId) }
            val bttv = async { bttvApi.getChannelEmotes(channelId) }
            val ffz = async { ffzApi.getChannelEmotes(channelId) }

            val sevenTvResult = sevenTv.await()
            val bttvEmotes = bttv.await()
            val ffzEmotes = ffz.await()


            sevenTvResult.emoteSetId?.let { setId ->
                channelEmoteSetIds[channelName.lowercase()] = setId
            }

            flow.value = flow.value.copy(
                sevenTvChannel = sevenTvResult.emotes,
                bttvChannel = bttvEmotes,
                ffzChannel = ffzEmotes
            )

            Napier.d("Channel $channelName emotes loaded: 7TV=${sevenTvResult.emotes.size}, BTTV=${bttvEmotes.size}, FFZ=${ffzEmotes.size}", tag = TAG)
        }
    }

    fun isOverlayEmote(emote: GenericEmote): Boolean {
        return emote.isZeroWidth || emote.code in OVERLAY_EMOTES
    }


    fun patchChannelEmote(channelName: String, emote: GenericEmote) {
        val key = channelName.lowercase()
        val flow = channelEmotesMap.getOrPut(key) { MutableStateFlow(ChannelEmotes()) }
        val current = flow.value
        val updated = current.sevenTvChannel
            .filterNot { it.id == emote.id } + emote
        flow.value = current.copy(sevenTvChannel = updated)
        Napier.d("Patched 7TV emote ${emote.code} in $channelName", tag = TAG)
    }


    fun removeChannelEmote(channelName: String, emoteId: String, emoteName: String) {
        val key = channelName.lowercase()
        val flow = channelEmotesMap[key] ?: return
        val current = flow.value
        flow.value = current.copy(
            sevenTvChannel = current.sevenTvChannel.filterNot { it.id == emoteId || it.code == emoteName }
        )
        Napier.d("Removed 7TV emote $emoteName from $channelName", tag = TAG)
    }


    fun renameChannelEmote(channelName: String, emoteId: String, newName: String) {
        val key = channelName.lowercase()
        val flow = channelEmotesMap[key] ?: return
        val current = flow.value
        flow.value = current.copy(
            sevenTvChannel = current.sevenTvChannel.map {
                if (it.id == emoteId) it.copy(code = newName) else it
            }
        )
        Napier.d("Renamed 7TV emote $emoteId → $newName in $channelName", tag = TAG)
    }

    fun invalidateChannel(channelName: String) {
        val key = channelName.lowercase()
        channelEmotesMap[key]?.value = ChannelEmotes()
        channelEmoteSetIds.remove(key)
        Napier.d("Invalidated emote cache for $channelName", tag = TAG)
    }

    fun invalidatePersonalEmotes() {
        personalEmoteSetsByUser.clear()
        personalEmoteSetContents.clear()
        personalEmoteSetsInflight.clear()
        personalEmotesFetched.clear()
        Napier.d("Cleared personal emotes cache", tag = TAG)
    }

    fun hasAttemptedPersonalEmotes(twitchUserId: String): Boolean =
        twitchUserId in personalEmotesFetched

    fun markPersonalEmotesAttempted(twitchUserId: String) {
        if (twitchUserId.isBlank()) return
        personalEmotesFetched.add(twitchUserId)
    }

    fun getCachedPersonalEmotes(twitchUserId: String): List<GenericEmote> {
        if (twitchUserId.isBlank()) return emptyList()
        val setIds = personalEmoteSetsByUser[twitchUserId] ?: return emptyList()
        if (setIds.isEmpty()) return emptyList()
        val merged = mutableMapOf<String, GenericEmote>()
        for (setId in setIds) {
            val emotes = personalEmoteSetContents[setId] ?: continue
            for (e in emotes) merged[e.code] = e
        }
        return merged.values.toList()
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun loadPersonalEmotes(twitchUserId: String): List<GenericEmote> {
        if (twitchUserId.isBlank()) return emptyList()
        markPersonalEmotesAttempted(twitchUserId)
        return getCachedPersonalEmotes(twitchUserId)
    }

    suspend fun grantPersonalEmoteSet(
        twitchUserId: String,
        emoteSetId: String
    ): List<GenericEmote>? {
        if (twitchUserId.isBlank() || emoteSetId.isBlank()) return null

        var needFetch = false
        personalEmotesMutex.withLock {
            val sets = personalEmoteSetsByUser.getOrPut(twitchUserId) { mutableSetOf() }
            sets.add(emoteSetId)
            personalEmotesFetched.add(twitchUserId)
            if (emoteSetId !in personalEmoteSetContents &&
                emoteSetId !in personalEmoteSetsInflight
            ) {
                personalEmoteSetsInflight.add(emoteSetId)
                needFetch = true
            }
        }

        if (needFetch) {
            val emotes = try {
                sevenTvApi.getEmoteSet(emoteSetId)
            } catch (e: Exception) {
                Napier.w("Failed to fetch 7TV personal set $emoteSetId: ${e.message}", tag = TAG)
                emptyList()
            }
            personalEmotesMutex.withLock {
                personalEmoteSetContents[emoteSetId] = emotes
                personalEmoteSetsInflight.remove(emoteSetId)
            }
            if (emotes.isNotEmpty()) {
                Napier.d(
                    "Loaded ${emotes.size} 7TV personal emotes (set $emoteSetId) for user $twitchUserId",
                    tag = TAG
                )
            }
        }

        return getCachedPersonalEmotes(twitchUserId)
    }

    fun revokePersonalEmoteSet(twitchUserId: String, emoteSetId: String): List<GenericEmote> {
        if (twitchUserId.isBlank() || emoteSetId.isBlank()) return emptyList()
        val sets = personalEmoteSetsByUser[twitchUserId]
        if (sets != null) {
            sets.remove(emoteSetId)
            if (sets.isEmpty()) personalEmoteSetsByUser.remove(twitchUserId)
        }
        return getCachedPersonalEmotes(twitchUserId)
    }

    fun isPersonalEmoteSet(emoteSetId: String): Boolean =
        emoteSetId in personalEmoteSetContents

    fun usersForPersonalSet(emoteSetId: String): List<String> {
        return personalEmoteSetsByUser.entries
            .filter { emoteSetId in it.value }
            .map { it.key }
    }

    fun patchPersonalSet(emoteSetId: String, emote: GenericEmote) {
        val existing = personalEmoteSetContents[emoteSetId] ?: return
        personalEmoteSetContents[emoteSetId] = existing.filterNot { it.id == emote.id } + emote
    }

    fun removeFromPersonalSet(emoteSetId: String, emoteId: String, emoteName: String) {
        val existing = personalEmoteSetContents[emoteSetId] ?: return
        personalEmoteSetContents[emoteSetId] =
            existing.filterNot { it.id == emoteId || it.code == emoteName }
    }

    fun renameInPersonalSet(emoteSetId: String, emoteId: String, newName: String) {
        val existing = personalEmoteSetContents[emoteSetId] ?: return
        personalEmoteSetContents[emoteSetId] = existing.map {
            if (it.id == emoteId) it.copy(code = newName) else it
        }
    }
}
