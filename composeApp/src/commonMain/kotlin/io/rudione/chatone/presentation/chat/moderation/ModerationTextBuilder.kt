package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.format


object ModerationTextBuilder {

    fun ban(strings: AppStrings, target: String, moderator: String?): String {
        val base = "$target was banned"
        return if (!moderator.isNullOrBlank()) base + strings.format(strings.modActionBy, moderator)
        else base
    }

    fun timeout(strings: AppStrings, target: String, durationSeconds: Int, moderator: String?): String {
        val base = "$target was timed out for ${durationSeconds}s"
        return if (!moderator.isNullOrBlank()) base + strings.format(strings.modActionBy, moderator)
        else base
    }

    fun unban(strings: AppStrings, target: String, moderator: String?): String {
        val raw = strings.format(strings.modUnbannedBy, target)
        return if (!moderator.isNullOrBlank()) raw + strings.format(strings.modActionBy, moderator)
        else raw
    }

    fun untimeout(strings: AppStrings, target: String, moderator: String?): String {
        val raw = strings.format(strings.modUntimedoutBy, target)
        return if (!moderator.isNullOrBlank()) raw + strings.format(strings.modActionBy, moderator)
        else raw
    }

    fun delete(strings: AppStrings, target: String?, moderator: String?): String {
        val base = if (!target.isNullOrBlank()) "Message from $target was deleted"
        else "A message was deleted"
        return if (!moderator.isNullOrBlank()) base + strings.format(strings.modActionBy, moderator)
        else base
    }

    fun mod(strings: AppStrings, target: String, moderator: String?): String {
        val raw = strings.format(strings.modModdedBy, target)
        return if (!moderator.isNullOrBlank()) raw + strings.format(strings.modActionBy, moderator)
        else raw
    }

    fun unmod(strings: AppStrings, target: String, moderator: String?): String {
        val raw = strings.format(strings.modUnmoddedBy, target)
        return if (!moderator.isNullOrBlank()) raw + strings.format(strings.modActionBy, moderator)
        else raw
    }

    fun vip(strings: AppStrings, target: String, moderator: String?): String {
        val raw = strings.format(strings.modVippedBy, target)
        return if (!moderator.isNullOrBlank()) raw + strings.format(strings.modActionBy, moderator)
        else raw
    }

    fun unvip(strings: AppStrings, target: String, moderator: String?): String {
        val raw = strings.format(strings.modUnvippedBy, target)
        return if (!moderator.isNullOrBlank()) raw + strings.format(strings.modActionBy, moderator)
        else raw
    }

    fun clear(strings: AppStrings, moderator: String?): String {
        val base = "Chat was cleared by a moderator"
        return if (!moderator.isNullOrBlank()) "Chat was cleared" + strings.format(strings.modActionBy, moderator)
        else base
    }


    fun fromEvent(strings: AppStrings, event: IrcEvent.ModeratorAction): String {
        val target = event.target ?: "—"
        return when (event.action) {
            IrcEvent.ModeratorAction.ACTION_BAN -> ban(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_TIMEOUT -> timeout(strings, target, event.duration ?: 0, event.moderator)
            IrcEvent.ModeratorAction.ACTION_UNBAN -> unban(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_UNTIMEOUT -> untimeout(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_DELETE -> delete(strings, event.target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_MOD -> mod(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_UNMOD -> unmod(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_VIP -> vip(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_UNVIP -> unvip(strings, target, event.moderator)
            IrcEvent.ModeratorAction.ACTION_CLEAR -> clear(strings, event.moderator)
            else -> strings.modActionUnknown
        }
    }
}
