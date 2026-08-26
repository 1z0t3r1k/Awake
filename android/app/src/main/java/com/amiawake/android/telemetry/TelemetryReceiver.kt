package com.amiawake.android.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.amiawake.android.AmIAwakeApplication
import com.amiawake.android.data.DeviceEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TelemetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = when (intent.action) {
            Intent.ACTION_SCREEN_ON -> DeviceEventType.SCREEN_ON
            Intent.ACTION_SCREEN_OFF -> DeviceEventType.SCREEN_OFF
            Intent.ACTION_USER_PRESENT -> DeviceEventType.PHONE_UNLOCKED
            Intent.ACTION_POWER_CONNECTED -> DeviceEventType.CHARGING_STARTED
            Intent.ACTION_POWER_DISCONNECTED -> DeviceEventType.CHARGING_STOPPED
            else -> return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AmIAwakeApplication
                if (app.container.sessionStore.current() != null) {
                    app.container.repository.queueEvent(type)
                    enqueueSync(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun enqueueSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<EventSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
