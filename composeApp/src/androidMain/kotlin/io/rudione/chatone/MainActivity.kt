package io.rudione.chatone

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.rudione.chatone.presentation.splash.playChatoneExit
import io.rudione.chatone.util.media.AndroidFilePicker
import io.rudione.chatone.util.system.AndroidNotifier
import io.rudione.chatone.util.system.LaunchGate
import io.rudione.chatone.util.system.NotificationAccess
import io.rudione.chatone.util.system.NotificationLaunches
import io.rudione.chatone.util.system.NotificationTarget
import io.rudione.chatone.util.system.setAppForeground
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val launchGate: LaunchGate by inject()

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            AndroidFilePicker.deliver(this, uri)
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            NotificationAccess.onPermissionResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { launchGate.isHolding }
        splashScreen.setOnExitAnimationListener { provider -> provider.playChatoneExit() }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        AndroidFilePicker.attach { mimeTypes -> filePickerLauncher.launch(mimeTypes) }
        NotificationAccess.attach {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        requestNotificationPermissionIfNeeded()
        if (savedInstanceState == null) openNotificationTarget(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openNotificationTarget(intent)
    }

    override fun onResume() {
        super.onResume()
        NotificationAccess.refresh()
        setAppForeground(true)
    }

    override fun onPause() {
        setAppForeground(false)
        super.onPause()
    }

    override fun onDestroy() {
        AndroidFilePicker.detach()
        NotificationAccess.detach()
        super.onDestroy()
    }

    private fun openNotificationTarget(intent: Intent?) {
        if (intent?.action != AndroidNotifier.ACTION_OPEN_TARGET) return
        NotificationTarget.of(
            intent.getStringExtra(AndroidNotifier.EXTRA_CHANNEL),
            intent.getStringExtra(AndroidNotifier.EXTRA_MESSAGE_ID)
        )?.let(NotificationLaunches::publish)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        if (AndroidNotifier.hasPermission(applicationContext)) return
        runCatching {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
