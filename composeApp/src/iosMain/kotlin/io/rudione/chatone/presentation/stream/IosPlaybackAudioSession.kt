package io.rudione.chatone.presentation.stream

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeMoviePlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
internal class IosPlaybackAudioSession {
    private var holders = 0

    fun activate() {
        if (holders++ > 0) return
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeMoviePlayback, options = 0u, error = null)
        session.setActive(true, error = null)
    }

    fun deactivate() {
        if (holders == 0) return
        if (--holders > 0) return
        AVAudioSession.sharedInstance().setActive(false, withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation, error = null)
    }
}
