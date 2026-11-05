package io.rudione.chatone.tv.player

import androidx.compose.ui.input.key.Key

internal enum class TvPlayerPanel { NONE, CONTROLS, QUALITY, CHANNELS, CHAT, CONFIRM }

internal enum class TvPlayerCommand {
    SHOW_CONTROLS,
    TOGGLE_PLAYBACK,
    OPEN_CHANNELS,
    FOCUS_CHAT,
    NEXT_CHANNEL,
    PREVIOUS_CHANNEL
}

internal fun mediaCommand(key: Key): TvPlayerCommand? = when (key) {
    Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> TvPlayerCommand.TOGGLE_PLAYBACK
    Key.ChannelUp -> TvPlayerCommand.PREVIOUS_CHANNEL
    Key.ChannelDown -> TvPlayerCommand.NEXT_CHANNEL
    else -> null
}

internal fun idleCommand(key: Key): TvPlayerCommand? = when (key) {
    Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionUp, Key.DirectionDown -> TvPlayerCommand.SHOW_CONTROLS
    Key.DirectionLeft -> TvPlayerCommand.OPEN_CHANNELS
    Key.DirectionRight -> TvPlayerCommand.FOCUS_CHAT
    else -> mediaCommand(key)
}

internal fun <T> List<T>.neighbour(index: Int, step: Int): T? {
    if (isEmpty()) return null
    val base = if (index in indices) index else 0
    return this[(base + step).mod(size)]
}
