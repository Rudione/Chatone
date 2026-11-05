package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.stream.StreamMediaInfo
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyIsLiveStream
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

internal class IosRemoteControls(onPlay: () -> Unit, onPause: () -> Unit) {
    private val center = MPRemoteCommandCenter.sharedCommandCenter()
    private val playTarget = center.playCommand.addTargetWithHandler { _ ->
        onPlay()
        MPRemoteCommandHandlerStatusSuccess
    }
    private val pauseTarget = center.pauseCommand.addTargetWithHandler { _ ->
        onPause()
        MPRemoteCommandHandlerStatusSuccess
    }

    fun show(info: StreamMediaInfo) {
        val channel = info.channelName.ifBlank { info.channelLogin }
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = mapOf<Any?, Any?>(
            MPMediaItemPropertyTitle to info.title.ifBlank { channel },
            MPMediaItemPropertyArtist to channel,
            MPNowPlayingInfoPropertyIsLiveStream to true
        )
    }

    fun clear() {
        center.playCommand.removeTarget(playTarget)
        center.pauseCommand.removeTarget(pauseTarget)
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }
}
