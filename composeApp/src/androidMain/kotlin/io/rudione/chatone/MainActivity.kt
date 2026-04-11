package io.rudione.chatone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import io.rudione.chatone.auth.AuthTokenBridge
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
        val fragment = uri.fragment ?: return
        val params = fragment.split("&").associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }
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
