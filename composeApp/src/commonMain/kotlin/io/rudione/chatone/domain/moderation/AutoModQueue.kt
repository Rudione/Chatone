package io.rudione.chatone.domain.moderation

import io.rudione.chatone.domain.model.IrcEvent

class AutoModQueue(
    private val maxPerChannel: Int = 50,
    private val ttlMs: Long = 15L * 60 * 1000,
    private val resolvedMemory: Int = 500
) {
    private val byChannel = LinkedHashMap<String, LinkedHashMap<String, IrcEvent.AutoModHeld>>()
    private val resolved = LinkedHashSet<String>()

    fun onHeld(event: IrcEvent.AutoModHeld): Boolean {
        if (event.msgId in resolved) return false
        val channel = byChannel.getOrPut(event.channel) { LinkedHashMap() }
        if (event.msgId in channel) return false
        channel[event.msgId] = event
        while (channel.size > maxPerChannel) channel.remove(channel.keys.first())
        return true
    }

    fun onResolved(event: IrcEvent.AutoModResolved) {
        byChannel[event.channel]?.remove(event.msgId) ?: byChannel.values.forEach { it.remove(event.msgId) }
        resolved += event.msgId
        while (resolved.size > resolvedMemory) resolved.remove(resolved.first())
    }

    fun isResolved(msgId: String): Boolean = msgId in resolved

    fun snapshot(nowMs: Long): Map<String, List<IrcEvent.AutoModHeld>> {
        byChannel.values.forEach { channel -> channel.values.removeAll { nowMs - it.heldAtMs > ttlMs } }
        byChannel.entries.removeAll { it.value.isEmpty() }
        return byChannel.mapValues { it.value.values.toList() }
    }

    fun clear() {
        byChannel.clear()
        resolved.clear()
    }
}
