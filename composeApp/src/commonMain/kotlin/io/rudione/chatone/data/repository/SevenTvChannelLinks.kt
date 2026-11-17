package io.rudione.chatone.data.repository

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

data class SevenTvChannelLink(
    val twitchChannelId: String,
    val emoteSetId: String?,
    val sevenTvUserId: String?
)

internal class SevenTvChannelLinks {
    private val lock = SynchronizedObject()
    private val byChannel = HashMap<String, SevenTvChannelLink>()
    private val bySet = HashMap<String, MutableSet<String>>()
    private val byUser = HashMap<String, MutableSet<String>>()

    fun get(channel: String): SevenTvChannelLink? = synchronized(lock) { byChannel[channel] }

    fun put(channel: String, link: SevenTvChannelLink) = synchronized(lock) { store(channel, link) }

    fun replace(channel: String, expected: SevenTvChannelLink, updated: SevenTvChannelLink): Boolean =
        synchronized(lock) {
            if (byChannel[channel] != expected) return@synchronized false
            store(channel, updated)
            true
        }

    fun remove(channel: String) = synchronized(lock) {
        byChannel.remove(channel)?.let { unindex(channel, it) }
    }

    fun channelsUsingSet(emoteSetId: String): List<String> =
        synchronized(lock) { bySet[emoteSetId]?.toList().orEmpty() }

    fun channelsForUser(sevenTvUserId: String): List<String> =
        synchronized(lock) { byUser[sevenTvUserId]?.toList().orEmpty() }

    private fun store(channel: String, link: SevenTvChannelLink) {
        byChannel.put(channel, link)?.let { unindex(channel, it) }
        link.emoteSetId?.let { bySet.getOrPut(it) { LinkedHashSet() } += channel }
        link.sevenTvUserId?.let { byUser.getOrPut(it) { LinkedHashSet() } += channel }
    }

    private fun unindex(channel: String, link: SevenTvChannelLink) {
        link.emoteSetId?.let { bySet.detach(it, channel) }
        link.sevenTvUserId?.let { byUser.detach(it, channel) }
    }

    private fun HashMap<String, MutableSet<String>>.detach(key: String, channel: String) {
        val channels = this[key] ?: return
        channels -= channel
        if (channels.isEmpty()) remove(key)
    }
}
