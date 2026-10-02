package com.amiawake.android.telemetry

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amiawake.android.AmIAwakeApplication
import com.amiawake.android.data.DeviceEventType
import kotlinx.coroutines.CancellationException

class HeartbeatWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AmIAwakeApplication
        if (app.container.sessionStore.current() == null) return Result.success()
        return runCatching {
            app.container.repository.observeChargingState(applicationContext)
            app.container.repository.queueEvent(DeviceEventType.HEARTBEAT)
            app.container.repository.syncEvents()
        }.fold(
            onSuccess = { Result.success() },
            // The heartbeat is already durable in EventQueue. A later worker or
            // telemetry event will retry the batch without creating duplicates.
            onFailure = {
                if (it is CancellationException) throw it
                EventSyncWorker.enqueue(applicationContext)
                Result.success()
            },
        )
    }

    companion object { const val WORK_NAME = "periodic-heartbeat" }
}
