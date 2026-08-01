package io.rudione.chatone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.rudione.chatone.data.auth.AuthTokenBridge
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            AuthTokenBridge.tokenFlow.collect { token ->
                if (token.startsWith("__open_url__:")) {
                    val url = token.removePrefix("__open_url__:")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    } catch (e: Exception) {
                        io.github.aakira.napier.Napier.e("Failed to open auth URL: ${e.message}", tag = "MainActivity")
                    }
                }
            }
        }

        handleOAuthCallback(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent) {
        val uri = intent.data ?: return
        val fragment = uri.fragment?.takeIf { it.isNotBlank() } ?: uri.query ?: return
        val params = fragment.split("&").mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx <= 0) null else part.substring(0, idx) to part.substring(idx + 1)
        }.toMap()
        val accessToken = params["access_token"] ?: return
        OAuthTokenHolder.onTokenReceived(accessToken)
    }
}

object OAuthTokenHolder {
    private var callback: ((String) -> Unit)? = null

    fun setCallback(cb: (String) -> Unit) {
        callback = cb
    }

    fun onTokenReceived(token: String) {
        callback?.invoke(token)
        callback = null
    }
}
