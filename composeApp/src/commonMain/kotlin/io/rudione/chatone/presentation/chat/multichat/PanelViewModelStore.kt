package io.rudione.chatone.presentation.chat.multichat

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner


class PanelViewModelStore : ViewModelStoreOwner {
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    fun clear() {
        store.clear()
    }
}


class PanelViewModelStoreRegistry {
    private val owners = mutableMapOf<String, PanelViewModelStore>()

    fun getOrCreate(panelId: String): PanelViewModelStore =
        owners.getOrPut(panelId) { PanelViewModelStore() }

    fun release(panelId: String) {
        owners.remove(panelId)?.clear()
    }

    fun releaseAll() {
        owners.values.forEach { it.clear() }
        owners.clear()
    }
}
