package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.emote.BttvApiClient
import io.rudione.chatone.data.remote.emote.FfzApiClient
import io.rudione.chatone.data.remote.emote.SevenTvApiClient
import io.rudione.chatone.domain.emote.SevenTvSetEditor
import io.rudione.chatone.domain.emote.SevenTvSetEdits
import io.rudione.chatone.domain.model.ChannelEmotes
import io.rudione.chatone.domain.model.GenericEmote
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class SevenTvSetSwitch(val emoteSetId: String?, val setName: String)

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

        private val EMPTY_EMOTES = ChannelEmotes()
    }

    private val channelEmotesMap = mutableMapOf<String, MutableStateFlow<ChannelEmotes>>()
    private val sevenTvLinks = SevenTvChannelLinks()

    @OptIn(InternalCoroutinesApi::class)
    private val channelLock = SynchronizedObject()

    @OptIn(InternalCoroutinesApi::class)
    private fun channelFlow(key: String): MutableStateFlow<ChannelEmotes>? =
        synchronized(channelLock) { channelEmotesMap[key] }

    @OptIn(InternalCoroutinesApi::class)
    private fun channelFlowOrCreate(key: String): MutableStateFlow<ChannelEmotes> =
        synchronized(channelLock) {
            channelEmotesMap.getOrPut(key) { MutableStateFlow(ChannelEmotes()) }
        }

    @OptIn(InternalCoroutinesApi::class)
    private val personalLock = SynchronizedObject()
    private val personalSetsByUser = HashMap<String, LinkedHashSet<String>>()
    private val personalUsersBySet = HashMap<String, LinkedHashSet<String>>()
    private val personalSetContents = HashMap<String, List<GenericEmote>>()
    private val personalSetsInflight = HashSet<String>()
    private val personalEmotesFetched = HashSet<String>()
    private val personalEmotesByUser = HashMap<String, List<GenericEmote>>()

    private val _globalEmotes = MutableStateFlow(ChannelEmotes())
    val globalEmotes: StateFlow<ChannelEmotes> = _globalEmotes

    private var globalLoaded = false

    fun getSevenTvEmoteSetId(channelName: String): String? = sevenTvLink(channelName)?.emoteSetId

    fun sevenTvLink(channelName: String): SevenTvChannelLink? = sevenTvLinks.get(channelName.lowercase())

    fun channelsUsingSevenTvSet(emoteSetId: String): List<String> = sevenTvLinks.channelsUsingSet(emoteSetId)

    fun channelsForSevenTvUser(sevenTvUserId: String): List<String> = sevenTvLinks.channelsForUser(sevenTvUserId)

    fun getChannelEmotes(channelName: String): StateFlow<ChannelEmotes> =
        channelFlowOrCreate(channelName.lowercase())

    private class ResolvedEmotes(
        val channelSource: ChannelEmotes,
        val globalSource: ChannelEmotes,
        val merged: ChannelEmotes
    )

    private val resolvedCache = mutableMapOf<String, ResolvedEmotes>()
    private val resolvedLock = SynchronizedObject()

    @OptIn(InternalCoroutinesApi::class)
    fun getResolvedEmotes(channelName: String): ChannelEmotes {
        val key = channelName.lowercase()
        val channel = channelFlow(key)?.value ?: EMPTY_EMOTES
        val global = _globalEmotes.value

        synchronized(resolvedLock) {
            val cached = resolvedCache[key]
            if (cached != null && cached.channelSource === channel && cached.globalSource === global) {
                return cached.merged
            }
        }

        val merged = ChannelEmotes(
            twitchEmotes = channel.twitchEmotes,
            sevenTvChannel = channel.sevenTvChannel,
            sevenTvGlobal = global.sevenTvGlobal,
            bttvChannel = channel.bttvChannel,
            bttvGlobal = global.bttvGlobal,
            ffzChannel = channel.ffzChannel,
            ffzGlobal = global.ffzGlobal
        )
        synchronized(resolvedLock) {
            resolvedCache[key] = ResolvedEmotes(channel, global, merged)
        }
        return merged
    }

    suspend fun loadGlobalEmotes() {
        if (globalLoaded) return
        globalLoaded = true

        withContext(Dispatchers.Default) {
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
        val flow = channelFlowOrCreate(channelName.lowercase())

        coroutineScope {
            val sevenTv = async { sevenTvApi.getChannelEmotesWithSetId(channelId) }
            val bttv = async { bttvApi.getChannelEmotes(channelId) }
            val ffz = async { ffzApi.getChannelEmotes(channelId) }

            val sevenTvResult = sevenTv.await()
            val bttvEmotes = bttv.await()
            val ffzEmotes = ffz.await()

            if (sevenTvResult.emoteSetId != null || sevenTvResult.sevenTvUserId != null) {
                sevenTvLinks.put(
                    channelName.lowercase(),
                    SevenTvChannelLink(channelId, sevenTvResult.emoteSetId, sevenTvResult.sevenTvUserId)
                )
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

    fun applySevenTvChannelEdits(channelName: String, edits: SevenTvSetEdits): Set<String>? {
        val flow = channelFlow(channelName.lowercase()) ?: return null
        var touched = emptySet<String>()
        flow.update { current ->
            val result = SevenTvSetEditor.apply(current.sevenTvChannel, edits)
            touched = result.touchedCodes
            if (result.emotes === current.sevenTvChannel) current else current.copy(sevenTvChannel = result.emotes)
        }
        return touched
    }

    suspend fun reloadSevenTvChannel(channelName: String): SevenTvSetSwitch? {
        val key = channelName.lowercase()
        val link = sevenTvLinks.get(key) ?: return null
        val result = sevenTvApi.getChannelEmotesWithSetId(link.twitchChannelId)
        if (result.sevenTvUserId == null && result.emoteSetId == null) return null
        if (result.emoteSetId == link.emoteSetId) return null
        val updated = link.copy(emoteSetId = result.emoteSetId, sevenTvUserId = result.sevenTvUserId ?: link.sevenTvUserId)
        if (!sevenTvLinks.replace(key, link, updated)) return null
        channelFlowOrCreate(key).update { it.copy(sevenTvChannel = result.emotes) }
        Napier.d("7TV emote set of $channelName switched to ${result.emoteSetId}", tag = TAG)
        return SevenTvSetSwitch(result.emoteSetId, result.emoteSetName.orEmpty())
    }

    fun invalidateChannel(channelName: String) {
        val key = channelName.lowercase()
        channelFlow(key)?.value = ChannelEmotes()
        sevenTvLinks.remove(key)
        Napier.d("Invalidated emote cache for $channelName", tag = TAG)
    }

    @OptIn(InternalCoroutinesApi::class)
    fun invalidatePersonalEmotes() {
        synchronized(personalLock) {
            personalSetsByUser.clear()
            personalUsersBySet.clear()
            personalSetContents.clear()
            personalSetsInflight.clear()
            personalEmotesFetched.clear()
            personalEmotesByUser.clear()
        }
        Napier.d("Cleared personal emotes cache", tag = TAG)
    }

    @OptIn(InternalCoroutinesApi::class)
    fun hasAttemptedPersonalEmotes(twitchUserId: String): Boolean =
        synchronized(personalLock) { twitchUserId in personalEmotesFetched }

    @OptIn(InternalCoroutinesApi::class)
    fun markPersonalEmotesAttempted(twitchUserId: String) {
        if (twitchUserId.isBlank()) return
        synchronized(personalLock) { personalEmotesFetched.add(twitchUserId) }
    }

    @OptIn(InternalCoroutinesApi::class)
    fun getCachedPersonalEmotes(twitchUserId: String): List<GenericEmote> {
        if (twitchUserId.isBlank()) return emptyList()
        return synchronized(personalLock) {
            personalEmotesByUser[twitchUserId] ?: mergePersonalEmotes(twitchUserId)
        }
    }

    @OptIn(InternalCoroutinesApi::class)
    suspend fun grantPersonalEmoteSet(
        twitchUserId: String,
        emoteSetId: String
    ): List<GenericEmote>? {
        if (twitchUserId.isBlank() || emoteSetId.isBlank()) return null

        val needFetch = synchronized(personalLock) {
            personalSetsByUser.getOrPut(twitchUserId) { LinkedHashSet() } += emoteSetId
            personalUsersBySet.getOrPut(emoteSetId) { LinkedHashSet() } += twitchUserId
            personalEmotesFetched += twitchUserId
            personalEmotesByUser.remove(twitchUserId)
            emoteSetId !in personalSetContents && personalSetsInflight.add(emoteSetId)
        }

        if (needFetch) {
            val emotes = try {
                sevenTvApi.getEmoteSet(emoteSetId)
            } catch (e: Exception) {
                Napier.w("Failed to fetch 7TV personal set $emoteSetId: ${e.message}", tag = TAG)
                emptyList()
            }
            synchronized(personalLock) {
                personalSetContents[emoteSetId] = emotes
                personalSetsInflight.remove(emoteSetId)
                forgetMergedFor(emoteSetId)
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

    @OptIn(InternalCoroutinesApi::class)
    fun revokePersonalEmoteSet(twitchUserId: String, emoteSetId: String): List<GenericEmote> {
        if (twitchUserId.isBlank() || emoteSetId.isBlank()) return emptyList()
        synchronized(personalLock) {
            personalSetsByUser.detach(twitchUserId, emoteSetId)
            personalUsersBySet.detach(emoteSetId, twitchUserId)
            personalEmotesByUser.remove(twitchUserId)
        }
        return getCachedPersonalEmotes(twitchUserId)
    }

    @OptIn(InternalCoroutinesApi::class)
    fun isPersonalEmoteSet(emoteSetId: String): Boolean =
        synchronized(personalLock) { emoteSetId in personalSetContents }

    @OptIn(InternalCoroutinesApi::class)
    fun applyPersonalSetEdits(emoteSetId: String, edits: SevenTvSetEdits): Set<String> =
        synchronized(personalLock) {
            val existing = personalSetContents[emoteSetId] ?: return@synchronized emptySet()
            val result = SevenTvSetEditor.apply(existing, edits)
            if (result.emotes === existing) return@synchronized emptySet()
            personalSetContents[emoteSetId] = result.emotes
            forgetMergedFor(emoteSetId)
            personalUsersBySet[emoteSetId]?.toSet().orEmpty()
        }

    private fun mergePersonalEmotes(twitchUserId: String): List<GenericEmote> {
        val setIds = personalSetsByUser[twitchUserId]
        if (setIds.isNullOrEmpty()) return emptyList()
        val merged = LinkedHashMap<String, GenericEmote>()
        for (setId in setIds) {
            val emotes = personalSetContents[setId] ?: continue
            for (emote in emotes) merged[emote.code] = emote
        }
        return merged.values.toList().also { personalEmotesByUser[twitchUserId] = it }
    }

    private fun forgetMergedFor(emoteSetId: String) {
        personalUsersBySet[emoteSetId]?.forEach { personalEmotesByUser.remove(it) }
    }

    private fun HashMap<String, LinkedHashSet<String>>.detach(key: String, value: String) {
        val values = this[key] ?: return
        values -= value
        if (values.isEmpty()) remove(key)
    }
}
