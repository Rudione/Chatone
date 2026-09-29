package io.rudione.chatone.presentation.chat.multichat

import io.rudione.chatone.presentation.chat.ChatViewModel
import org.koin.mp.KoinPlatformTools

class ChatViewModelPerPanelFactory {
    private val cache = mutableMapOf<String, ChatViewModel>()

    fun get(panelId: String): ChatViewModel {
        cache[panelId]?.let { return it }
        val vm = KoinPlatformTools.defaultContext().get().get<ChatViewModel>()
        cache[panelId] = vm
        return vm
    }

    fun release(panelId: String) {
        cache.remove(panelId)
    }

    fun releaseAll() {
        cache.clear()
    }
}
