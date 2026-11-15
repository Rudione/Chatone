package io.rudione.chatone.util.system

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

@Composable
actual fun rememberStoreUpdateAvailable(): Boolean {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val manager = remember(context) { AppUpdateManagerFactory.create(context) }
    var available by remember(context) { mutableStateOf(false) }
    DisposableEffect(manager, lifecycle) {
        var active = true
        var checking = false
        fun check() {
            if (checking) return
            checking = true
            manager.appUpdateInfo.addOnSuccessListener { info ->
                if (active) {
                    val installed = context.packageManager.getPackageInfo(context.packageName, 0)
                    val versionCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
                    available = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.availableVersionCode().toLong() > versionCode
                }
            }.addOnFailureListener { if (active) available = false }
                .addOnCompleteListener { checking = false }
        }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) check() }
        lifecycle.addObserver(observer)
        check()
        onDispose { active = false; lifecycle.removeObserver(observer) }
    }
    return available
}
