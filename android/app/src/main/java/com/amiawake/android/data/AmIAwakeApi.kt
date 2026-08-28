package com.amiawake.android.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AmIAwakeApi {
    @POST("api/v1/users") suspend fun register(@Body request: RegisterRequest): UserResponse
    @POST("api/v1/auth/login") suspend fun login(@Body request: LoginRequest): TokenResponse
    @POST("api/v1/auth/refresh") suspend fun refresh(@Body request: RefreshRequest): TokenResponse
    @POST("api/v1/auth/logout") suspend fun logout(@Body request: LogoutRequest): Response<Unit>

    @GET("api/v1/users/me") suspend fun me(): UserResponse
    @GET("api/v1/users/me/status") suspend fun getStatus(): StatusResponse
    @PATCH("api/v1/users/me/status") suspend fun setStatus(@Body request: StatusRequest): StatusResponse
    @PATCH("api/v1/users/me/display-name") suspend fun setDisplayName(@Body request: DisplayNameRequest): Response<Unit>
    @PATCH("api/v1/users/me/time-zone") suspend fun setTimeZone(@Body request: TimeZoneRequest): Response<Unit>
    @GET("api/v1/users/search") suspend fun searchUsers(@Query("query") query: String): List<UserSearchResponse>

    @GET("api/v1/friendship") suspend fun friends(): List<FriendResponse>
    @GET("api/v1/friendship/requests/incoming") suspend fun incomingRequests(): List<IncomingFriendRequest>
    @GET("api/v1/friendship/requests/outgoing") suspend fun outgoingRequests(): List<OutgoingFriendRequest>
    @POST("api/v1/friendship/requests") suspend fun sendFriendRequest(@Body request: FriendRequest): Response<Unit>
    @POST("api/v1/friendship/{username}/accept") suspend fun acceptFriendRequest(@Path("username") username: String): Response<Unit>
    @DELETE("api/v1/friendship/{username}") suspend fun deleteFriend(@Path("username") username: String): Response<Unit>
    @DELETE("api/v1/friendship/requests/{username}") suspend fun deletePendingRequest(@Path("username") username: String): Response<Unit>
    @GET("api/v1/friendship/{username}/state") suspend fun getFriendState(@Path("username") username: String): UserStateResponse

    @GET("api/v1/sleep-schedule") suspend fun getSleepSchedule(): SleepScheduleResponse
    @PUT("api/v1/sleep-schedule") suspend fun setSleepSchedule(@Body request: SleepScheduleRequest): SleepScheduleResponse
    @PATCH("api/v1/sleep-schedule/enabled") suspend fun setSleepScheduleEnabled(@Body request: SleepScheduleEnabledRequest): SleepScheduleResponse
    @DELETE("api/v1/sleep-schedule") suspend fun deleteSleepSchedule(): Response<Unit>

    @GET("api/v1/user-state/me") suspend fun getUserState(): UserStateResponse
    @POST("api/v1/device-events") suspend fun sendEvent(@Body request: DeviceEventRequest): Response<Unit>
    @POST("api/v1/device-events/batch") suspend fun sendEventBatch(@Body request: DeviceEventBatchRequest): Response<Unit>
}
