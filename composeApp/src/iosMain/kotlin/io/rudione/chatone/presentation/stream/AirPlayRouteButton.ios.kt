package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVKit.AVRoutePickerView
import platform.AVKit.AVRoutePickerViewDelegateProtocol
import platform.UIKit.UIColor
import platform.UIKit.accessibilityLabel
import platform.darwin.NSObject
import kotlinx.cinterop.cValue

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AirPlayRouteButton(modifier: Modifier, onVisibilityChanged: (Boolean) -> Unit) {
    val visibility = rememberUpdatedState(onVisibilityChanged)
    UIKitView(
        factory = { ChatoneRoutePicker { visibility.value(it) } },
        modifier = modifier,
        onRelease = { it.dispose() }
    )
}

@OptIn(ExperimentalForeignApi::class)
private class ChatoneRoutePicker : AVRoutePickerView {
    private val events: RouteEvents
    constructor(onVisibilityChanged: (Boolean) -> Unit) : super(frame = cValue { }) {
        events = RouteEvents(onVisibilityChanged)
        prioritizesVideoDevices = true
        tintColor = UIColor.whiteColor
        activeTintColor = UIColor.colorWithRed(0.2, 0.85, 0.7, 1.0)
        accessibilityLabel = "AirPlay"
        delegate = events
    }
    fun dispose() { delegate = null; events.dispose() }
}

private class RouteEvents(private val onVisibilityChanged: (Boolean) -> Unit) : NSObject(), AVRoutePickerViewDelegateProtocol {
    private var presenting = false
    override fun routePickerViewWillBeginPresentingRoutes(routePickerView: AVRoutePickerView) {
        if (presenting) return
        presenting = true
        onVisibilityChanged(true)
        IosAirPlayRouting.prepare()
    }
    override fun routePickerViewDidEndPresentingRoutes(routePickerView: AVRoutePickerView) { dispose() }
    fun dispose() {
        if (!presenting) return
        presenting = false
        onVisibilityChanged(false)
        IosAirPlayRouting.finishPicking()
    }
}
