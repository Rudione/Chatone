package io.rudione.chatone.presentation.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
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
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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
        watchNetwork()
        chatRepository.reconnectIfFailed()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        networkCallback?.let { callback ->
            runCatching {
                getSystemService(
                    ConnectivityManager::class.java
                )?.unregisterNetworkCallback(
                    callback
                )
            }
        }
        networkCallback = null
        super.onDestroy()
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

    private fun watchNetwork() {
        if (networkCallback != null) return
        val manager = getSystemService(ConnectivityManager::class.java) ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                chatRepository.reconnectIfFailed()
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }.onSuccess {
            networkCallback = callback
        }.onFailure { Napier.w("Network callback unavailable: ${it.message}", tag = TAG) }
    }

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
