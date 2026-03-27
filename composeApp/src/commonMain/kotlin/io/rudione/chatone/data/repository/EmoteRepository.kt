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
    private val channelEmoteSetIds = mutableMapOf<String, String>() // channelName -> 7TV emote set ID

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

            // Store the 7TV emote set ID for EventAPI subscription
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
}
