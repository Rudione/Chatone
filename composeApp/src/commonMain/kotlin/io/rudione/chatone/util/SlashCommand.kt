package io.rudione.chatone.util

object SlashCommand {

    data class CommandInfo(
        val name: String,
        val aliases: List<String>,
        val usage: String,
        val description: String,
        val requiresMod: Boolean = false,
        val requiresBroadcaster: Boolean = false
    )

    val ALL: List<CommandInfo> = listOf(
        CommandInfo(
            "ban",
            listOf("ban"),
            "/ban <user> [reason]",
            "Permanently ban a user",
            requiresMod = true
        ),
        CommandInfo(
            "unban",
            listOf("unban", "untimeout"),
            "/unban <user>",
            "Lift a ban or timeout",
            requiresMod = true
        ),
        CommandInfo(
            "timeout",
            listOf("timeout", "to"),
            "/timeout <user> [duration=600] [reason]",
            "Time a user out",
            requiresMod = true
        ),
        CommandInfo(
            "clear",
            listOf("clear"),
            "/clear",
            "Clear chat for everyone",
            requiresMod = true
        ),
        CommandInfo(
            "raid",
            listOf("raid"),
            "/raid <channel>",
            "Raid another channel",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "unraid",
            listOf("unraid"),
            "/unraid",
            "Cancel an outgoing raid",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "pin",
            listOf("pin"),
            "/pin <messageId>",
            "Pin a message by ID",
            requiresMod = true
        ),
        CommandInfo(
            "unpin",
            listOf("unpin"),
            "/unpin",
            "Unpin the current pinned message",
            requiresMod = true
        ),
        CommandInfo(
            "announce",
            listOf("announce", "announceblue", "announcegreen", "announceorange", "announcepurple"),
            "/announce[blue|green|orange|purple] <text>",
            "Send a coloured announcement",
            requiresMod = true
        ),
        CommandInfo(
            "slow",
            listOf("slow"),
            "/slow [seconds=30]",
            "Enable slow-mode",
            requiresMod = true
        ),
        CommandInfo(
            "slowoff",
            listOf("slowoff"),
            "/slowoff",
            "Disable slow-mode",
            requiresMod = true
        ),
        CommandInfo(
            "followers",
            listOf("followers"),
            "/followers [duration=0]",
            "Follower-only mode",
            requiresMod = true
        ),
        CommandInfo(
            "followersoff",
            listOf("followersoff"),
            "/followersoff",
            "Disable follower-only",
            requiresMod = true
        ),
        CommandInfo(
            "subscribers",
            listOf("subscribers"),
            "/subscribers",
            "Subscriber-only mode",
            requiresMod = true
        ),
        CommandInfo(
            "subscribersoff",
            listOf("subscribersoff"),
            "/subscribersoff",
            "Disable sub-only",
            requiresMod = true
        ),
        CommandInfo(
            "emoteonly",
            listOf("emoteonly"),
            "/emoteonly",
            "Emote-only mode",
            requiresMod = true
        ),
        CommandInfo(
            "emoteonlyoff",
            listOf("emoteonlyoff"),
            "/emoteonlyoff",
            "Disable emote-only",
            requiresMod = true
        ),
        CommandInfo(
            "uniquechat",
            listOf("uniquechat", "r9kbeta"),
            "/uniquechat",
            "Enable unique-chat (R9K)",
            requiresMod = true
        ),
        CommandInfo(
            "uniquechatoff",
            listOf("uniquechatoff", "r9kbetaoff"),
            "/uniquechatoff",
            "Disable unique-chat",
            requiresMod = true
        ),
        CommandInfo("vip", listOf("vip"), "/vip <user>", "Add a VIP", requiresBroadcaster = true),
        CommandInfo(
            "unvip",
            listOf("unvip"),
            "/unvip <user>",
            "Remove a VIP",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "mod",
            listOf("mod"),
            "/mod <user>",
            "Add a moderator",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "unmod",
            listOf("unmod"),
            "/unmod <user>",
            "Remove a moderator",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "shoutout",
            listOf("shoutout", "so"),
            "/shoutout <user>",
            "Send a shoutout",
            requiresMod = true
        ),
        CommandInfo(
            "warn",
            listOf("warn"),
            "/warn <user> <reason>",
            "Issue a warning",
            requiresMod = true
        ),
        CommandInfo("color", listOf("color"), "/color <name|#hex>", "Change your chat color"),
        CommandInfo("me", listOf("me"), "/me <text>", "Action message"),
        CommandInfo("w", listOf("w"), "/w <user> <text>", "Whisper a user"),
        CommandInfo("block", listOf("block"), "/block <user>", "Block a user"),
        CommandInfo("unblock", listOf("unblock"), "/unblock <user>", "Unblock a user"),
        CommandInfo("help", listOf("help", "commands"), "/help", "Show this command list"),
        CommandInfo(
            "spam",
            listOf("spam"),
            "/spam <count> [delayMs] <message> | /spam stop",
            "Send a message N times (max 30). Optional 2nd arg = delay in ms (50–60000). /spam stop cancels"
        ),
        CommandInfo(
            "poll",
            listOf("poll"),
            "/poll <duration> <title> | <choice1> | <choice2> ...",
            "Start a Twitch poll. Duration in sec (15–1800). 2–5 choices separated by '|'",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "endpoll",
            listOf("endpoll"),
            "/endpoll",
            "End the active poll and show results",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "cancelpoll",
            listOf("cancelpoll"),
            "/cancelpoll",
            "Cancel the active poll (no results shown)",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "prediction",
            listOf("prediction"),
            "/prediction <window> <title> | <outcome1> | <outcome2> ...",
            "Start a prediction. Window in sec (30–1800). 2–10 outcomes",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "lockprediction",
            listOf("lockprediction"),
            "/lockprediction",
            "Lock the active prediction (no more bets)",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "cancelprediction",
            listOf("cancelprediction"),
            "/cancelprediction",
            "Cancel the active prediction and refund points",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "completeprediction",
            listOf("completeprediction"),
            "/completeprediction <outcome# or text>",
            "Resolve the prediction picking the winning outcome",
            requiresBroadcaster = true
        ),
        CommandInfo(
            "user",
            listOf("user"),
            "/user <nickname>",
            "Open user profile popup"
        ),
    )

