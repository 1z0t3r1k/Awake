package com.amiawake.android.data

import retrofit2.HttpException

class AmIAwakeRepository(
    private val api: AmIAwakeApi,
    private val sessionStore: SessionStore,
    private val eventQueue: EventQueue,
) {
    suspend fun register(username: String, password: String, displayName: String) {
        api.register(RegisterRequest(username.trim(), password))
        login(username, password)
        if (displayName.trim().isNotEmpty() && displayName.trim() != username.trim()) {
            api.setDisplayName(DisplayNameRequest(displayName.trim()))
        }
    }

    suspend fun login(username: String, password: String) {
        sessionStore.save(api.login(LoginRequest(username.trim(), password)))
    }

    suspend fun logout() {
        val refreshToken = sessionStore.current()?.refreshToken
        try {
            if (refreshToken != null) api.logout(LogoutRequest(refreshToken))
        } finally {
            sessionStore.clear()
        }
    }

    suspend fun loadDashboard(): DashboardData {
        val user = api.me()
        return DashboardData(
            user = user,
            status = user.status,
            userState = runCatching { api.getUserState() }.getOrNull(),
            pendingEventCount = eventQueue.count(),
        )
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

    suspend fun sendSleepClassification(request: SleepClassificationRequest) {
        val response = api.sendSleepClassification(request)
        if (!response.isSuccessful) throw HttpException(response)
    }

    suspend fun queueEvent(type: DeviceEventType) { eventQueue.enqueue(type) }

    suspend fun syncEvents(): Int {
        var sent = 0
        while (true) {
            val events = eventQueue.peek()
            if (events.isEmpty()) return sent
            api.sendEventBatch(DeviceEventBatchRequest(events))
            eventQueue.remove(events.mapTo(mutableSetOf()) { it.eventId })
            sent += events.size
        }
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
