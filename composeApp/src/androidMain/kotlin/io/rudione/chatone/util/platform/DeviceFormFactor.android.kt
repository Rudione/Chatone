package io.rudione.chatone.util.platform

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import org.koin.core.context.GlobalContext

private const val FEATURE_XR_IMMERSIVE = "android.software.xr.immersive"
private const val FEATURE_VR_HEADTRACKING = "android.hardware.vr.headtracking"

private val resolved: DeviceFormFactor by lazy { detect() }

actual fun currentFormFactor(): DeviceFormFactor = resolved

private fun detect(): DeviceFormFactor {
    val context = runCatching {
        GlobalContext.getOrNull()?.get<Context>()
    }.getOrNull() ?: return DeviceFormFactor.PHONE

    val pm = context.packageManager
    if (pm.hasSystemFeature(PackageManager.FEATURE_WATCH)) return DeviceFormFactor.WATCH
    if (pm.hasSystemFeature(FEATURE_XR_IMMERSIVE) || pm.hasSystemFeature(FEATURE_VR_HEADTRACKING)) {
        return DeviceFormFactor.XR
    }

    val uiMode = (context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager)?.currentModeType
    if (uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
        pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    ) {
        return DeviceFormFactor.TV
    }

    val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp
    return if (smallestWidthDp >= 600) DeviceFormFactor.TABLET else DeviceFormFactor.PHONE
}
