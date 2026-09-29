package io.rudione.chatone.domain.model

const val DEFAULT_FOLDER_COLOR_HEX = "#9B6DFF"

data class ChannelFolder(
    val id: String,
    val name: String,
    val color: String = DEFAULT_FOLDER_COLOR_HEX,
    val isExpanded: Boolean = true,
    val channels: List<ChannelTab> = emptyList()
)

data class ChannelTab(
    val login: String,
    val displayName: String,
    val profileImageUrl: String = "",
    val isLive: Boolean = false,
    val unreadCount: Int = 0,
    val notificationsMuted: Boolean = false
)
