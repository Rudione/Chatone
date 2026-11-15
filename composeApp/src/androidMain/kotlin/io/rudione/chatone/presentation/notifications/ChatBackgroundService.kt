package io.rudione.chatone.presentation.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.repository.ChatRepository
import io.rudione.chatone.data.repository.NotificationPreferencesRepository
import io.rudione.chatone.util.system.AndroidNotifier
import org.koin.android.ext.android.inject

class ChatBackgroundService : Service() {

    private val chatRepository: ChatRepository by inject()
    private val preferences: NotificationPreferencesRepository by inject()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_TURN_OFF) {
            preferences.setMentionAlerts(false)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        chatRepository.reconnectIfFailed()
        return START_NOT_STICKY
    }

    private fun enterForeground(): Boolean = runCatching {
        val notification = AndroidNotifier.backgroundConnectionNotification(
            this, turnOffIntent()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }.onFailure {
        Napier.w(
            "Background connection could not start: ${it.message}", tag = TAG
        )
    }.isSuccess

    private fun turnOffIntent(): PendingIntent = PendingIntent.getService(
        this,
        0,
        Intent(this, ChatBackgroundService::class.java).setAction(ACTION_TURN_OFF),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        const val TAG = "ChatBackgroundService"
        const val NOTIFICATION_ID = 7301
        const val ACTION_TURN_OFF = "io.rudione.chatone.action.STOP_BACKGROUND_CONNECTION"
    }
}
