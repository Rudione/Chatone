package io.rudione.chatone.data.remote.emote

import io.rudione.chatone.data.remote.dto.SevenTvEmote
import io.rudione.chatone.domain.model.EmoteProvider
import io.rudione.chatone.domain.model.GenericEmote

internal object SevenTvEmoteMapper {
    private const val ZERO_WIDTH_FLAG = 1 shl 8
    private const val CDN = "https://cdn.7tv.app/emote"
    private val FORMAT_PREFERENCE = listOf("WEBP", "AVIF", "GIF", "PNG")

    fun fromSetEntry(entry: SevenTvEmote): GenericEmote? {
        val emoteData = entry.data ?: return null
        val host = emoteData.host
        val baseUrl = "https:${host.url}"

        val files = FORMAT_PREFERENCE
            .firstNotNullOfOrNull { fmt ->
                host.files.filter { it.format == fmt }.takeIf { it.isNotEmpty() }
            }
            ?: return null

        val file1x = files.find { it.name.contains("1x") }
        val file2x = files.find { it.name.contains("2x") }
        val file3x = files.find { it.name.contains("4x") }
            ?: files.find { it.name.contains("3x") }

        val url1x = file1x?.let { "$baseUrl/${it.name}" }
        val url2x = file2x?.let { "$baseUrl/${it.name}" }
        val url3x = file3x?.let { "$baseUrl/${it.name}" }

        val sizeFile = file2x ?: file1x ?: files.first()

        return GenericEmote(
            id = entry.id,
            code = entry.name,
            url1x = url1x ?: "$baseUrl/${files.first().name}",
            url2x = url2x ?: url1x ?: "$baseUrl/${files.first().name}",
            url3x = url3x ?: url2x ?: url1x ?: "$baseUrl/${files.first().name}",
            provider = EmoteProvider.SEVEN_TV,
            isZeroWidth = (entry.flags and ZERO_WIDTH_FLAG) != 0 || (emoteData.flags and ZERO_WIDTH_FLAG) != 0,
            width = sizeFile.width,
            height = sizeFile.height,
            originalName = emoteData.name,
            authorName = emoteData.owner?.displayName?.ifEmpty { emoteData.owner.username } ?: ""
        )
    }

    fun fromCdn(id: String, name: String): GenericEmote = GenericEmote(
        id = id,
        code = name,
        url1x = "$CDN/$id/1x.webp",
        url2x = "$CDN/$id/2x.webp",
        url3x = "$CDN/$id/4x.webp",
        provider = EmoteProvider.SEVEN_TV
    )
}