    fun suggest(input: String, isMod: Boolean, isBroadcaster: Boolean): List<CommandInfo> {
        if (!input.startsWith("/")) return emptyList()
        val firstSpace = input.indexOf(' ')
        if (firstSpace >= 0) return emptyList()
        val typed = input.removePrefix("/").lowercase()
        return ALL.asSequence()
            .filter { ci ->
                when {
                    ci.requiresBroadcaster -> isBroadcaster
                    ci.requiresMod -> isMod || isBroadcaster
                    else -> true
                }
            }
            .filter { ci ->
                typed.isEmpty() || ci.aliases.any { it.startsWith(typed) }
            }
            .distinctBy { it.name }
            .take(8)
            .toList()
    }

    sealed class Parsed {
        data class Ban(val targetLogin: String, val reason: String?) : Parsed()
        data class Timeout(val targetLogin: String, val seconds: Int, val reason: String?) :
            Parsed()

        data class Unban(val targetLogin: String) : Parsed()
        object Clear : Parsed()
        data class Raid(val targetLogin: String) : Parsed()
        object UnRaid : Parsed()
        data class Pin(val messageId: String) : Parsed()
        object Unpin : Parsed()
        data class Announce(val text: String, val color: String) : Parsed()
        data class Slow(val seconds: Int) : Parsed()
        object SlowOff : Parsed()
        data class Followers(val seconds: Int) : Parsed()
        object FollowersOff : Parsed()
        object SubsOnly : Parsed()
        object SubsOnlyOff : Parsed()
        object EmoteOnly : Parsed()
        object EmoteOnlyOff : Parsed()
        object UniqueOn : Parsed()
        object UniqueOff : Parsed()
        data class Vip(val targetLogin: String) : Parsed()
        data class UnVip(val targetLogin: String) : Parsed()
        data class Mod(val targetLogin: String) : Parsed()
        data class UnMod(val targetLogin: String) : Parsed()
        data class Shoutout(val targetLogin: String) : Parsed()
        data class Color(val value: String) : Parsed()
        data class Warn(val targetLogin: String, val reason: String) : Parsed()
        data class Me(val text: String) : Parsed()
        data class Whisper(val targetLogin: String, val text: String) : Parsed()
        data class Block(val targetLogin: String) : Parsed()
        data class Unblock(val targetLogin: String) : Parsed()
        object Help : Parsed()
        data class Spam(val count: Int, val message: String, val delayMs: Long = 1500L) : Parsed()
        object SpamStop : Parsed()
        data class Poll(val durationSeconds: Int, val title: String, val choices: List<String>) :
            Parsed()

