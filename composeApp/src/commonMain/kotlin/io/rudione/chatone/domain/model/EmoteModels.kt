package io.rudione.chatone.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
enum class EmoteProvider {
    TWITCH, SEVEN_TV, BTTV, FFZ
}

@Immutable
data class GenericEmote(
    val id: String,
    val code: String,
    val url1x: String,
    val url2x: String,
    val url3x: String,
    val provider: EmoteProvider,
    val isZeroWidth: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val originalName: String = "",
    val authorName: String = ""
) {
    val imageCacheKey: String by lazy { "${provider.name}_$id" }
    val listKey: String by lazy { "${provider.name}_$id" }
}

@Stable
data class ChannelEmotes(
    val twitchEmotes: List<GenericEmote> = emptyList(),
    val twitchGlobal: List<GenericEmote> = emptyList(),
    val sevenTvChannel: List<GenericEmote> = emptyList(),
    val sevenTvGlobal: List<GenericEmote> = emptyList(),
    val bttvChannel: List<GenericEmote> = emptyList(),
    val bttvGlobal: List<GenericEmote> = emptyList(),
    val ffzChannel: List<GenericEmote> = emptyList(),
    val ffzGlobal: List<GenericEmote> = emptyList()
) {
    val allByCode: Map<String, GenericEmote> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            twitchGlobal.forEach { put(it.code, it) }
            ffzGlobal.forEach { put(it.code, it) }
            bttvGlobal.forEach { put(it.code, it) }
            sevenTvGlobal.forEach { put(it.code, it) }
            ffzChannel.forEach { put(it.code, it) }
            bttvChannel.forEach { put(it.code, it) }
            sevenTvChannel.forEach { put(it.code, it) }
            twitchEmotes.forEach { put(it.code, it) }
        }
    }

    val all: List<GenericEmote> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<GenericEmote>()
        val sources = listOf(
            twitchEmotes, sevenTvChannel, bttvChannel, ffzChannel,
            sevenTvGlobal, bttvGlobal, ffzGlobal, twitchGlobal
        )
        for (source in sources) {
            for (emote in source) {
                if (seen.add(emote.listKey)) {
                    result.add(emote)
                }
            }
        }
        result
    }

    val byProvider: Map<EmoteProvider, List<GenericEmote>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val result = mutableMapOf<EmoteProvider, MutableList<GenericEmote>>()
        for (emote in all) {
            result.getOrPut(emote.provider) { mutableListOf() }.add(emote)
        }
        result
    }
}
