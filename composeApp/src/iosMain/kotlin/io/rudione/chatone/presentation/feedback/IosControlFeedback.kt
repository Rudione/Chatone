package io.rudione.chatone.presentation.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import io.rudione.chatone.presentation.components.ControlFeedbackBatch
import io.rudione.chatone.presentation.components.ControlMotion
import io.rudione.chatone.presentation.components.LocalControlFeedback
import io.rudione.chatone.presentation.components.LocalControlMotion
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled
import platform.UIKit.UIAccessibilityReduceMotionStatusDidChangeNotification
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UISelectionFeedbackGenerator
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
internal fun IosControlFeedback(content: @Composable () -> Unit) {
    val native = LocalHapticFeedback.current
    val feedback = remember(native) { IosActionFeedback(native) }
    var reducedMotion by remember { mutableStateOf(UIAccessibilityIsReduceMotionEnabled()) }
    DisposableEffect(feedback) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            UIAccessibilityReduceMotionStatusDidChangeNotification, null, NSOperationQueue.mainQueue
        ) { reducedMotion = UIAccessibilityIsReduceMotionEnabled() }
        onDispose {
            center.removeObserver(observer)
            feedback.dispose()
        }
    }
    CompositionLocalProvider(
        LocalControlFeedback provides feedback,
        LocalHapticFeedback provides feedback,
        LocalControlMotion provides if (reducedMotion) ControlMotion.Reduced else ControlMotion.Ios,
        content = content
    )
}

private class IosActionFeedback(private val native: HapticFeedback) : HapticFeedback {
    private val impact by lazy { UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleSoft) }
    private val selection by lazy { UISelectionFeedbackGenerator() }
    private val batch = ControlFeedbackBatch()
    private var scheduled = false
    private var active = true

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        if (hapticFeedbackType == HapticFeedbackType.TextHandleMove || hapticFeedbackType == HapticFeedbackType.KeyboardTap) return
        dispatch_async(dispatch_get_main_queue()) {
            if (!active) return@dispatch_async
            batch.offer(hapticFeedbackType)
            if (!scheduled) {
                scheduled = true
                dispatch_async(dispatch_get_main_queue()) { flush() }
            }
        }
    }

    private fun flush() {
        scheduled = false
        if (!active) return
        val feedback = batch.take(CACurrentMediaTime()) ?: return
        when (feedback) {
            HapticFeedbackType.VirtualKey -> impact.impactOccurredWithIntensity(0.55)
            HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff -> selection.selectionChanged()
            else -> native.performHapticFeedback(feedback)
        }
    }

    fun dispose() {
        active = false
        batch.clear()
    }

}
