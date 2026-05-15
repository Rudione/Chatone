package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.runtime.Stable


@Stable
data class ChannelDragPayload(
    val channelLogin: String,
    val source: DragSource = DragSource.Unknown
) {
    enum class DragSource { Sidebar, TabBar, MiniRail, Unknown }

    fun encode(): String = "${ChatDragData.PREFIX}${channelLogin.lowercase()}|${source.name}"

    companion object {
        fun parse(raw: String?): ChannelDragPayload? {
            if (raw.isNullOrBlank()) return null
            val trimmed = raw.trim()
            val withoutPrefix = if (trimmed.startsWith(ChatDragData.PREFIX))
                trimmed.removePrefix(ChatDragData.PREFIX) else trimmed
            val parts = withoutPrefix.split("|")
            val channel = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
            val source = parts.getOrNull(1)?.let { name ->
                try { DragSource.valueOf(name) } catch (_: Exception) { DragSource.Unknown }
            } ?: DragSource.Unknown
            return ChannelDragPayload(channel, source)
        }
    }
}
