package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.stream.StreamMediaInfo

object IosNowPlayingBridge {
    var onMetadata: ((String, String, String?) -> Unit)? = null
    var onPlayback: ((Boolean) -> Unit)? = null
    var onClear: (() -> Unit)? = null
    internal var owner: IosRemoteControls? = null

    fun play() { owner?.play() }
    fun pause() { owner?.pause() }
    fun toggle() { owner?.toggle() }
}

internal class IosRemoteControls(private val onPlay: () -> Unit, private val onPause: () -> Unit) {
    private var playing = true

    fun show(info: StreamMediaInfo) {
        IosNowPlayingBridge.owner = this
        val channel = info.channelName.ifBlank { info.channelLogin }
        IosNowPlayingBridge.onMetadata?.invoke(info.title.ifBlank { channel }, channel, info.artworkUrl)
        IosNowPlayingBridge.onPlayback?.invoke(playing)
    }

    fun setPlaying(playing: Boolean) {
        this.playing = playing
        if (IosNowPlayingBridge.owner === this) IosNowPlayingBridge.onPlayback?.invoke(playing)
    }

    fun play() = onPlay()
    fun pause() = onPause()
    fun toggle() { if (playing) onPause() else onPlay() }

    fun clear() {
        if (IosNowPlayingBridge.owner !== this) return
        IosNowPlayingBridge.owner = null
        IosNowPlayingBridge.onClear?.invoke()
    }
}
