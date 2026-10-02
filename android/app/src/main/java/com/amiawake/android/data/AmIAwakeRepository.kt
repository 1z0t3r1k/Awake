package com.amiawake.android.data

import retrofit2.HttpException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import android.os.SystemClock
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.time.Instant

class AmIAwakeRepository(
    private val api: AmIAwakeApi,
    private val sessionStore: SessionStore,
    private val eventQueue: EventQueue,
) {
    private val telemetryMutex = Mutex()
    private var lastHeartbeatElapsedMillis: Long? = null
    private var unlockReported = false

    suspend fun register(username: String, password: String, displayName: String) {
        require(displayName.trim().length <= 32) { "Имя должно быть не длиннее 32 символов" }
        api.register(RegisterRequest(username.trim(), password))
        login(username, password)
    }

    suspend fun configureNewProfile(displayName: String) {
        api.setTimeZone(TimeZoneRequest(ZoneId.systemDefault().id))
        if (displayName.trim().isNotEmpty()) api.setDisplayName(DisplayNameRequest(displayName.trim()))
    }

    suspend fun login(username: String, password: String) = telemetryMutex.withLock {
        val tokens = api.login(LoginRequest(username.trim(), password))
        eventQueue.clear()
        sessionStore.save(tokens)
        lastHeartbeatElapsedMillis = null
        unlockReported = false
    }

    suspend fun logout() = telemetryMutex.withLock {
        val refreshToken = sessionStore.current()?.refreshToken
        try {
            if (refreshToken != null) api.logout(LogoutRequest(refreshToken))
        } finally {
            sessionStore.clear()
            eventQueue.clear()
        }
    }

    suspend fun deleteAccount() = telemetryMutex.withLock {
        api.deleteAccount()
        sessionStore.clear()
        eventQueue.clear()
    }

    suspend fun registerDevice(installationId: String) = telemetryMutex.withLock {
        if (sessionStore.current() == null) return@withLock
        api.registerDevice(DeviceRegistrationRequest(installationId))
    }

    suspend fun loadDashboard(): DashboardData {
        val user = api.me()
        return DashboardData(
            user = user,
            status = user.status,
            userState = loadUserState(),
            pendingEventCount = eventQueue.count(),
        )
    }

    private suspend fun loadUserState(): UserStateResponse? = try {
        api.getUserState()
    } catch (error: HttpException) {
        if (error.code() == 404) null else throw error
    }

    suspend fun setStatus(status: AvailabilityStatus): AvailabilityStatus =
        api.setStatus(StatusRequest(status)).status

    suspend fun updateDisplayName(displayName: String): UserResponse {
        api.setDisplayName(DisplayNameRequest(displayName.trim()))
        return api.me()
    }

    suspend fun updateTimeZone(zoneId: String): UserResponse {
        api.setTimeZone(TimeZoneRequest(zoneId.trim()))
        return api.me()
    }

    suspend fun searchUsers(query: String): List<UserSearchResponse> = api.searchUsers(query.trim())

    suspend fun loadFriends(): FriendsData = FriendsData(
        friends = api.friends(),
        incoming = api.incomingRequests(),
        outgoing = api.outgoingRequests(),
    )

    suspend fun sendFriendRequest(username: String) { api.sendFriendRequest(FriendRequest(username.trim())) }
    suspend fun acceptFriendRequest(username: String) { api.acceptFriendRequest(username) }
    suspend fun deleteFriend(username: String) { api.deleteFriend(username) }
    suspend fun deletePendingRequest(username: String) { api.deletePendingRequest(username) }

    suspend fun loadSchedule(): SleepScheduleResponse? = try {
        api.getSleepSchedule()
    } catch (error: HttpException) {
        // A missing schedule is an empty product state. Other failures must remain visible to the UI.
        if (error.code() == 404) null else throw error
    }
    suspend fun saveSchedule(sleepTime: String, wakeTime: String): SleepScheduleResponse =
        api.setSleepSchedule(SleepScheduleRequest(sleepTime, wakeTime))
    suspend fun setScheduleEnabled(enabled: Boolean): SleepScheduleResponse =
        api.setSleepScheduleEnabled(SleepScheduleEnabledRequest(enabled))
    suspend fun deleteSchedule() { api.deleteSleepSchedule() }

    suspend fun queueSleepClassification(request: SleepClassificationRequest) = telemetryMutex.withLock {
        if (sessionStore.current() != null) eventQueue.enqueueClassification(request)
    }

    suspend fun queueEvent(type: DeviceEventType, occurredAt: Instant = Instant.now()): Boolean = telemetryMutex.withLock {
        if (type == DeviceEventType.SCREEN_OFF) unlockReported = false
        if (sessionStore.current() == null) return@withLock false
        val elapsedMillis = SystemClock.elapsedRealtime()
        if (type == DeviceEventType.HEARTBEAT && lastHeartbeatElapsedMillis?.let {
                elapsedMillis - it < java.util.concurrent.TimeUnit.MINUTES.toMillis(5)
            } == true) return@withLock false
        // USER_PRESENT, SCREEN_ON fallback and activity resume can describe the same unlock.
        if (type == DeviceEventType.PHONE_UNLOCKED && unlockReported) return@withLock false
        if (!eventQueue.enqueue(type, occurredAt)) return@withLock false
        if (type == DeviceEventType.HEARTBEAT) lastHeartbeatElapsedMillis = elapsedMillis
        if (type == DeviceEventType.PHONE_UNLOCKED) unlockReported = true
        true
    }

    suspend fun observeChargingState(context: Context): Boolean {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        if (plugged < 0) return false
        // This is an observation made now, not an invented time for a missed cable event.
        return queueEvent(if (plugged != 0) DeviceEventType.CHARGING_STARTED else DeviceEventType.CHARGING_STOPPED)
    }

    suspend fun syncEvents(): Int = telemetryMutex.withLock {
        var sent = 0
        while (sessionStore.current() != null) {
            val events = eventQueue.peek()
            if (events.isEmpty() || sessionStore.current() == null) break
            api.sendEventBatch(DeviceEventBatchRequest(events))
            eventQueue.remove(events.mapTo(mutableSetOf()) { it.eventId })
            sent += events.size
        }
        while (sessionStore.current() != null) {
            val classifications = eventQueue.peekClassifications()
            if (classifications.isEmpty()) break
            for (classification in classifications) {
                if (sessionStore.current() == null) return@withLock sent
                api.sendSleepClassification(classification)
                eventQueue.removeClassification(classification)
                sent++
            }
        }
        sent
    }
}

data class DashboardData(
    val user: UserResponse,
    val status: AvailabilityStatus,
    val userState: UserStateResponse?,
    val pendingEventCount: Int,
)

data class FriendsData(
    val friends: List<FriendResponse>,
    val incoming: List<IncomingFriendRequest>,
    val outgoing: List<OutgoingFriendRequest>,
)
