package io.rudione.chatone.util.link

import kotlin.concurrent.Volatile

sealed interface TwitchLink {
    data class Channel(val login: String) : TwitchLink
    data class Clip(val slug: String) : TwitchLink
    data class Video(val id: String, val startSeconds: Int) : TwitchLink
}

object TwitchLinks {
    private val HOSTS = setOf("twitch.tv", "www.twitch.tv", "m.twitch.tv", "clips.twitch.tv")
    private val LOGIN = Regex("^[a-z0-9_]{1,25}$")
    private val SLUG = Regex("^[A-Za-z0-9_-]{3,100}$")
    private val VIDEO_ID = Regex("^[0-9]{1,20}$")
    private val TIME = Regex("^(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$")
    private val RESERVED = setOf(
        "directory", "videos", "settings", "p", "search", "downloads", "jobs", "turbo", "popout", "subs",
        "friends", "inventory", "wallet", "drops", "prime", "store", "products", "team", "moderator", "u",
        "login", "signup", "broadcast", "messages", "following", "user", "embed", "collections"
    )

    fun parse(url: String): TwitchLink? {
        val trimmed = url.trim()
        val withoutScheme = trimmed.substringAfter("://", missingDelimiterValue = "")
        if (withoutScheme.isEmpty() || !trimmed.startsWith("http")) return null
        val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#').lowercase()
        if (host !in HOSTS) return null
        val rest = withoutScheme.substringAfter('/', missingDelimiterValue = "")
        val path = rest.substringBefore('?').substringBefore('#').trim('/')
        val query = rest.substringAfter('?', missingDelimiterValue = "").substringBefore('#')
        val segments = path.split('/').filter { it.isNotEmpty() }
        if (host == "clips.twitch.tv") {
            val slug = if (segments.firstOrNull() == "embed") param(query, "clip") else segments.firstOrNull()
            return slug?.takeIf { SLUG.matches(it) }?.let(TwitchLink::Clip)
        }
        val first = segments.firstOrNull()?.lowercase() ?: return null
        return when {
            first == "videos" -> segments.getOrNull(1)?.takeIf { VIDEO_ID.matches(it) }
                ?.let { TwitchLink.Video(it, seconds(param(query, "t"))) }
            first == "popout" -> segments.getOrNull(1)?.lowercase()?.takeIf { LOGIN.matches(it) }?.let(TwitchLink::Channel)
            first in RESERVED -> null
            segments.getOrNull(1) == "clip" -> segments.getOrNull(2)?.takeIf { SLUG.matches(it) }?.let(TwitchLink::Clip)
            segments.getOrNull(1) == "v" -> segments.getOrNull(2)?.takeIf { VIDEO_ID.matches(it) }
                ?.let { TwitchLink.Video(it, seconds(param(query, "t"))) }
            LOGIN.matches(first) && segments.size <= 2 -> TwitchLink.Channel(first)
            else -> null
        }
    }

    fun channelLogin(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("http", ignoreCase = true)) return (parse(trimmed) as? TwitchLink.Channel)?.login
        val bare = trimmed.removePrefix("#").removePrefix("@")
        val login = if (bare.contains('/')) {
            bare.substringAfter("twitch.tv/", missingDelimiterValue = "").substringBefore('/').substringBefore('?')
        } else {
            bare
        }.lowercase()
        return login.takeIf { LOGIN.matches(it) && it !in RESERVED }
    }

    private fun param(query: String, name: String): String? =
        query.split('&').firstOrNull { it.substringBefore('=') == name }?.substringAfter('=', missingDelimiterValue = "")
            ?.takeIf { it.isNotEmpty() }

    private fun seconds(raw: String?): Int {
        val value = raw ?: return 0
        value.toIntOrNull()?.let { return it.coerceAtLeast(0) }
        val match = TIME.matchEntire(value) ?: return 0
        val (h, m, s) = match.destructured
        return (h.toIntOrNull() ?: 0) * 3600 + (m.toIntOrNull() ?: 0) * 60 + (s.toIntOrNull() ?: 0)
    }
}

object InAppLinkRouter {
    @Volatile
    private var handler: ((TwitchLink) -> Boolean)? = null

    fun register(value: (TwitchLink) -> Boolean) {
        handler = value
    }

    fun unregister(value: (TwitchLink) -> Boolean) {
        if (handler === value) handler = null
    }

    fun dispatch(url: String): Boolean {
        val current = handler ?: return false
        val link = TwitchLinks.parse(url) ?: return false
        return current(link)
    }
}
