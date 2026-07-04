package io.rudione.chatone.util

enum class HotkeyAction(val id: String, val defaultCombo: String) {
    TOGGLE_NAVIGATION("toggle_navigation", "ctrl+u"),
    TOGGLE_SIDEBAR("toggle_sidebar", "ctrl+b"),
    TOGGLE_MENTIONS("toggle_mentions", "ctrl+m"),
    ADD_CHANNEL("add_channel", "ctrl+t"),
    OPEN_SETTINGS("open_settings", "ctrl+o"),
    NEXT_CHANNEL("next_channel", "ctrl+tab"),
    PREV_CHANNEL("prev_channel", "ctrl+shift+tab"),
    CLOSE_CHANNEL("close_channel", "ctrl+shift+w"),
    TOGGLE_WHISPERS("toggle_whispers", "ctrl+shift+m"),
}

fun defaultHotkeys(): Map<String, String> =
    HotkeyAction.entries.associate { it.id to it.defaultCombo }

fun Map<String, String>.comboFor(action: HotkeyAction): String =
    this[action.id] ?: action.defaultCombo
