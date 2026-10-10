package io.rudione.chatone.util.platform

import android.content.Context
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberInputCapabilities(): InputCapabilities {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var pointer by remember { mutableStateOf(hasPointerDevice(context)) }
    DisposableEffect(context) {
        val manager = context.getSystemService(InputManager::class.java)
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                pointer = hasPointerDevice(context)
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                pointer = hasPointerDevice(context)
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                pointer = hasPointerDevice(context)
            }
        }
        manager?.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager?.unregisterInputDeviceListener(listener) }
    }
    val keyboard = configuration.keyboard == Configuration.KEYBOARD_QWERTY &&
        configuration.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES
    return remember(keyboard, pointer) { InputCapabilities(hardwareKeyboard = keyboard, pointer = pointer) }
}

private fun hasPointerDevice(context: Context): Boolean {
    val manager = context.getSystemService(InputManager::class.java) ?: return false
    return manager.inputDeviceIds.any { id ->
        val device = manager.getInputDevice(id) ?: return@any false
        !device.isVirtual && (device.supportsSource(InputDevice.SOURCE_MOUSE) || device.supportsSource(InputDevice.SOURCE_TOUCHPAD))
    }
}
