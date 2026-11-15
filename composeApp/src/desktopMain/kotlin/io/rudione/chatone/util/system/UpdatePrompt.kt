package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class UpdatePrompt {
    private val _available = MutableStateFlow<AutoUpdater.Available?>(null)
    val available = _available.asStateFlow()

    private val _visible = MutableStateFlow(false)
    val visible = _visible.asStateFlow()

    fun offer(entry: AutoUpdater.Available) {
        val changed = _available.value?.version != entry.version
        _available.value = entry
        if (changed) _visible.value = true
    }

    fun dismiss() {
        _visible.value = false
    }

    fun reopen() {
        if (_available.value != null) _visible.value = true
    }

    fun clear() {
        _visible.value = false
        _available.value = null
    }
}