        object EndPoll : Parsed()
        object CancelPoll : Parsed()
        data class Prediction(
            val windowSeconds: Int,
            val title: String,
            val outcomes: List<String>
        ) : Parsed()

        object LockPrediction : Parsed()
        object CancelPrediction : Parsed()
        data class CompletePrediction(val outcomeRef: String) : Parsed()
        data class Unknown(val name: String, val rest: String) : Parsed()
        data class BadUsage(val name: String, val usage: String) : Parsed()

        data class User(val targetLogin: String) : Parsed()
    }

    fun parse(line: String): Parsed? {
        val trimmed = line.trim()
        if (!trimmed.startsWith("/")) return null
        val space = trimmed.indexOf(' ')
        val name =
            (if (space < 0) trimmed.substring(1) else trimmed.substring(1, space)).lowercase()
        val rest = if (space < 0) "" else trimmed.substring(space + 1).trim()
        if (name.isEmpty()) return null

        fun firstWord() = rest.substringBefore(' ').trim().removePrefix("@").lowercase()
        fun afterFirst() = rest.substringAfter(' ', "").trim()

        return when (name) {
            "ban" -> {
                val target = firstWord()
                if (target.isEmpty()) Parsed.BadUsage("ban", "/ban <user> [reason]")
                else Parsed.Ban(target, afterFirst().ifBlank { null })
            }

            "unban", "untimeout" -> {
                val target = firstWord()
                if (target.isEmpty()) Parsed.BadUsage("unban", "/unban <user>")
                else Parsed.Unban(target)
            }

            "timeout", "to" -> {
                val parts = rest.split(' ', limit = 3)
                val target = parts.getOrNull(0)?.trim()?.removePrefix("@")?.lowercase().orEmpty()
                if (target.isEmpty()) Parsed.BadUsage(
                    "timeout",
                    "/timeout <user> [duration] [reason]"
                )
                else {
                    val secs = parts.getOrNull(1)?.let { parseDuration(it) } ?: 600
                    val reason = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
                    Parsed.Timeout(target, secs.coerceIn(1, 1_209_600), reason)
                }
            }

            "clear" -> Parsed.Clear
            "raid" -> {
                val t = firstWord()
                if (t.isEmpty()) Parsed.BadUsage("raid", "/raid <channel>") else Parsed.Raid(t)
            }

            "unraid" -> Parsed.UnRaid
            "pin" -> {
                val t = firstWord()
                if (t.isEmpty()) Parsed.BadUsage("pin", "/pin <messageId>") else Parsed.Pin(t)
            }

            "unpin" -> Parsed.Unpin
            "announce" -> if (rest.isEmpty()) Parsed.BadUsage(
                "announce",
                "/announce <text>"
            ) else Parsed.Announce(rest, "primary")

            "announceblue" -> if (rest.isEmpty()) Parsed.BadUsage(
                "announceblue",
                "/announceblue <text>"
            ) else Parsed.Announce(rest, "blue")

            "announcegreen" -> if (rest.isEmpty()) Parsed.BadUsage(
                "announcegreen",
                "/announcegreen <text>"
            ) else Parsed.Announce(rest, "green")

            "announceorange" -> if (rest.isEmpty()) Parsed.BadUsage(
                "announceorange",
                "/announceorange <text>"
            ) else Parsed.Announce(rest, "orange")

            "announcepurple" -> if (rest.isEmpty()) Parsed.BadUsage(
                "announcepurple",
                "/announcepurple <text>"
            ) else Parsed.Announce(rest, "purple")

            "slow" -> Parsed.Slow(parseDuration(firstWord()) ?: 30)
            "slowoff" -> Parsed.SlowOff
            "followers" -> Parsed.Followers(parseDuration(firstWord()) ?: 0)
            "followersoff" -> Parsed.FollowersOff
            "subscribers" -> Parsed.SubsOnly
            "subscribersoff" -> Parsed.SubsOnlyOff
            "emoteonly", "emoteonlymode" -> Parsed.EmoteOnly
            "emoteonlyoff" -> Parsed.EmoteOnlyOff
            "uniquechat", "r9kbeta", "r9kbetamode" -> Parsed.UniqueOn
            "uniquechatoff", "r9kbetaoff" -> Parsed.UniqueOff
            "vip" -> firstWord().takeIf { it.isNotEmpty() }?.let { Parsed.Vip(it) }
                ?: Parsed.BadUsage("vip", "/vip <user>")

            "unvip" -> firstWord().takeIf { it.isNotEmpty() }?.let { Parsed.UnVip(it) }
                ?: Parsed.BadUsage("unvip", "/unvip <user>")

            "mod" -> firstWord().takeIf { it.isNotEmpty() }?.let { Parsed.Mod(it) }
                ?: Parsed.BadUsage("mod", "/mod <user>")

            "unmod" -> firstWord().takeIf { it.isNotEmpty() }?.let { Parsed.UnMod(it) }
                ?: Parsed.BadUsage("unmod", "/unmod <user>")

            "shoutout", "so" -> firstWord().takeIf { it.isNotEmpty() }?.let { Parsed.Shoutout(it) }
                ?: Parsed.BadUsage("shoutout", "/shoutout <user>")

            "color" -> rest.takeIf { it.isNotEmpty() }?.let { Parsed.Color(it.trim()) }
                ?: Parsed.BadUsage("color", "/color <name|#hex>")

            "warn" -> {
                val t = firstWord()
                val reason = afterFirst()
                if (t.isEmpty() || reason.isEmpty()) Parsed.BadUsage(
                    "warn",
                    "/warn <user> <reason>"
                )
                else Parsed.Warn(t, reason)
            }

            "me" -> rest.takeIf { it.isNotEmpty() }?.let { Parsed.Me(it) } ?: Parsed.BadUsage(
                "me",
                "/me <text>"
            )

            "w", "whisper" -> {
                val t = firstWord()
                val text = afterFirst()
                if (t.isEmpty() || text.isEmpty()) Parsed.BadUsage("w", "/w <user> <text>")
                else Parsed.Whisper(t, text)
            }

            "block" -> firstWord().takeIf { it.isNotEmpty() }
                ?.let { Parsed.Block(it) }
                ?: Parsed.BadUsage("block", "/block <user>")

            "unblock" -> firstWord().takeIf { it.isNotEmpty() }
                ?.let { Parsed.Unblock(it) }
                ?: Parsed.BadUsage("unblock", "/unblock <user>")

            "help", "commands" -> Parsed.Help
            "poll" -> {
                val firstSpace = rest.indexOf(' ')
                if (firstSpace < 0) Parsed.BadUsage(
                    "poll",
                    "/poll <duration> <title> | <choice1> | <choice2> ..."
                )
                else {
                    val duration = rest.substring(0, firstSpace).toIntOrNull()
                    val tail = rest.substring(firstSpace + 1).trim()
                    val parts = tail.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                    if (duration == null || parts.size < 3)
                        Parsed.BadUsage(
                            "poll",
                            "/poll <duration> <title> | <choice1> | <choice2> ... (need title + 2–5 choices)"
                        )
                    else Parsed.Poll(
                        durationSeconds = duration.coerceIn(15, 1800),
                        title = parts[0].take(60),
                        choices = parts.drop(1).take(5).map { it.take(25) }
                    )
                }
            }

            "endpoll" -> Parsed.EndPoll
            "cancelpoll" -> Parsed.CancelPoll
            "prediction" -> {
                val firstSpace = rest.indexOf(' ')
                if (firstSpace < 0) Parsed.BadUsage(
                    "prediction",
                    "/prediction <window> <title> | <out1> | <out2> ..."
                )
                else {
                    val window = rest.substring(0, firstSpace).toIntOrNull()
                    val tail = rest.substring(firstSpace + 1).trim()
                    val parts = tail.split('|').map { it.trim() }.filter { it.isNotEmpty() }
                    if (window == null || parts.size < 3)
                        Parsed.BadUsage(
                            "prediction",
                            "/prediction <window> <title> | <out1> | <out2> ... (need title + 2–10 outcomes)"
                        )
                    else Parsed.Prediction(
                        windowSeconds = window.coerceIn(30, 1800),
                        title = parts[0].take(45),
                        outcomes = parts.drop(1).take(10).map { it.take(25) }
                    )
                }
            }
            "user" -> firstWord().takeIf { it.isNotEmpty() }
                ?.let { Parsed.User(it) }
                ?: Parsed.BadUsage("user", "/user <nickname>")

            "lockprediction" -> Parsed.LockPrediction
            "cancelprediction" -> Parsed.CancelPrediction
            "completeprediction" -> {
                val arg = rest.trim()
                if (arg.isEmpty()) Parsed.BadUsage(
                    "completeprediction",
                    "/completeprediction <outcome# or text>"
                )
                else Parsed.CompletePrediction(arg)
            }

            "spam" -> {
                val firstWord = rest.substringBefore(' ').trim()
                if (firstWord.equals("stop", ignoreCase = true)) Parsed.SpamStop
                else {

                    val parts = rest.split(' ', limit = 3)
                    val count = parts.getOrNull(0)?.toIntOrNull()
                    val maybeDelay = parts.getOrNull(1)?.toLongOrNull()
                    val msg: String
                    val delayMs: Long
                    if (maybeDelay != null) {
                        delayMs = maybeDelay.coerceIn(50L, 60_000L)
                        msg = parts.getOrNull(2)?.trim().orEmpty()
                    } else {
                        delayMs = 1500L
                        msg = rest.substringAfter(' ', "").trim()
                    }
                    if (count == null || count <= 0 || msg.isEmpty())
                        Parsed.BadUsage("spam", "/spam <count> [delayMs] <message>  or  /spam stop")
                    else Parsed.Spam(count.coerceAtMost(30), msg, delayMs)
                }
            }

            else -> Parsed.Unknown(name, rest)
        }
    }

    private fun parseDuration(token: String?): Int? {
        if (token.isNullOrBlank()) return null
        val t = token.trim().lowercase()
        val (numPart, unit) = when (val u = t.lastOrNull()) {
            's', 'm', 'h', 'd', 'w' -> t.dropLast(1) to u
            else -> t to 's'
        }
        val n = numPart.toIntOrNull() ?: return null
        return when (unit) {
            's' -> n
            'm' -> n * 60
            'h' -> n * 3600
            'd' -> n * 86_400
            'w' -> n * 604_800
            else -> n
        }
    }
}