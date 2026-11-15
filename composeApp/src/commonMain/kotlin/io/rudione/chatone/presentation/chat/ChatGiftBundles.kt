package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.ChatGiftBundle
import io.rudione.chatone.domain.model.ChatNoticeUser
import io.rudione.chatone.domain.model.DisplayMessage
import kotlin.math.abs
import kotlin.math.max

internal object ChatGiftBundles {
    private const val MATCH_WINDOW_MS = 20_000L

    fun append(messages: List<DisplayMessage>, incoming: DisplayMessage): List<DisplayMessage> {
        val notice = incoming as? DisplayMessage.UserNoticeMsg ?: return messages + incoming
        val gifts = notice.details?.gifts ?: return messages + incoming
        val index = messages.indexOfLast { existing ->
            existing is DisplayMessage.UserNoticeMsg && matches(existing, notice)
        }
        if (index < 0) return messages + incoming
        val existing = messages[index] as DisplayMessage.UserNoticeMsg
        val previous = existing.details!!.gifts!!
        if (gifts.eventIds.isNotEmpty() && previous.eventIds.containsAll(gifts.eventIds)) return messages
        val recipients = (previous.recipients + gifts.recipients).distinctBy(::recipientKey)
        if (!gifts.hasSummary && gifts.recipients.isNotEmpty() && recipients.size == previous.recipients.size) return messages
        val summary = previous.hasSummary || gifts.hasSummary
        val total = when {
            previous.hasSummary && gifts.hasSummary ->
                if (previous.communityId != null && previous.communityId == gifts.communityId) max(previous.total, gifts.total)
                else previous.total + gifts.total
            summary -> max(max(previous.total, gifts.total), recipients.size)
            else -> max(recipients.size, previous.total + gifts.total)
        }
        val merged = existing.copy(
            details = existing.details.copy(
                author = existing.details.author ?: notice.details.author,
                gifts = previous.copy(
                    total = total,
                    recipients = recipients,
                    communityId = previous.communityId ?: gifts.communityId,
                    hasSummary = summary,
                    eventIds = previous.eventIds + gifts.eventIds,
                    lastTimestamp = max(previous.lastTimestamp, gifts.lastTimestamp)
                )
            )
        )
        return messages.toMutableList().also { it[index] = merged }
    }

    fun fold(messages: List<DisplayMessage>): List<DisplayMessage> {
        val result = ArrayList<DisplayMessage>(messages.size)
        messages.forEach { message ->
            if (message is DisplayMessage.UserNoticeMsg && message.details?.gifts != null) {
                val merged = append(result, message)
                if (merged !== result) {
                    result.clear()
                    result.addAll(merged)
                }
            } else result += message
        }
        return result
    }

    private fun matches(first: DisplayMessage.UserNoticeMsg, second: DisplayMessage.UserNoticeMsg): Boolean {
        if (!first.channel.removePrefix("#").equals(second.channel.removePrefix("#"), true)) return false
        val a = first.details ?: return false
        val b = second.details ?: return false
        val previous = a.gifts ?: return false
        val incoming = b.gifts ?: return false
        if (previous.plan != incoming.plan || previous.anonymous != incoming.anonymous) return false
        if (!sameSender(a.author, b.author, previous.anonymous)) return false
        if (incoming.eventIds.isNotEmpty() && previous.eventIds.containsAll(incoming.eventIds)) return true
        if (previous.communityId != null && incoming.communityId != null) return previous.communityId == incoming.communityId
        if (abs(previous.lastTimestamp - incoming.lastTimestamp) > MATCH_WINDOW_MS) return false
        if (previous.hasSummary && previous.recipients.size >= previous.total) return false
        if (previous.hasSummary && incoming.hasSummary && (previous.communityId != null || incoming.communityId != null)) return false
        return true
    }

    private fun sameSender(first: ChatNoticeUser?, second: ChatNoticeUser?, anonymous: Boolean): Boolean {
        if (anonymous) return true
        if (first == null || second == null) return false
        if (first.userId.isNotBlank() && second.userId.isNotBlank()) return first.userId == second.userId
        return first.login.isNotBlank() && first.login.equals(second.login, true)
    }

    private fun recipientKey(user: ChatNoticeUser): String = user.userId.ifBlank { user.login.lowercase() }
}
