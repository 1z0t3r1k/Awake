package com.amiawake.android.telemetry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amiawake.android.AmIAwakeApplication
import com.amiawake.android.data.DeviceEventType
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MotionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = ActivityRecognitionResult.extractResult(intent) ?: return
        val activity = result.mostProbableActivity
        if (activity.confidence < 75 || activity.type !in MOVING_TYPES) return
        val occurredAt = Instant.ofEpochMilli(result.time)
        if (occurredAt.isAfter(Instant.now())) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = (context.applicationContext as AmIAwakeApplication).container.repository
                if (repository.queueEvent(DeviceEventType.MOTION, occurredAt)) EventSyncWorker.enqueue(context)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("AmIAwakeMotion", "Failed to queue motion", error)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val MOVING_TYPES = setOf(DetectedActivity.IN_VEHICLE, DetectedActivity.ON_BICYCLE,
            DetectedActivity.ON_FOOT, DetectedActivity.WALKING, DetectedActivity.RUNNING)
    }
}
