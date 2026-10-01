package com.amiawake.android.telemetry

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.CancellationException
import com.amiawake.android.AmIAwakeApplication

class EventSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AmIAwakeApplication
        if (app.container.sessionStore.current() == null) return Result.success()
        return runCatching { app.container.repository.syncEvents() }
            .fold(onSuccess = { Result.success() }, onFailure = { if (it is CancellationException) throw it else Result.retry() })
    }
    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "event-sync",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<EventSyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build(),
            )
        }
    }
}
