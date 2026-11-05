package io.rudione.chatone.presentation.profile.editor

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
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.profile_editor_seven_tv_connect
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.X
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.darwin.NSObject

private const val LOGIN_URL = "https://api.7tv.app/v4/auth/login?platform=twitch"
private const val SITE_HOST = "7tv.app"
private const val TOKEN_SCRIPT = "(function(){try{return localStorage.getItem('7tv-token')}catch(e){return null}})()"
private val ALLOWED_HOSTS = setOf("7tv.app", "api.7tv.app", "7tv.io", "id.twitch.tv", "www.twitch.tv", "twitch.tv", "passport.twitch.tv")
private const val TOKEN_POLL_MS = 900L

actual val sevenTvSignInSupported: Boolean = true

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun SevenTvSignInDialog(onToken: (String) -> Unit, onDismiss: () -> Unit) {
    val deliver by rememberUpdatedState(onToken)
    var loading by remember { mutableStateOf(true) }
    var onSite by remember { mutableStateOf(false) }
    var delivered by remember { mutableStateOf(false) }
    val navigation = remember {
        SignInNavigation(
            onLoading = { loading = it },
            onPage = { host -> onSite = host == SITE_HOST }
        )
    }
    val web = remember {
        val configuration = WKWebViewConfiguration().apply {
            websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        }
        WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).apply {
            navigationDelegate = navigation
            NSURL.URLWithString(LOGIN_URL)?.let { loadRequest(NSURLRequest.requestWithURL(it)) }
        }
    }

    LaunchedEffect(web, onSite) {
        while (onSite && !delivered) {
            web.evaluateJavaScript(TOKEN_SCRIPT) { result, _ ->
                val token = (result as? String)?.takeIf { it.isNotBlank() }
                if (!delivered && token != null) {
                    delivered = true
                    deliver(token)
                }
            }
            delay(TOKEN_POLL_MS)
        }
    }
    DisposableEffect(web) {
        onDispose {
            web.stopLoading()
            web.navigationDelegate = null
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    UIKitView(factory = { web }, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

private class SignInNavigation(
    private val onLoading: (Boolean) -> Unit,
    private val onPage: (String?) -> Unit
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        val mainFrame = decidePolicyForNavigationAction.targetFrame?.isMainFrame() ?: true
        val host = decidePolicyForNavigationAction.request.URL?.host?.lowercase()
        val allowed = !mainFrame || (host != null && ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") })
        decisionHandler(
            if (allowed) WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            else WKNavigationActionPolicy.WKNavigationActionPolicyCancel
        )
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        onLoading(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        onLoading(false)
        onPage(webView.URL?.host)
    }
}
