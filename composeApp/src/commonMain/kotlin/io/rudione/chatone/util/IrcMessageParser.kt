package io.rudione.chatone.util

import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.Emote
import io.rudione.chatone.domain.model.EmotePosition
import kotlinx.datetime.Clock

/**
 * Parsed representation of a raw IRC message before being converted to domain models.
 */
data class IrcMessage(
    val tags: Map<String, String>,
    val prefix: String,
    val command: String,
    val params: List<String>
) {
    val nick: String get() = prefix.substringBefore("!")
    val channel: String get() = params.firstOrNull()?.removePrefix("#") ?: ""
    val trailing: String get() = params.lastOrNull() ?: ""

    companion object {
        /**
         * Parse a raw IRC line into an IrcMessage.
         * Format: [@tags] [:prefix] COMMAND [params...] [:trailing]
         */
        fun parse(raw: String): IrcMessage? {
            if (raw.isBlank()) return null

            var remaining = raw.trim()
            var tags = emptyMap<String, String>()
            var prefix = ""

            // Parse tags
            if (remaining.startsWith("@")) {
                val spaceIdx = remaining.indexOf(' ')
                if (spaceIdx == -1) return null
                tags = parseTags(remaining.substring(1, spaceIdx))
                remaining = remaining.substring(spaceIdx + 1).trimStart()
            }

            // Parse prefix
            if (remaining.startsWith(":")) {
                val spaceIdx = remaining.indexOf(' ')
                if (spaceIdx == -1) return null
                prefix = remaining.substring(1, spaceIdx)
                remaining = remaining.substring(spaceIdx + 1).trimStart()
            }

            // Parse command
            val spaceIdx = remaining.indexOf(' ')
            val command: String
            if (spaceIdx == -1) {
                command = remaining
                remaining = ""
            } else {
                command = remaining.substring(0, spaceIdx)
                remaining = remaining.substring(spaceIdx + 1).trimStart()
            }

            // Parse params
            val params = mutableListOf<String>()
            while (remaining.isNotEmpty()) {
                if (remaining.startsWith(":")) {
                    params.add(remaining.substring(1))
                    break
                }
                val nextSpace = remaining.indexOf(' ')
                if (nextSpace == -1) {
                    params.add(remaining)
                    break
                }
                params.add(remaining.substring(0, nextSpace))
                remaining = remaining.substring(nextSpace + 1).trimStart()
            }

            return IrcMessage(tags, prefix, command, params)
        }

        private fun parseTags(tagsString: String): Map<String, String> {
            if (tagsString.isEmpty()) return emptyMap()

            return buildMap {
                tagsString.split(";").forEach { tag ->
                    val eqIdx = tag.indexOf('=')
                    if (eqIdx != -1) {
                        val key = tag.substring(0, eqIdx)
                        val value = unescapeTagValue(tag.substring(eqIdx + 1))
                        put(key, value)
                    } else {
                        put(tag, "")
                    }
                }
            }
        }

        private fun unescapeTagValue(value: String): String {
            if (!value.contains('\\')) return value
            val sb = StringBuilder(value.length)
            var i = 0
            while (i < value.length) {
                if (value[i] == '\\' && i + 1 < value.length) {
                    when (value[i + 1]) {
                        ':' -> sb.append(';')
                        's' -> sb.append(' ')
                        '\\' -> sb.append('\\')
                        'r' -> sb.append('\r')
                        'n' -> sb.append('\n')
                        else -> sb.append(value[i + 1])
                    }
                    i += 2
                } else {
                    sb.append(value[i])
                    i++
                }
            }
            return sb.toString()
        }
    }
}

object IrcMessageParser {

    fun parsePrivMsg(ircMessage: IrcMessage): ChatMessage {
        val tags = ircMessage.tags
        val channelName = ircMessage.channel
        val messageText = ircMessage.trailing

        val username = ircMessage.nick.removePrefix(":")
        val displayName = tags["display-name"] ?: username
        val userId = tags["user-id"] ?: ""
        val color = tags["color"]?.takeIf { it.isNotEmpty() }
        val timestamp = tags["tmi-sent-ts"]?.toLongOrNull() ?: Clock.System.now().toEpochMilliseconds()
        val messageId = tags["id"] ?: generateMessageId()

        val badges = parseBadges(tags["badges"] ?: "")
        val emotes = parseEmotes(tags["emotes"] ?: "", messageText)

        val isModerator = tags["mod"] == "1"
        val isSubscriber = tags["subscriber"] == "1"
        val isVip = badges.any { it.id == "vip" }
        val isBroadcaster = badges.any { it.id == "broadcaster" }

        val isAction = messageText.startsWith("\u0001ACTION ") && messageText.endsWith("\u0001")
        val finalMessage = if (isAction) {
            messageText.removePrefix("\u0001ACTION ").removeSuffix("\u0001")
        } else {
            messageText
        }

        return ChatMessage(
            id = messageId,
            channelId = tags["room-id"] ?: "",
            channelName = channelName,
            userId = userId,
            username = username,
            displayName = displayName,
            message = finalMessage,
            timestamp = timestamp,
            color = color,
            badges = badges,
            emotes = emotes,
            isModerator = isModerator,
            isSubscriber = isSubscriber,
            isVip = isVip,
            isBroadcaster = isBroadcaster,
            isMention = false,
            isAction = isAction
        )
    }

    // Legacy method for backward compat
    fun parseMessage(rawMessage: String): ChatMessage {
        val ircMessage = IrcMessage.parse(rawMessage)
            ?: throw IllegalArgumentException("Invalid IRC message format")
        return parsePrivMsg(ircMessage)
    }

    fun parseBadges(badgesString: String): List<Badge> {
        if (badgesString.isEmpty()) return emptyList()

        return badgesString.split(",").mapNotNull { badge ->
            val parts = badge.split("/")
            if (parts.size == 2) {
                Badge(
                    id = parts[0],
                    version = parts[1],
                    imageUrl = "" // Will be resolved by BadgeRepository
                )
            } else null
        }
    }

    fun parseEmotes(emotesString: String, messageText: String): List<Emote> {
        if (emotesString.isEmpty()) return emptyList()

        return emotesString.split("/").mapNotNull { emote ->
            val parts = emote.split(":")
            if (parts.size == 2) {
                val emoteId = parts[0]
                val positions = parts[1].split(",").mapNotNull { pos ->
                    val range = pos.split("-")
                    if (range.size == 2) {
                        EmotePosition(
                            start = range[0].toIntOrNull() ?: return@mapNotNull null,
                            end = range[1].toIntOrNull() ?: return@mapNotNull null
                        )
                    } else null
                }

                if (positions.isNotEmpty()) {
                    val firstPos = positions.first()
                    val emoteName = try {
                        messageText.substring(firstPos.start, (firstPos.end + 1).coerceAtMost(messageText.length))
                    } catch (e: Exception) {
                        emoteId
                    }

                    Emote(
                        id = emoteId,
                        name = emoteName,
                        positions = positions,
                        imageUrl = "https://static-cdn.jtvnw.net/emoticons/v2/$emoteId/default/dark/2.0"
                    )
                } else null
            } else null
        }
    }

    private fun generateMessageId(): String {
        return "msg_${Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
    }
}
