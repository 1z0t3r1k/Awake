package com.amiawake.android.push

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.amiawake.android.AmIAwakeApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException

class PushRegistrationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as AmIAwakeApplication).container
        if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.success()
        return try {
            val messaging = FirebaseMessaging.getInstance()
            if (container.sessionStore.current() == null) {
                messaging.isAutoInitEnabled = false
                messaging.unregister().await()
                return Result.success()
            }
            messaging.isAutoInitEnabled = true
            if (inputData.getString("installationId") == null) {
                FirebaseMessaging.getInstance().register().await()
            }
            val installationId = FirebaseInstallations.getInstance().id.await()
            container.repository.registerDevice(installationId)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            if (error.code() == 401 && container.sessionStore.current() == null) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, installationId: String? = null) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "push-registration",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                OneTimeWorkRequestBuilder<PushRegistrationWorker>()
                    .setInputData(workDataOf("installationId" to installationId))
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build(),
            )
        }
    }
}
