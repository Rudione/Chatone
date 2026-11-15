package io.rudione.chatone.presentation.components

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

internal enum class ControlMotion { Default, Ios, Reduced }

internal val LocalControlMotion = staticCompositionLocalOf { ControlMotion.Default }
internal val LocalControlFeedback = staticCompositionLocalOf<HapticFeedback?> { null }

@Composable
internal fun controlClick(
    onClick: () -> Unit,
    feedbackType: HapticFeedbackType = HapticFeedbackType.VirtualKey
): () -> Unit {
    val feedback = LocalControlFeedback.current ?: return onClick
    return remember(onClick, feedback, feedbackType) {
        {
            feedback.performHapticFeedback(feedbackType)
            onClick()
        }
    }
}

@Composable
internal fun Modifier.nativeControlPress(source: InteractionSource): Modifier =
    if (LocalControlMotion.current == ControlMotion.Default) this else tactilePress(source, pressedScale = 0.96f)

@Composable
internal fun controlToggle(onValueChange: (Boolean) -> Unit): (Boolean) -> Unit {
    val feedback = LocalControlFeedback.current ?: return onValueChange
    return remember(onValueChange, feedback) {
        { checked ->
            feedback.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onValueChange(checked)
        }
    }
}
