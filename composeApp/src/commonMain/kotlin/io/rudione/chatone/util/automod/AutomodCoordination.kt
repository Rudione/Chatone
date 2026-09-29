package io.rudione.chatone.util.automod

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

object ChatRuleClaims {

    private const val CAPACITY = 4_096

    private val lock = SynchronizedObject()
    private val claimed = LinkedHashSet<String>()

    fun claim(messageId: String): Boolean {
        if (messageId.isBlank()) return true
        return synchronized(lock) {
            if (!claimed.add(messageId)) return@synchronized false
            if (claimed.size > CAPACITY) claimed.remove(claimed.first())
            true
        }
    }

    fun clear() = synchronized(lock) { claimed.clear() }
}

object DisplayedChatChannels {

    private val lock = SynchronizedObject()
    private val holders = HashMap<String, Int>()

    fun acquire(channelLogin: String) {
        val key = normalize(channelLogin) ?: return
        synchronized(lock) { holders[key] = (holders[key] ?: 0) + 1 }
    }

    fun release(channelLogin: String) {
        val key = normalize(channelLogin) ?: return
        synchronized(lock) {
            val remaining = (holders[key] ?: 0) - 1
            if (remaining > 0) holders[key] = remaining else holders.remove(key)
        }
    }

    fun isDisplayed(channelLogin: String): Boolean {
        val key = normalize(channelLogin) ?: return false
        return synchronized(lock) { key in holders }
    }

    private fun normalize(channelLogin: String): String? =
        channelLogin.trim().removePrefix("#").lowercase().takeIf { it.isNotEmpty() }
}
