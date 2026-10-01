package com.amiawake.android

import android.Manifest
import android.app.KeyguardManager
import android.os.PowerManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import com.amiawake.android.data.DeviceEventType
import com.amiawake.android.telemetry.EventSyncWorker
import java.util.concurrent.TimeUnit
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.amiawake.android.sleep.SleepClassificationReceiver
import com.amiawake.android.ui.AmIAwakeApp
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest

class MainActivity : ComponentActivity() {
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val container = (application as AmIAwakeApplication).container
                container.sessionStore.session.map { it?.sessionId }.distinctUntilChanged().collectLatest { sessionId ->
                    if (sessionId == null) return@collectLatest
                    val power = getSystemService(PowerManager::class.java)
                    val keyguard = getSystemService(KeyguardManager::class.java)
                    var pendingUnlock = power.isInteractive && !keyguard.isKeyguardLocked
                    while (true) {
                        try {
                            val unlocked = pendingUnlock && container.repository.queueEvent(DeviceEventType.PHONE_UNLOCKED)
                            pendingUnlock = false
                            val heartbeat = container.repository.queueEvent(DeviceEventType.HEARTBEAT)
                            if (unlocked || heartbeat) EventSyncWorker.enqueue(this@MainActivity)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            Log.e(TAG, "Failed to queue presence events", error)
                        }
                        delay(TimeUnit.MINUTES.toMillis(5))
                    }
                }
            }
        }
        lifecycleScope.launch {
            (application as AmIAwakeApplication).container.sessionStore.session
                .map { it != null }.distinctUntilChanged().collect { authenticated ->
                    if (authenticated) ensureSleepApiSubscription()
                    else ActivityRecognition.getClient(this@MainActivity).removeSleepSegmentUpdates(sleepPendingIntent())
                }
        }
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
        val request = SleepSegmentRequest(SleepSegmentRequest.CLASSIFY_EVENTS_ONLY)

        ActivityRecognition.getClient(this)
            .requestSleepSegmentUpdates(sleepPendingIntent(), request)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to SleepClassifyEvent updates")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to subscribe to SleepClassifyEvent updates", exception)
            }
    }

    private fun sleepPendingIntent(): PendingIntent {
        val receiverIntent = Intent(this, SleepClassificationReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            SLEEP_PENDING_INTENT_REQUEST_CODE,
            receiverIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private companion object {
        const val TAG = "AmIAwakeSleepApi"
        const val SLEEP_PENDING_INTENT_REQUEST_CODE = 1001
    }
}
