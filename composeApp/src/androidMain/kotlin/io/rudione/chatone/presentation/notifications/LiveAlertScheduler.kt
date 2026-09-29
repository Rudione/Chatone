package io.rudione.chatone.presentation.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.aakira.napier.Napier
import io.rudione.chatone.data.repository.LiveAlertsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LiveAlertScheduler(
    private val context: Context,
    private val repository: LiveAlertsRepository,
    private val scope: CoroutineScope
) {
    fun start() {
        scope.launch {
            repository.channels
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect(::schedule)
        }
    }

    private fun schedule(active: Boolean) {
        runCatching {
            val workManager = WorkManager.getInstance(context)
            if (active) {
                workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request())
            } else {
                workManager.cancelUniqueWork(WORK_NAME)
            }
        }.onFailure { Napier.w("Live alert scheduling failed: ${it.message}", tag = TAG) }
    }

    private fun request() = PeriodicWorkRequestBuilder<LiveAlertWorker>(CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES)
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()

    private companion object {
        const val TAG = "LiveAlertScheduler"
        const val WORK_NAME = "live-alerts"
        const val CHECK_INTERVAL_MINUTES = 15L
    }
}
