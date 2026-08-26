package com.amiawake.android

import android.app.Application
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
        scheduleHeartbeat()
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

class AppContainer(application: Application) {
    val sessionStore = SessionStore(application)
    val network = NetworkStack(sessionStore)
    val eventQueue = EventQueue(application, network.json)
    val repository = AmIAwakeRepository(network.api, sessionStore, eventQueue)
}
