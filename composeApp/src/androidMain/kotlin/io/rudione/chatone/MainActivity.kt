package io.rudione.chatone

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import io.rudione.chatone.util.media.AndroidFilePicker
import io.rudione.chatone.util.system.AndroidNotifier
import io.rudione.chatone.util.system.setAppForeground

class MainActivity : ComponentActivity() {

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            AndroidFilePicker.deliver(this, uri)
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AndroidFilePicker.attach { mimeTypes -> filePickerLauncher.launch(mimeTypes) }

        requestNotificationPermissionIfNeeded()

        setContent {
            App()
        }
    }

    override fun onResume() {
        super.onResume()
        setAppForeground(true)
    }

    override fun onPause() {
        setAppForeground(false)
        super.onPause()
    }

    override fun onDestroy() {
        AndroidFilePicker.detach()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        AndroidNotifier.ensureChannel(applicationContext)
        if (Build.VERSION.SDK_INT < 33) return
        if (AndroidNotifier.hasPermission(applicationContext)) return
        runCatching {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
