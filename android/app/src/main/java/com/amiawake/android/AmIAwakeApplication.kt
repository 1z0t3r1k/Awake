package com.amiawake.android

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.amiawake.android.telemetry.TelemetryReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import com.amiawake.android.push.AwakeMessagingService
import com.amiawake.android.push.PushRegistrationWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.amiawake.android.data.AmIAwakeRepository
import com.amiawake.android.data.EventQueue
import com.amiawake.android.data.NetworkStack
import com.amiawake.android.data.SessionStore
import com.amiawake.android.telemetry.HeartbeatWorker
import java.util.concurrent.TimeUnit

class AmIAwakeApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            container.sessionStore.session.map { it?.sessionId }.distinctUntilChanged().collect { sessionId ->
                if (FirebaseApp.getApps(this@AmIAwakeApplication).isNotEmpty()) {
                    FirebaseMessaging.getInstance().isAutoInitEnabled = sessionId != null
                    PushRegistrationWorker.enqueue(this@AmIAwakeApplication)
                }
            }
        }
        scheduleHeartbeat()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(AwakeMessagingService.CHANNEL_ID, "Уведомления друзей", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, TelemetryReceiver(), filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun scheduleHeartbeat() {
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HeartbeatWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

class AppContainer(val application: Application) {
    val sessionStore = SessionStore(application)
    val network = NetworkStack(sessionStore)
    val eventQueue = EventQueue(application, network.json)
    val repository = AmIAwakeRepository(network.api, sessionStore, eventQueue)
}
