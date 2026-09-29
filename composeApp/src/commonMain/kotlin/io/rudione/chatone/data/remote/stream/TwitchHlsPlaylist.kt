package io.rudione.chatone.data.remote.stream

import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.domain.stream.TwitchServingInfo

internal class ParsedMasterPlaylist(
    val variants: List<StreamVariant>,
    val serving: TwitchServingInfo,
    val sanitizedPlaylist: String
)

internal object TwitchHlsPlaylist {
    const val MAX_PLAYLIST_CHARS = 512_000

    private const val HEADER = "#EXTM3U"
    private const val TAG_TWITCH_INFO = "#EXT-X-TWITCH-INFO:"
    private const val TAG_MEDIA = "#EXT-X-MEDIA:"
    private const val TAG_STREAM_INF = "#EXT-X-STREAM-INF:"
    private const val TAG_PREFETCH = "#EXT-X-TWITCH-PREFETCH:"
    private const val TAG_EXTINF = "#EXTINF:"
    private const val TAG_TARGET_DURATION = "#EXT-X-TARGETDURATION:"
    private const val DEFAULT_PREFETCH_DURATION = "2.000"
    private const val MIN_PREFETCH_DURATION_SECONDS = 1.0
    private const val MAX_ATTRIBUTES = 64
    private const val MAX_VARIANTS = 32
    private const val MAX_INFO_VALUE_LENGTH = 96

    fun parseMaster(text: String): ParsedMasterPlaylist? {
        if (text.length > MAX_PLAYLIST_CHARS) return null
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.firstOrNull() != HEADER) return null

        val groupNames = HashMap<String, String>()
        val variants = ArrayList<StreamVariant>()
        val sanitized = StringBuilder(text.length)
        var serving = TwitchServingInfo()
        var pendingStreamInf: Map<String, String>? = null

        for (line in lines) {
            when {
                line.startsWith(TAG_TWITCH_INFO) -> {
                    serving = servingInfoFrom(attributes(line.substring(TAG_TWITCH_INFO.length)))
                    continue
                }
                line.startsWith(TAG_MEDIA) -> {
                    val attrs = attributes(line.substring(TAG_MEDIA.length))
                    val group = attrs["GROUP-ID"]
                    val name = attrs["NAME"]
                    if (attrs["TYPE"] == "VIDEO" && !group.isNullOrEmpty() && !name.isNullOrEmpty()) {
                        groupNames[group] = name
                    }
                }
                line.startsWith(TAG_STREAM_INF) -> pendingStreamInf = attributes(line.substring(TAG_STREAM_INF.length))
                !line.startsWith("#") -> {
                    val attrs = pendingStreamInf
                    pendingStreamInf = null
                    if (attrs != null && variants.size < MAX_VARIANTS) {
                        variants += variantOf(attrs, line, groupNames)
                    }
                }
            }
            sanitized.append(line).append('\n')
        }

