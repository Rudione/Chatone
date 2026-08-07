package io.rudione.chatone.util.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.aakira.napier.Napier
import io.rudione.chatone.R
import kotlin.random.Random

object AndroidNotifier {

    private const val TAG = "AndroidNotifier"
    private const val CHANNEL_ID = "chatone_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Chatone",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }.onFailure { Napier.w("Notification channel setup failed: ${it.message}", tag = TAG) }
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun notify(context: Context, title: String, body: String) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        runCatching {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = launch?.let {
                PendingIntent.getActivity(context, 0, it, flags)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.chatbubbles)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .apply { if (pending != null) setContentIntent(pending) }
                .build()
            NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
        }.onFailure { Napier.w("Notification failed: ${it.message}", tag = TAG) }
    }
}
