package io.rudione.chatone.domain.channel

data class ChannelSnapshot(
    val login: String,
    val channelId: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val title: String = "",
    val category: String = ""
) {
    val complete: Boolean
        get() = channelId.isNotBlank() && avatarUrl.isNotBlank() && title.isNotBlank()

    fun mergedWith(newer: ChannelSnapshot): ChannelSnapshot = copy(
        channelId = newer.channelId.ifBlank { channelId },
        displayName = newer.displayName.ifBlank { displayName },
        avatarUrl = newer.avatarUrl.ifBlank { avatarUrl },
        title = newer.title.ifBlank { title },
        category = newer.category.ifBlank { category }
    )
}

interface ChannelSnapshotSource {
    suspend fun snapshot(login: String): ChannelSnapshot?
}
