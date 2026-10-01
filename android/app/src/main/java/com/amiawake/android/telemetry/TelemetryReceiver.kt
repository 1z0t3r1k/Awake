package com.amiawake.android.telemetry

import android.content.BroadcastReceiver
import android.app.KeyguardManager
import android.os.PowerManager
import android.content.Context
import android.content.Intent
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
                    val repository = app.container.repository
                    val queued = repository.queueEvent(type)
                    if (type == DeviceEventType.SCREEN_ON &&
                        context.getSystemService(PowerManager::class.java).isInteractive &&
                        !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
                    ) {
                        // Devices without a lock screen may never send USER_PRESENT.
                        if (repository.queueEvent(DeviceEventType.PHONE_UNLOCKED)) {
                            repository.queueEvent(DeviceEventType.HEARTBEAT)
                        }
                    } else if (type == DeviceEventType.PHONE_UNLOCKED && queued) {
                        repository.queueEvent(DeviceEventType.HEARTBEAT)
                    }
                    EventSyncWorker.enqueue(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

}
