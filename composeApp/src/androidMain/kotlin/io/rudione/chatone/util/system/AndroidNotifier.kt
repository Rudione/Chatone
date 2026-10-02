package io.rudione.chatone.util.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.settings.SettingsViewModel
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.shared.R
import kotlin.random.Random

object AndroidNotifier {

    private const val TAG = "AndroidNotifier"
    private const val CHANNEL_GENERAL = "chatone_alerts"
    private const val CHANNEL_MENTIONS = "chatone_mentions"
    private const val CHANNEL_LIVE = "chatone_live"
    private const val CHANNEL_BACKGROUND = "chatone_background"
    private const val TAG_MENTION = "mention:"
    private const val TAG_LIVE = "live:"
    private const val LIVE_NOTIFICATION_ID = 1

    const val ACTION_OPEN_TARGET = "io.rudione.chatone.action.OPEN_CHANNEL"
    const val EXTRA_CHANNEL = "io.rudione.chatone.extra.CHANNEL"
    const val EXTRA_MESSAGE_ID = "io.rudione.chatone.extra.MESSAGE_ID"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val strings = strings()
            manager.createNotificationChannels(
                listOf(
                    NotificationChannel(CHANNEL_GENERAL, "Chatone", NotificationManager.IMPORTANCE_DEFAULT),
                    NotificationChannel(
                        CHANNEL_MENTIONS,
                        strings.notificationChannelMentions,
                        NotificationManager.IMPORTANCE_HIGH
                    ),
                    NotificationChannel(
                        CHANNEL_LIVE,
                        strings.notificationChannelLive,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ),
                    NotificationChannel(
                        CHANNEL_BACKGROUND,
                        strings.notificationChannelBackground,
                        NotificationManager.IMPORTANCE_MIN
                    ).apply { setShowBadge(false) }
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

    fun canNotify(context: Context): Boolean =
        hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notify(
        context: Context,
        title: String,
        body: String,
        topic: NotificationTopic,
        target: NotificationTarget?
    ) {
        if (!canNotify(context)) return
        ensureChannels(context)
        runCatching {
            val builder = NotificationCompat.Builder(context, channelFor(topic))
                .setSmallIcon(R.drawable.chatbubbles)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(
                    if (topic == NotificationTopic.MENTION) NotificationCompat.PRIORITY_HIGH
                    else NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)
                .setContentIntent(contentIntent(context, target))
            when (topic) {
                NotificationTopic.MENTION -> builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                NotificationTopic.LIVE -> builder.setCategory(NotificationCompat.CATEGORY_EVENT)
                NotificationTopic.GENERAL -> Unit
            }
            val manager = NotificationManagerCompat.from(context)
            val notification = builder.build()
            when {
                topic == NotificationTopic.MENTION && target != null ->
                    manager.notify(TAG_MENTION + target.channelLogin, target.hashCode(), notification)
                topic == NotificationTopic.LIVE && target != null ->
                    manager.notify(TAG_LIVE + target.channelLogin, LIVE_NOTIFICATION_ID, notification)
                else -> manager.notify(Random.nextInt(), notification)
            }
        }.onFailure { Napier.w("Notification failed: ${it.message}", tag = TAG) }
    }

    fun dismiss(context: Context, channelLogin: String) {
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val tags = setOf(TAG_MENTION + channelLogin, TAG_LIVE + channelLogin)
            manager.activeNotifications
                .filter { it.tag in tags }
                .forEach { manager.cancel(it.tag, it.id) }
        }.onFailure { Napier.w("Notification dismiss failed: ${it.message}", tag = TAG) }
    }

    fun backgroundConnectionNotification(context: Context, stopIntent: PendingIntent): Notification {
        ensureChannels(context)
        val strings = strings()
        return NotificationCompat.Builder(context, CHANNEL_BACKGROUND)
            .setSmallIcon(R.drawable.chatbubbles)
            .setContentTitle(strings.backgroundConnectionTitle)
            .setContentText(strings.backgroundConnectionText)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setContentIntent(contentIntent(context, null))
            .addAction(0, strings.backgroundConnectionStop, stopIntent)
            .build()
    }

    private fun channelFor(topic: NotificationTopic): String = when (topic) {
        NotificationTopic.GENERAL -> CHANNEL_GENERAL
        NotificationTopic.MENTION -> CHANNEL_MENTIONS
        NotificationTopic.LIVE -> CHANNEL_LIVE
    }

    private fun contentIntent(context: Context, target: NotificationTarget?): PendingIntent {
        val intent = launcherIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (target != null) {
                action = ACTION_OPEN_TARGET
                putExtra(EXTRA_CHANNEL, target.channelLogin)
                target.messageId?.let { putExtra(EXTRA_MESSAGE_ID, it) }
            }
        }
        return PendingIntent.getActivity(
            context,
            target?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun launcherIntent(context: Context): Intent {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val component = (packageManager.getLaunchIntentForPackage(packageName)
            ?: packageManager.getLeanbackLaunchIntentForPackage(packageName))?.component
        return if (component != null) Intent().setComponent(component)
        else Intent(Intent.ACTION_MAIN).setPackage(packageName)
    }

    private fun strings(): AppStrings = AppStrings.forLocale(SettingsViewModel.currentLanguage())
}
