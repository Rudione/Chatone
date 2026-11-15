package io.rudione.chatone.domain.model

data class ChatNoticeUser(
    val userId: String,
    val login: String,
    val displayName: String,
    val color: String? = null
)

data class ChatGiftBundle(
    val total: Int,
    val recipients: List<ChatNoticeUser>,
    val plan: String,
    val communityId: String?,
    val anonymous: Boolean,
    val hasSummary: Boolean,
    val eventIds: Set<String>,
    val lastTimestamp: Long
)

data class ChatNoticeDetails(
    val author: ChatNoticeUser?,
    val raidViewers: Int? = null,
    val watchStreak: Int? = null,
    val gifts: ChatGiftBundle? = null
)
