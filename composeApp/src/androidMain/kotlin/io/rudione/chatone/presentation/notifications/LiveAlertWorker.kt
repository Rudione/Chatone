package io.rudione.chatone.presentation.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LiveAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val notifier: LiveAlertNotifier by inject()

    override suspend fun doWork(): Result = if (notifier.checkNow()) Result.success() else Result.retry()
}
