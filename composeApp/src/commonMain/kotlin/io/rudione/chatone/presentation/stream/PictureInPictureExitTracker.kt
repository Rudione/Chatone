package io.rudione.chatone.presentation.stream

class PictureInPictureExitTracker(private val onDismissed: () -> Unit) {

    private var leavingWhileStarted = false

    fun onModeChanged(inPictureInPicture: Boolean, started: Boolean) {
        if (inPictureInPicture) {
            leavingWhileStarted = false
            return
        }
        if (started) leavingWhileStarted = true else onDismissed()
    }

    fun onResumed() {
        leavingWhileStarted = false
    }

    fun onStopped() {
        if (!leavingWhileStarted) return
        leavingWhileStarted = false
        onDismissed()
    }
}
