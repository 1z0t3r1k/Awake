package com.amiawake.android.data

import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val username: String, val password: String)
@Serializable data class RegisterRequest(val username: String, val password: String)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class LogoutRequest(val refreshToken: String)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val displayName: String,
    val timeZone: String,
    val status: AvailabilityStatus,
)

@Serializable data class StatusRequest(val status: AvailabilityStatus)
@Serializable data class StatusResponse(val status: AvailabilityStatus)
@Serializable data class DisplayNameRequest(val displayName: String)
@Serializable data class TimeZoneRequest(val zoneId: String)
@Serializable data class UserSearchResponse(val userId: String, val username: String, val displayName: String)

@Serializable data class FriendRequest(val username: String)
@Serializable
data class FriendResponse(
    val username: String,
    val displayName: String,
    val status: AvailabilityStatus,
    val sleepState: SleepState,
    val sleepConfidence: Double,
    val sleepStateCalculatedAt: String? = null,
)
@Serializable data class IncomingFriendRequest(val username: String)
@Serializable data class OutgoingFriendRequest(val username: String)

@Serializable data class SleepScheduleRequest(val sleepTime: String, val wakeTime: String)
@Serializable data class SleepScheduleEnabledRequest(val enabled: Boolean)
@Serializable data class SleepScheduleResponse(val sleepTime: String, val wakeTime: String, val enabled: Boolean)

@Serializable
data class DeviceEventRequest(
    val eventId: String,
    val type: DeviceEventType,
    val occurredAt: String,
)

@Serializable data class DeviceEventBatchRequest(val events: List<DeviceEventRequest>)

@Serializable
data class UserStateResponse(
    val state: SleepState,
    val confidence: Double,
    val calculatedAt: String? = null,
)

@Serializable
data class ApiErrorResponse(
    val status: Int? = null,
    val message: String? = null,
    val errors: Map<String, String> = emptyMap(),
)

@Serializable enum class AvailabilityStatus { AVAILABLE, TEXT_ONLY, DO_NOT_DISTURB }
@Serializable enum class SleepState { SLEEPING, AWAKE, UNKNOWN }
@Serializable enum class DeviceEventType {
    SCREEN_ON,
    SCREEN_OFF,
    PHONE_UNLOCKED,
    CHARGING_STARTED,
    CHARGING_STOPPED,
    MOTION,
    HEARTBEAT,
}
