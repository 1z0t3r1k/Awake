package com.amiawake.android.telemetry

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amiawake.android.AmIAwakeApplication

class EventSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AmIAwakeApplication
        if (app.container.sessionStore.current() == null) return Result.success()
        return runCatching { app.container.repository.syncEvents() }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}
