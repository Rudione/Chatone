package io.rudione.chatone.domain.mention

data class MentionRef(
    val messageId: String,
    val timestampMs: Long,
    val authorLogin: String,
    val authorDisplayName: String
) {
    val insertionName: String get() = MentionInsertion.preferredName(authorDisplayName, authorLogin)
}

data class MentionReadMark(
    val watermarkMs: Long,
    val readIds: Set<String> = emptySet()
) {

    fun isRead(mention: MentionRef): Boolean =
        mention.timestampMs <= watermarkMs || mention.messageId in readIds

    fun unread(known: List<MentionRef>): List<MentionRef> = known.filterNot(::isRead)

    fun withRead(ids: Collection<String>, known: List<MentionRef>): MentionReadMark {
        if (ids.isEmpty()) return this
        val read = readIds + ids
        val oldestUnread = known
            .filter { it.timestampMs > watermarkMs && it.messageId !in read }
            .minOfOrNull { it.timestampMs }
        val newestRead = known.filter { it.messageId in read }.maxOfOrNull { it.timestampMs }
        val watermark = maxOf(watermarkMs, oldestUnread?.minus(1) ?: newestRead ?: watermarkMs)
        val kept = known
            .filter { it.messageId in read && it.timestampMs > watermark }
            .sortedByDescending { it.timestampMs }
            .take(MAX_READ_IDS)
            .mapTo(LinkedHashSet()) { it.messageId }
        return MentionReadMark(watermark, kept)
    }

    companion object {
        const val MAX_READ_IDS = 200

        fun startingAt(timestampMs: Long): MentionReadMark = MentionReadMark(timestampMs)
    }
}

interface MentionReadStore {
    fun mark(channelLogin: String): MentionReadMark?
    fun save(channelLogin: String, mark: MentionReadMark)
}
