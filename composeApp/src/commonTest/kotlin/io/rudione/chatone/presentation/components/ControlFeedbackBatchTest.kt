package io.rudione.chatone.presentation.components

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ControlFeedbackBatchTest {
    @Test fun nestedControlAndExplicitActionProduceOneMeaningfulFeedback() {
        val batch = ControlFeedbackBatch()
        batch.offer(HapticFeedbackType.VirtualKey)
        batch.offer(HapticFeedbackType.Confirm)
        batch.offer(HapticFeedbackType.VirtualKey)
        assertEquals(HapticFeedbackType.Confirm, batch.take(0.0))
        assertNull(batch.take(0.0))
    }

    @Test fun rapidDuplicateTapsAreDroppedWithoutPlayingLate() {
        val batch = ControlFeedbackBatch()
        batch.offer(HapticFeedbackType.VirtualKey)
        assertEquals(HapticFeedbackType.VirtualKey, batch.take(0.0))
        batch.offer(HapticFeedbackType.VirtualKey)
        assertNull(batch.take(0.01))
        assertNull(batch.take(1.0))
        batch.offer(HapticFeedbackType.VirtualKey)
        assertEquals(HapticFeedbackType.VirtualKey, batch.take(1.0))
    }

    @Test fun anActionFailureIsNotLostBehindTheInitialTap() {
        val batch = ControlFeedbackBatch()
        batch.offer(HapticFeedbackType.VirtualKey)
        batch.take(0.0)
        batch.offer(HapticFeedbackType.Reject)
        assertEquals(HapticFeedbackType.Reject, batch.take(0.01))
        batch.offer(HapticFeedbackType.Confirm)
        assertNull(batch.take(0.02))
    }

    @Test fun disposalDropsQueuedFeedback() {
        val batch = ControlFeedbackBatch()
        batch.offer(HapticFeedbackType.ToggleOn)
        batch.clear()
        assertNull(batch.take(0.0))
    }
}
