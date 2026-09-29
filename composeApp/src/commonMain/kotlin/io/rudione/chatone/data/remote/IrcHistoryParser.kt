package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.util.chat.IrcMessage
import io.rudione.chatone.util.chat.IrcMessageParser

internal object IrcHistoryParser {

    fun parse(lines: List<String>): RecentMessagesResult.Success {
        val messages = ArrayList<ChatMessage>(lines.size)
        val deletedIds = HashSet<String>()
        val events = ArrayList<RecentHistoryEvent>()
        lines.forEach { raw ->
            val ircMsg = runCatching { IrcMessage.parse(raw) }.getOrNull() ?: return@forEach
            val timestamp = (ircMsg.tags["tmi-sent-ts"] ?: ircMsg.tags["rm-received-ts"])
                ?.toLongOrNull() ?: return@forEach
            runCatching {
                when (ircMsg.command) {
                    "PRIVMSG" -> {
                        val message = IrcMessageParser.parsePrivMsg(ircMsg)
                        messages += message
                        if (ircMsg.tags["rm-deleted"] == "1") deletedIds += message.id
                    }

                    "CLEARCHAT" -> events += RecentHistoryEvent(
                        timestamp,
                        IrcEvent.ClearChat(
                            channel = ircMsg.channel,
                            targetUser = if (ircMsg.params.size > 1) ircMsg.trailing else null,
                            banDuration = ircMsg.tags["ban-duration"]?.toIntOrNull(),
                            tags = ircMsg.tags
                        )
                    )

                    "CLEARMSG" -> {
                        val targetId = ircMsg.tags["target-msg-id"].orEmpty()
                        if (targetId.isNotEmpty()) deletedIds += targetId
                        events += RecentHistoryEvent(
                            timestamp,
                            IrcEvent.ClearMsg(
                                channel = ircMsg.channel,
                                targetMessageId = targetId,
                                message = ircMsg.trailing,
                                login = ircMsg.tags["login"].orEmpty()
                            )
                        )
                    }

                    "USERNOTICE" -> events += RecentHistoryEvent(
                        timestamp,
                        IrcEvent.UserNotice(
                            channel = ircMsg.channel,
                            systemMsg = ircMsg.tags["system-msg"]?.replace("\\s", " ").orEmpty(),
                            message = if (ircMsg.params.size > 1) IrcMessageParser.parsePrivMsg(ircMsg) else null,
                            msgId = ircMsg.tags["msg-id"],
                            tags = ircMsg.tags
                        )
                    )
                }
            }
        }
        return RecentMessagesResult.Success(messages, deletedIds, events)
    }
}