        if (variants.isEmpty()) return null
        return ParsedMasterPlaylist(variants, serving, sanitized.toString())
    }

    fun rewriteMediaPlaylist(mediaPlaylist: String, expandPrefetch: Boolean): String? {
        val lines = mediaPlaylist.lineSequence().map { it.trimEnd('\r') }.toList()
        val hasPrefetch = expandPrefetch && lines.any { it.startsWith(TAG_PREFETCH) }
        val body = ArrayList<String>(lines.size + 8)
        var lastDuration = DEFAULT_PREFETCH_DURATION
        var longestRounded = 0L
        for (line in lines) {
            when {
                line.startsWith(TAG_EXTINF) -> {
                    val duration = line.substring(TAG_EXTINF.length).substringBefore(',').trim()
                    val seconds = duration.toDoubleOrNull()
                    if (seconds != null && seconds.isFinite() && seconds >= 0.0) {
                        longestRounded = maxOf(longestRounded, kotlin.math.round(seconds).toLong())
                        if (seconds >= MIN_PREFETCH_DURATION_SECONDS) lastDuration = duration
                    }
                    body += line
                }
                line.startsWith(TAG_PREFETCH) && hasPrefetch -> {
                    val uri = line.substring(TAG_PREFETCH.length).trim()
                    if (uri.isNotEmpty() && !uri.startsWith("#")) {
                        longestRounded = maxOf(longestRounded, lastDuration.toDoubleOrNull()?.let { kotlin.math.round(it).toLong() } ?: 0L)
                        body += "$TAG_EXTINF$lastDuration,live"
                        body += uri
                    }
                }
                else -> body += line
            }
        }
        val tightTarget = longestRounded.coerceAtLeast(1L)
        var targetChanged = false
        val out = StringBuilder(mediaPlaylist.length + 256)
        for (line in body) {
            if (line.startsWith(TAG_TARGET_DURATION)) {
                val declared = line.substring(TAG_TARGET_DURATION.length).trim().toLongOrNull()
                if (declared != null && longestRounded > 0L && declared > tightTarget) {
                    out.append(TAG_TARGET_DURATION).append(tightTarget).append('\n')
                    targetChanged = true
                    continue
                }
            }
            out.append(line).append('\n')
        }
        return if (hasPrefetch || targetChanged) out.toString() else null
    }

    fun attributes(list: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val n = list.length
        var i = 0
        while (i < n && result.size < MAX_ATTRIBUTES) {
            while (i < n && (list[i] == ',' || list[i] == ' ')) i++
            val keyStart = i
            while (i < n && list[i] != '=' && list[i] != ',') i++
            if (i >= n || list[i] != '=') continue
            val key = list.substring(keyStart, i).trim()
            i++
            val value = if (i < n && list[i] == '"') {
                val close = list.indexOf('"', i + 1)
                val end = if (close < 0) n else close
                val quoted = list.substring(i + 1, end)
                i = if (close < 0) n else close + 1
                quoted
            } else {
                val comma = list.indexOf(',', i)
                val end = if (comma < 0) n else comma
                val plain = list.substring(i, end).trim()
                i = end
                plain
            }
            if (key.isNotEmpty()) result[key] = value
        }
        return result
    }

    private fun variantOf(attrs: Map<String, String>, uri: String, groupNames: Map<String, String>): StreamVariant {
        val group = attrs["VIDEO"].orEmpty()
        val resolution = attrs["RESOLUTION"].orEmpty()
        val width = resolution.substringBefore('x', "").toIntOrNull() ?: 0
        val height = resolution.substringAfter('x', "").toIntOrNull() ?: 0
        val frameRate = attrs["FRAME-RATE"]?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: 0f
        val name = groupNames[group] ?: group.ifEmpty { if (height > 0) "${height}p" else StreamVariant.AUDIO_ONLY_GROUP }
        return StreamVariant(
            groupId = group.ifEmpty { name },
            name = name,
            url = uri,
            bandwidth = attrs["BANDWIDTH"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
            width = width.coerceAtLeast(0),
            height = height.coerceAtLeast(0),
            frameRate = frameRate,
            codecs = attrs["CODECS"].orEmpty()
        )
    }

    private fun servingInfoFrom(attrs: Map<String, String>): TwitchServingInfo {
        val serverTimeMs = attrs["SERVER-TIME"].positiveSecondsToMs()
        val streamTimeMs = attrs["STREAM-TIME"].positiveSecondsToMs()
        return TwitchServingInfo(
            servingId = attrs["SERVING-ID"].displaySafe(),
            broadcastId = attrs["BROADCAST-ID"].displaySafe(),
            transcodeStack = attrs["TRANSCODESTACK"].displaySafe(),
            serverTimeMs = serverTimeMs,
            streamStartedAtMs = if (serverTimeMs != null && streamTimeMs != null && streamTimeMs <= serverTimeMs) {
                serverTimeMs - streamTimeMs
            } else {
                null
            }
        )
    }

    private fun String?.positiveSecondsToMs(): Long? =
        this?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?.let { (it * 1000.0).toLong() }

    private fun String?.displaySafe(): String? {
        if (this.isNullOrEmpty() || length > MAX_INFO_VALUE_LENGTH) return null
        return takeIf { value -> value.all { it.isAsciiIdentifierChar() } }
    }

    private fun Char.isAsciiIdentifierChar(): Boolean =
        this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9' || this == '.' || this == '-' || this == '_'
}
