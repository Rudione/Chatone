package io.rudione.chatone.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class ControlFeedbackSceneTest {
    private class Feedback : HapticFeedback {
        val effects = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { effects.add(hapticFeedbackType) }
    }

    private fun scene(feedback: HapticFeedback?, content: @Composable () -> Unit) = ImageComposeScene(
        width = 220, height = 100, density = Density(1f)
    ) {
        CompositionLocalProvider(LocalControlFeedback provides feedback, LocalControlMotion provides ControlMotion.Ios) {
            MaterialTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() } }
        }
    }

    private fun ImageComposeScene.tap() {
        sendPointerEvent(PointerEventType.Press, Offset(110f, 50f))
        sendPointerEvent(PointerEventType.Release, Offset(110f, 50f))
        render()
        render()
    }

    @Test fun aCommittedButtonTapHasOneFeedbackAndIdleFramesHaveNone() {
        val feedback = Feedback()
        var clicks = 0
        val scene = scene(feedback) { ChatoneButton("Action", { clicks++ }, Modifier.width(140.dp), height = 40.dp) }
        try {
            scene.render(); scene.render()
            scene.tap()
            repeat(60) { scene.render() }
            assertEquals(1, clicks)
            assertEquals(listOf(HapticFeedbackType.VirtualKey), feedback.effects)
        } finally { scene.close() }
    }

    @Test fun disabledOrCancelledButtonsDoNotProduceFeedback() {
        val feedback = Feedback()
        var clicks = 0
        val enabled = mutableStateOf(false)
        val scene = scene(feedback) { ChatoneButton("Action", { clicks++ }, Modifier.width(140.dp), enabled = enabled.value, height = 40.dp) }
        try {
            scene.render(); scene.render(); scene.tap()
            enabled.value = true
            scene.render(); scene.render()
            scene.sendPointerEvent(PointerEventType.Press, Offset(110f, 50f))
            scene.sendPointerEvent(PointerEventType.Move, Offset(215f, 95f))
            scene.sendPointerEvent(PointerEventType.Release, Offset(215f, 95f))
            scene.render()
            assertEquals(0, clicks)
            assertTrue(feedback.effects.isEmpty())
        } finally { scene.close() }
    }

    @Test fun switchesReportTheNewValueAndItsMatchingFeedback() {
        val feedback = Feedback()
        val checked = mutableStateOf(false)
        val scene = scene(feedback) { ChatoneSwitch(checked.value, { checked.value = it }) }
        try {
            scene.render(); scene.render(); scene.tap()
            assertTrue(checked.value)
            scene.tap()
            assertEquals(false, checked.value)
            assertEquals(listOf(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff), feedback.effects)
        } finally { scene.close() }
    }

    @Test fun theDefaultPlatformPathDoesNotAddHaptics() {
        var clicks = 0
        val scene = scene(null) { ChatoneButton("Action", { clicks++ }, Modifier.width(140.dp), height = 40.dp) }
        try {
            scene.render(); scene.render(); scene.tap()
            assertEquals(1, clicks)
        } finally { scene.close() }
    }
}
