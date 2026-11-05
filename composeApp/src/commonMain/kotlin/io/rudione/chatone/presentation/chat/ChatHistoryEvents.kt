package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.remote.RecentHistoryEvent
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.IrcEvent
import kotlin.math.abs

private const val DUPLICATE_WINDOW_MS = 10_000L

internal object ChatHistoryEvents {

    fun toDisplay(
        entry: RecentHistoryEvent,
        innerToDisplay: (ChatMessage) -> DisplayMessage.PrivMsg
    ): DisplayMessage? = when (val event = entry.event) {
        is IrcEvent.ClearChat -> clearChat(entry.timestamp, event)
        is IrcEvent.ClearMsg -> clearMsg(entry.timestamp, event)
        is IrcEvent.UserNotice -> userNotice(entry.timestamp, event, innerToDisplay)
        else -> null
    }

    fun isDuplicate(candidate: DisplayMessage, existing: List<DisplayMessage>): Boolean = when (candidate) {
        is DisplayMessage.ModerationMsg -> existing.any {
            it is DisplayMessage.ModerationMsg && ChatModerationEvents.isSameEvent(candidate, it)
        }

        is DisplayMessage.UserNoticeMsg -> existing.any {
            it is DisplayMessage.UserNoticeMsg && it.systemText == candidate.systemText &&
                    it.innerMessage?.rawMessage?.message == candidate.innerMessage?.rawMessage?.message &&
                    abs(it.timestamp - candidate.timestamp) <= DUPLICATE_WINDOW_MS
        }

        else -> false
    }

    fun newMessages(incoming: List<DisplayMessage>, existing: List<DisplayMessage>): List<DisplayMessage> {
        val ids = existing.mapTo(HashSet()) { it.id }
        val events = existing.filter { it !is DisplayMessage.PrivMsg }.toMutableList()
        return incoming.filter { message ->
            if (!ids.add(message.id)) return@filter false
            if (message is DisplayMessage.PrivMsg) return@filter true
            if (isDuplicate(message, events)) return@filter false
            events += message
            true
        }
    }

    fun withRewardTitles(messages: List<DisplayMessage>, titles: Map<String, String>): List<DisplayMessage> {
        if (titles.isEmpty()) return messages
        var changed = false
        val updated = messages.map { message ->
            val title = (message as? DisplayMessage.PrivMsg)?.customRewardId?.let(titles::get)
            if (title != null && title != (message as DisplayMessage.PrivMsg).rewardName) {
                changed = true
                message.copy(rewardName = title)
            } else message
        }
        return if (changed) updated else messages
    }

    private fun clearChat(timestamp: Long, event: IrcEvent.ClearChat): DisplayMessage.ModerationMsg {
        val target = event.targetUser
        if (target == null) {
            return DisplayMessage.ModerationMsg(
                id = "hist_clear_$timestamp",
                timestamp = timestamp,
                channel = event.channel,
                text = "Chat was cleared by a moderator",
                action = DisplayMessage.ModerationMsg.ModerationAction.CLEAR,
                serverTimestamp = event.tags["tmi-sent-ts"]?.toLongOrNull()
            )
        }
        val isTimeout = event.banDuration != null
        return DisplayMessage.ModerationMsg(
            id = "hist_mod_${event.tags["target-user-id"] ?: target}_$timestamp",
            timestamp = timestamp,
            channel = event.channel,
            text = if (isTimeout) "$target was timed out for ${event.banDuration}s" else "$target was banned",
            action = if (isTimeout) DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT
            else DisplayMessage.ModerationMsg.ModerationAction.BAN,
            targetUser = target,
            duration = event.banDuration,
            serverTimestamp = event.tags["tmi-sent-ts"]?.toLongOrNull()
        )
    }

    private fun clearMsg(timestamp: Long, event: IrcEvent.ClearMsg): DisplayMessage.ModerationMsg {
        val target = event.login.takeIf { it.isNotBlank() }
        return DisplayMessage.ModerationMsg(
            id = "hist_del_${event.targetMessageId.ifEmpty { timestamp.toString() }}",
            timestamp = timestamp,
            channel = event.channel,
            text = if (target != null) "Message from $target was deleted" else "A message was deleted",
            action = DisplayMessage.ModerationMsg.ModerationAction.DELETE,
            targetUser = target,
            targetMessageId = event.targetMessageId.takeIf { it.isNotBlank() }
        )
    }

    private fun userNotice(
        timestamp: Long,
        event: IrcEvent.UserNotice,
        innerToDisplay: (ChatMessage) -> DisplayMessage.PrivMsg
    ): DisplayMessage.UserNoticeMsg {
        val isAnnouncement = (event.msgId ?: "").equals("announcement", ignoreCase = true)
        val noticeId = event.tags["id"]?.takeIf { it.isNotBlank() } ?: timestamp.toString()
        val inner = event.message?.let(innerToDisplay)?.let { raw ->
            val innerId = "inner_${raw.id.ifEmpty { noticeId }}"
            if (isAnnouncement) raw.copy(
                id = innerId,
                isHighlighted = false,
                isMention = false,
                isFirstMessage = false,
                highlightColor = null
            ) else raw.copy(id = innerId)
        }
        return DisplayMessage.UserNoticeMsg(
            id = "hist_notice_$noticeId",
            timestamp = timestamp,
            channel = event.channel,
            systemText = event.systemMsg,
            innerMessage = inner,
            noticeType = event.msgId ?: "",
            announceColor = event.tags["msg-param-color"]?.takeIf { it.isNotBlank() }
        )
    }
}
