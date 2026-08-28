package com.amiawake.android

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.amiawake.android.sleep.SleepClassificationReceiver
import com.amiawake.android.telemetry.TelemetryReceiver
import com.amiawake.android.ui.AmIAwakeApp
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest

class MainActivity : ComponentActivity() {
    private val telemetryReceiver = TelemetryReceiver()
    private val activityRecognitionPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            subscribeToSleepClassifications()
        } else {
            Log.w(TAG, "ACTIVITY_RECOGNITION permission denied; Sleep API is not subscribed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AmIAwakeApp() }
        ensureSleepApiSubscription()
    }

    private fun ensureSleepApiSubscription() {
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            subscribeToSleepClassifications()
        } else {
            activityRecognitionPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToSleepClassifications() {
        val receiverIntent = Intent(this, SleepClassificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            SLEEP_PENDING_INTENT_REQUEST_CODE,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val request = SleepSegmentRequest(SleepSegmentRequest.CLASSIFY_EVENTS_ONLY)

        ActivityRecognition.getClient(this)
            .requestSleepSegmentUpdates(pendingIntent, request)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to SleepClassifyEvent updates")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to subscribe to SleepClassifyEvent updates", exception)
            }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        ContextCompat.registerReceiver(this, telemetryReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        unregisterReceiver(telemetryReceiver)
        super.onStop()
    }

    private companion object {
        const val TAG = "AmIAwakeSleepApi"
        const val SLEEP_PENDING_INTENT_REQUEST_CODE = 1001
    }
}
