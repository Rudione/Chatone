package io.rudione.chatone.domain.model

enum class EmoteProvider {
    TWITCH, SEVEN_TV, BTTV, FFZ
}

data class GenericEmote(
    val id: String,
    val code: String,
    val url1x: String,
    val url2x: String,
    val url3x: String,
    val provider: EmoteProvider,
    val isZeroWidth: Boolean = false,
    // Original dimensions from API — used to compute aspect-aware display size
    val width: Int = 0,
    val height: Int = 0,
    // Extra info for tooltip (7TV specific)
    val originalName: String = "",  // original emote name (may differ from alias)
    val authorName: String = ""     // uploader/author name
)

data class ChannelEmotes(
    val twitchEmotes: List<GenericEmote> = emptyList(),
    val sevenTvChannel: List<GenericEmote> = emptyList(),
    val sevenTvGlobal: List<GenericEmote> = emptyList(),
    val bttvChannel: List<GenericEmote> = emptyList(),
    val bttvGlobal: List<GenericEmote> = emptyList(),
    val ffzChannel: List<GenericEmote> = emptyList(),
    val ffzGlobal: List<GenericEmote> = emptyList()
) {
    val allByCode: Map<String, GenericEmote> by lazy {
        buildMap {
            ffzGlobal.forEach { put(it.code, it) }
            bttvGlobal.forEach { put(it.code, it) }
            sevenTvGlobal.forEach { put(it.code, it) }
            ffzChannel.forEach { put(it.code, it) }
            bttvChannel.forEach { put(it.code, it) }
            sevenTvChannel.forEach { put(it.code, it) }
            twitchEmotes.forEach { put(it.code, it) }
        }
    }

    val all: List<GenericEmote> by lazy {
        val seen = mutableSetOf<String>()
        (twitchEmotes + sevenTvChannel + bttvChannel + ffzChannel +
                sevenTvGlobal + bttvGlobal + ffzGlobal)
            .filter { emote ->
                seen.add("${emote.provider}_${emote.id}")
            }
    }
}