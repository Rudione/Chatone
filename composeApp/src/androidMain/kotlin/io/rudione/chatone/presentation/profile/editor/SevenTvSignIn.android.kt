package io.rudione.chatone.presentation.profile.editor

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_editor_seven_tv_connect
import org.jetbrains.compose.resources.stringResource
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val LOGIN_URL = "https://api.7tv.app/v4/auth/login?platform=twitch"
private const val SITE_HOST = "7tv.app"
private const val TOKEN_SCRIPT = "(function(){try{return localStorage.getItem('7tv-token')}catch(e){return null}})()"
private val ALLOWED_HOSTS = setOf("7tv.app", "api.7tv.app", "7tv.io", "id.twitch.tv", "www.twitch.tv", "twitch.tv", "passport.twitch.tv")
private const val TOKEN_POLL_MS = 900L

actual val sevenTvSignInSupported: Boolean = true

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun SevenTvSignInDialog(onToken: (String) -> Unit, onDismiss: () -> Unit) {
    val deliver by rememberUpdatedState(onToken)
    var web by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var onSite by remember { mutableStateOf(false) }
    var delivered by remember { mutableStateOf(false) }

    LaunchedEffect(web, onSite) {
        val view = web ?: return@LaunchedEffect
        while (onSite && !delivered) {
            view.evaluateJavascript(TOKEN_SCRIPT) { raw ->
                val token = runCatching { (Json.parseToJsonElement(raw) as? JsonPrimitive)?.contentOrNull }.getOrNull()
                if (!delivered && !token.isNullOrBlank()) {
                    delivered = true
                    deliver(token)
                }
            }
            delay(TOKEN_POLL_MS)
        }
    }
    DisposableEffect(Unit) {
        onDispose { web?.destroy() }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.profile_editor_seven_tv_connect),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    GlassIconButton(Lucide.X, stringResource(Res.string.profile_editor_seven_tv_connect), onDismiss)
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.userAgentString = settings.userAgentString.replace("; wv", "")
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        if (!request.isForMainFrame) return false
                                        val host = request.url.host?.lowercase() ?: return true
                                        return ALLOWED_HOSTS.none { host == it || host.endsWith(".$it") }
                                    }

                                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                        loading = true
                                    }

                                    override fun onPageFinished(view: WebView, url: String?) {
                                        loading = false
                                        onSite = Uri.parse(url.orEmpty()).host == SITE_HOST
                                    }
                                }
                                loadUrl(LOGIN_URL)
                                web = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
