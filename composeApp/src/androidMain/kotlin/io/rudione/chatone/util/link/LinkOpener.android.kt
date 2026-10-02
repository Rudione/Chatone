package io.rudione.chatone.util.link

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.settings.SettingsState
import org.koin.mp.KoinPlatform

actual fun openUrl(url: String, mode: SettingsState.LinkOpenMode) {
    if (!isSafeHttpUrl(url)) return
    try {
        val app = KoinPlatform.getKoin().get<Application>()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    } catch (e: Exception) {
        Napier.e("openUrl failed: ${e.message}", e, tag = "LinkOpener")
    }
}

actual fun openAuthTab(url: String) {
    if (!isSafeHttpUrl(url)) return
    try {
        val app = KoinPlatform.getKoin().get<Application>()
        val tab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()
        tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        CustomTabsClient.getPackageName(app, null)?.let(tab.intent::setPackage)
        tab.launchUrl(app, Uri.parse(url))
    } catch (e: Exception) {
        Napier.e("openAuthTab failed: ${e.message}", e, tag = "LinkOpener")
        openUrl(url, SettingsState.LinkOpenMode.DEFAULT)
    }
}
