package io.rudione.chatone.util

import android.app.Application
import android.content.Intent
import android.net.Uri
import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.settings.SettingsState
import org.koin.mp.KoinPlatform

actual fun openUrl(url: String, mode: SettingsState.LinkOpenMode) {
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
