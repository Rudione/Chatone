package io.rudione.chatone.util.platform

import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad

actual fun currentFormFactor(): DeviceFormFactor =
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        DeviceFormFactor.TABLET
    } else {
        DeviceFormFactor.PHONE
    }
