package io.rudione.chatone.presentation.stream

import kotlin.test.Test
import kotlin.test.assertEquals

class PictureInPictureExitTrackerTest {

    private var dismissals = 0
    private val tracker = PictureInPictureExitTracker { dismissals++ }

    @Test
    fun closingAfterStopCountsAsDismissal() {
        tracker.onModeChanged(inPictureInPicture = true, started = true)
        tracker.onStopped()
        tracker.onModeChanged(inPictureInPicture = false, started = false)
        assertEquals(1, dismissals)
    }

    @Test
    fun closingBeforeStopCountsAsDismissal() {
        tracker.onModeChanged(inPictureInPicture = true, started = true)
        tracker.onModeChanged(inPictureInPicture = false, started = true)
        tracker.onStopped()
        assertEquals(1, dismissals)
    }

    @Test
    fun expandingBackToTheAppIsNotADismissal() {
        tracker.onModeChanged(inPictureInPicture = true, started = true)
        tracker.onModeChanged(inPictureInPicture = false, started = true)
        tracker.onResumed()
        tracker.onStopped()
        assertEquals(0, dismissals)
    }

    @Test
    fun lockingTheScreenWhileInPictureInPictureKeepsPlaying() {
        tracker.onModeChanged(inPictureInPicture = true, started = true)
        tracker.onStopped()
        assertEquals(0, dismissals)
    }

    @Test
    fun aRegularStopOutsidePictureInPictureIsIgnored() {
        tracker.onStopped()
        tracker.onResumed()
        tracker.onStopped()
        assertEquals(0, dismissals)
    }
}
