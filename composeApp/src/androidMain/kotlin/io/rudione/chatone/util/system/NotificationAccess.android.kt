package io.rudione.chatone.util.system

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.mp.KoinPlatform

actual object NotificationAccess {

    private val _granted = MutableStateFlow(true)
    actual val granted: StateFlow<Boolean> = _granted.asStateFlow()

    private var requester: (() -> Unit)? = null
    private var openSettingsIfDenied = false

    fun attach(requestPermission: () -> Unit) {
        requester = requestPermission
        refresh()
    }

    fun detach() {
        requester = null
    }

    fun onPermissionResult(granted: Boolean) {
        refresh()
        if (!granted && openSettingsIfDenied) openSystemSettings()
        openSettingsIfDenied = false
    }

    actual fun refresh() {
        _granted.value = application()?.let(AndroidNotifier::canNotify) ?: true
    }

    actual fun request() {
        refresh()
        if (_granted.value) return
        val launch = requester
        val app = application()
        if (launch != null && app != null && !AndroidNotifier.hasPermission(app)) {
            openSettingsIfDenied = true
            launch()
        } else {
            openSystemSettings()
        }
    }

    actual fun openSystemSettings() {
        val app = application() ?: return
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, app.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null))
        }
        runCatching { app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { Napier.w("Notification settings unavailable: ${it.message}", tag = "NotificationAccess") }
    }

    private fun application(): Application? = runCatching { KoinPlatform.getKoin().get<Application>() }.getOrNull()
}
