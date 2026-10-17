package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.mutableStateListOf

object ChatSessions {

    private class Entry(val channel: String, val viewModel: ChatViewModel)

    private val entries = mutableStateListOf<Entry>()

    fun attach(channelLogin: String, viewModel: ChatViewModel): () -> Unit {
        val entry = Entry(normalize(channelLogin), viewModel)
        if (entry.channel.isEmpty()) return {}
        entries += entry
        return { entries.remove(entry) }
    }

    fun find(channelLogin: String): ChatViewModel? {
        val key = normalize(channelLogin)
        return entries.lastOrNull { it.channel == key }?.viewModel
    }

    private fun normalize(channelLogin: String): String = channelLogin.trim().removePrefix("#").lowercase()
}
