package io.rudione.chatone

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.rudione.chatone.data.auth.AuthTokenBridge
import io.rudione.chatone.util.link.isSafeHttpUrl
import io.rudione.chatone.util.media.AndroidFilePicker
import io.rudione.chatone.util.system.AndroidNotifier
import io.rudione.chatone.util.system.setAppForeground
import kotlinx.coroutines.launch

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

        lifecycleScope.launch {
            AuthTokenBridge.tokenFlow.collect { token ->
                if (token.startsWith("__open_url__:")) {
                    val url = token.removePrefix("__open_url__:")
                    if (!isSafeHttpUrl(url)) {
                        io.github.aakira.napier.Napier.e(
                            "Refusing to open non-http auth URL",
                            tag = "MainActivity"
                        )
                        return@collect
                    }
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        io.github.aakira.napier.Napier.e(
                            "Failed to open auth URL: ${e.message}",
                            tag = "MainActivity"
                        )
                    }
                }
            }
        }

        handleOAuthCallback(intent)
        requestNotificationPermissionIfNeeded()

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthCallback(intent)
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

    private fun handleOAuthCallback(intent: Intent) {
        val uri = intent.data ?: return
        val fragment = uri.fragment?.takeIf { it.isNotBlank() } ?: uri.query ?: return
        val params = fragment.split("&").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx) to part.substring(idx + 1)
        }.toMap()
        val accessToken = params["access_token"] ?: return
        OAuthTokenHolder.onTokenReceived(accessToken, params["state"].orEmpty())
    }
}

object OAuthTokenHolder {
    private val lock = Any()
    private var callback: ((String, String) -> Boolean)? = null
    private var pendingToken: String? = null
    private var pendingState: String = ""

    fun setCallback(cb: (String, String) -> Boolean) {
        val replay = synchronized(lock) {
            callback = cb
            val token = pendingToken
            val state = pendingState
            pendingToken = null
            pendingState = ""
            token?.let { it to state }
        }
        if (replay != null) deliver(cb, replay.first, replay.second)
    }

    fun reset() {
        synchronized(lock) {
            callback = null
            pendingToken = null
            pendingState = ""
        }
    }

    fun onTokenReceived(token: String, state: String = "") {
        val current = synchronized(lock) {
            val cb = callback
            if (cb == null) {
                pendingToken = token
                pendingState = state
            }
            cb
        }
        if (current == null) return
        deliver(current, token, state)
    }

    private fun deliver(cb: (String, String) -> Boolean, token: String, state: String) {
        if (cb(token, state)) {
            synchronized(lock) { callback = null }
        }
    }
}
