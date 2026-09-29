package io.rudione.chatone.presentation.notifications

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.repository.AuthRepository
import io.rudione.chatone.data.repository.NotificationPreferencesRepository
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import io.rudione.chatone.util.system.NotificationAccess
import io.rudione.chatone.util.system.appForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChatBackgroundController(
    private val context: Context,
    private val preferences: NotificationPreferencesRepository,
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope
) {
    private data class Demand(val wanted: Boolean, val foreground: Boolean)

    fun start() {
        if (currentFormFactor() != DeviceFormFactor.PHONE) return
        scope.launch(Dispatchers.Main) {
            val hasAccount = flow { emitAll(authRepository.getAccounts()) }.map { it.isNotEmpty() }
            combine(
                preferences.mentionAlerts,
                hasAccount,
                NotificationAccess.granted,
                appForeground
            ) { enabled, signedIn, permitted, foreground ->
                Demand(wanted = enabled && signedIn && permitted, foreground = foreground)
            }.distinctUntilChanged().collect(::apply)
        }
    }

    private fun apply(demand: Demand) {
        val intent = Intent(context, ChatBackgroundService::class.java)
        when {
            !demand.wanted -> context.stopService(intent)
            demand.foreground -> runCatching {
                ContextCompat.startForegroundService(
                    context,
                    intent
                )
            }
                .onFailure {
                    Napier.w(
                        "Background connection start refused: ${it.message}",
                        tag = TAG
                    )
                }
        }
    }

    private companion object {
        const val TAG = "ChatBackgroundCtl"
    }
}
