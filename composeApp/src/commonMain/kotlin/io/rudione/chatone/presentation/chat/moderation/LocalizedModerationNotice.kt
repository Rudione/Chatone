package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.format

object LocalizedModerationNotice {

    fun ban(strings: AppStrings, target: String, durationSec: Int?, moderator: String?): String {
        val core = if (durationSec != null) {
            "$target was timed out for ${durationSec}s"
        } else {
            "$target was banned"
        }
        return if (!moderator.isNullOrBlank()) core + strings.format(strings.modActionBy, moderator)
        else core
    }

    fun delete(strings: AppStrings, target: String?, moderator: String?): String {
        val core = if (!target.isNullOrBlank()) {
            "Message from $target was deleted"
        } else {
            "A message was deleted"
        }
        return if (!moderator.isNullOrBlank()) core + strings.format(strings.modActionBy, moderator)
        else core
    }

    fun unban(strings: AppStrings, target: String, moderator: String?): String {
        val core = strings.format(strings.modUnbannedBy, target)
        return if (!moderator.isNullOrBlank()) core + strings.format(strings.modActionBy, moderator) else core
    }

    fun untimeout(strings: AppStrings, target: String, moderator: String?): String {
        val core = strings.format(strings.modUntimedoutBy, target)
        return if (!moderator.isNullOrBlank()) core + strings.format(strings.modActionBy, moderator) else core
    }

    fun clearAll(strings: AppStrings, moderator: String?): String =
        if (!moderator.isNullOrBlank()) "Chat cleared" + strings.format(strings.modActionBy, moderator)
        else "Chat was cleared by a moderator"
}
