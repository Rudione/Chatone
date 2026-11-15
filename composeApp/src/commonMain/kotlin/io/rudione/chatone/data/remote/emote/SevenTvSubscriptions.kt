package io.rudione.chatone.data.remote.emote

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class SevenTvSubscriptions {
    private val lock = SynchronizedObject()
    private val ids = mutableSetOf<String>()

    fun add(id: String): Boolean = synchronized(lock) { ids.add(id) }

    fun remove(id: String) = synchronized(lock) { ids.remove(id) }

    fun snapshot(): List<String> = synchronized(lock) { ids.toList() }

    fun clear() = synchronized(lock) { ids.clear() }
}
