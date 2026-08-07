package io.rudione.chatone.util.system

import android.app.Application
import android.content.Intent
import android.net.Uri
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform

actual fun openInStreamlink(channelLogin: String, quality: String) {
    val login = channelLogin.trim().lowercase()
    if (login.isEmpty() || !login.all { it.isLetterOrDigit() || it == '_' }) return
    try {
        val app = KoinPlatform.getKoin().get<Application>()
        val targets = listOf(
            "twitch://stream/$login",
            "https://www.twitch.tv/$login"
        )
        for (target in targets) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(app.packageManager) != null) {
                app.startActivity(intent)
                return
            }
        }
    } catch (e: Exception) {
        Napier.w("openInStreamlink failed: ${e.message}", tag = "Streamlink")
    }
}
