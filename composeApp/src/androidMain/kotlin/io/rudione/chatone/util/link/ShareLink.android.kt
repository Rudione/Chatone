package io.rudione.chatone.util.link

import android.app.Application
import android.content.Intent
import io.github.aakira.napier.Napier
import org.koin.mp.KoinPlatform

actual fun shareLink(url: String, title: String): Boolean {
    if (!isSafeHttpUrl(url)) return false
    return try {
        val app = KoinPlatform.getKoin().get<Application>()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, url)
        }
        val chooser = Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(chooser)
        true
    } catch (e: Exception) {
        Napier.w("shareLink failed: ${e.message}", tag = "ShareLink")
        false
    }
}
