package com.amiawake.android.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amiawake.android.AmIAwakeApplication
import com.amiawake.android.MainActivity
import com.amiawake.android.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.runBlocking

// FID mode observes onRegistered(); onNewToken belongs to the legacy token mode.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class AwakeMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        // Let the durable worker read the latest FID, including after account changes.
        PushRegistrationWorker.enqueue(this, installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val container = (application as AmIAwakeApplication).container
        if (runBlocking { container.sessionStore.current() } == null) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && android.os.Build.VERSION.SDK_INT >= 33) return
        val notification = message.notification ?: return
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Уведомления друзей", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        NotificationManagerCompat.from(this).notify(
            message.messageId?.hashCode() ?: System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notification.title ?: "Awake?")
                .setContentText(notification.body)
                .setAutoCancel(true)
                .setContentIntent(intent)
                .build(),
        )
    }

    companion object { const val CHANNEL_ID = "friends" }
}
