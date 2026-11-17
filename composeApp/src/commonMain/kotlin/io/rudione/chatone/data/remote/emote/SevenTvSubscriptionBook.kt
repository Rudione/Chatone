package io.rudione.chatone.data.remote.emote

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class SevenTvSubscriptionBook(
    private val personalSetCap: Int = DEFAULT_PERSONAL_SET_CAP
) {
    enum class Kind { CHANNEL_SET, TWITCH_CHANNEL, USER, PERSONAL_SET }

    private val lock = SynchronizedObject()
    private val registry = Kind.entries.associateWith { LinkedHashSet<String>() }
    private var limit = DEFAULT_LIMIT
    private var used = 0
    private var live = false

    fun add(kind: Kind, id: String): List<String> = synchronized(lock) {
        if (!SevenTvCommands.isValidId(id)) return@synchronized emptyList()
        val ids = registry.getValue(kind)
        if (id in ids) return@synchronized emptyList()
        if (kind == Kind.PERSONAL_SET && ids.size >= personalSetCap) return@synchronized emptyList()
        ids += id
        if (!live) return@synchronized emptyList()
        reserve(kind, id)
    }

    fun startSession(serverLimit: Int): List<String> = synchronized(lock) {
        limit = serverLimit.coerceAtLeast(0)
        used = 0
        live = true
        Kind.entries.flatMap { kind -> registry.getValue(kind).flatMap { id -> reserve(kind, id) } }
    }

    fun endSession() = synchronized(lock) { live = false }

    fun clear() = synchronized(lock) {
        registry.values.forEach { it.clear() }
        used = 0
        live = false
    }

    fun count(kind: Kind): Int = synchronized(lock) { registry.getValue(kind).size }

    private fun reserve(kind: Kind, id: String): List<String> {
        val commands = commandsFor(kind, id)
        if (used + commands.size > limit) return emptyList()
        used += commands.size
        return commands
    }

    private fun commandsFor(kind: Kind, id: String): List<String> = when (kind) {
        Kind.CHANNEL_SET, Kind.PERSONAL_SET -> listOf(SevenTvCommands.emoteSet(id))
        Kind.USER -> listOf(SevenTvCommands.user(id))
        Kind.TWITCH_CHANNEL -> SevenTvCommands.twitchChannel(id)
    }

    companion object {
        const val DEFAULT_LIMIT = 500
        const val DEFAULT_PERSONAL_SET_CAP = 200
    }
}
