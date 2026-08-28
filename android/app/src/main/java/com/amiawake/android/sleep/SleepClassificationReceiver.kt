package com.amiawake.android.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amiawake.android.AmIAwakeApplication
import com.amiawake.android.data.SleepClassificationRequest
import com.google.android.gms.location.SleepClassifyEvent
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepClassificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!SleepClassifyEvent.hasEvents(intent)) {
            Log.d(TAG, "Sleep API callback received without classification events")
            return
        }

        val events = SleepClassifyEvent.extractEvents(intent)
        events.forEach { event ->
            Log.d(
                TAG,
                "SleepClassifyEvent(" +
                    "timestamp=${event.timestampMillis}, " +
                    "sleepConfidence=${event.confidence}, " +
                    "motion=${event.motion}, " +
                    "light=${event.light}" +
                    ")",
            )
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AmIAwakeApplication
                if (app.container.sessionStore.current() == null) {
                    Log.w(TAG, "Skipping ${events.size} sleep classification event(s): no active session")
                    return@launch
                }

                events.forEach { event ->
                    val request = SleepClassificationRequest(
                        occurredAt = Instant.ofEpochMilli(event.timestampMillis).toString(),
                        sleepConfidence = event.confidence,
                        motion = event.motion,
                        light = event.light,
                    )
                    runCatching { app.container.repository.sendSleepClassification(request) }
                        .onSuccess {
                            Log.d(TAG, "Sent sleep classification for ${request.occurredAt}")
                        }
                        .onFailure { error ->
                            Log.e(TAG, "Failed to send sleep classification for ${request.occurredAt}", error)
                        }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AmIAwakeSleepApi"
    }
}
