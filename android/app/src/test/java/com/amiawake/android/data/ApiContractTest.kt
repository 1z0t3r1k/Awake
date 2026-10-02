package com.amiawake.android.data

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit

class ApiContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AmIAwakeApi
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build().create(AmIAwakeApi::class.java)
    }

    @After fun tearDown() { server.shutdown() }

    private fun reply(code: Int, body: String = "") {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }

    @Test fun failedTelemetryBatchThrowsInsteadOfAcknowledgingEvents() = runBlocking {
        for (code in listOf(400, 401, 403, 500, 503)) {
            reply(code, """{"status":$code,"message":"Rejected","errors":{}}""")
            try {
                api.sendEventBatch(DeviceEventBatchRequest(listOf(DeviceEventRequest("00000000-0000-0000-0000-000000000001", DeviceEventType.HEARTBEAT, "2026-10-01T00:00:00Z"))))
                fail("HTTP $code must not be acknowledged")
            } catch (error: HttpException) { assertEquals(code, error.code()) }
        }
    }

    @Test fun successfulBatchUsesBackendEnvelope() = runBlocking {
        reply(204)
        api.sendEventBatch(DeviceEventBatchRequest(listOf(DeviceEventRequest("00000000-0000-0000-0000-000000000001", DeviceEventType.PHONE_UNLOCKED, "2026-10-01T00:00:00Z"))))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/device-events/batch", request.path)
        assertEquals(json.parseToJsonElement("""{"events":[{"eventId":"00000000-0000-0000-0000-000000000001","type":"PHONE_UNLOCKED","occurredAt":"2026-10-01T00:00:00Z"}]}"""), json.parseToJsonElement(request.body.readUtf8()))
    }

    @Test fun rejectedProfileAndFriendChangesThrow() = runBlocking {
        val operations: List<suspend () -> Unit> = listOf(
            { api.setDisplayName(DisplayNameRequest("Name")) },
            { api.setTimeZone(TimeZoneRequest("Europe/Moscow")) },
            { api.sendFriendRequest(FriendRequest("friend")) },
            { api.acceptFriendRequest("friend") },
            { api.deleteFriend("friend") },
            { api.deletePendingRequest("friend") },
            { api.deleteSleepSchedule() },
            { api.deleteAccount() },
            { api.registerDevice(DeviceRegistrationRequest("installation")) },
            { api.sendSleepClassification(SleepClassificationRequest("2026-10-01T00:00:00Z", 80, 1, 2)) },
        )
        for (operation in operations) {
            reply(403)
            try { operation(); fail("Forbidden mutation must throw") }
            catch (error: HttpException) { assertEquals(403, error.code()) }
        }
    }

    @Test fun accountDeletionAcceptsNoContent() = runBlocking {
        reply(204)
        api.deleteAccount()
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/users/me", request.path)
    }

    @Test fun deviceRegistrationSendsInstallationId() = runBlocking {
        reply(204)
        api.registerDevice(DeviceRegistrationRequest("firebase-installation"))
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/device-registrations", request.path)
        assertEquals("""{"firebaseInstallationId":"firebase-installation"}""", request.body.readUtf8())
    }

    @Test fun friendsSupportMissingInferenceTimestamp() = runBlocking {
        reply(200, """[{"username":"friend","displayName":"Friend","status":"TEXT_ONLY","sleepState":"UNKNOWN","sleepConfidence":0.0,"sleepStateCalculatedAt":null}]""")
        val friend = api.friends().single()
        assertEquals(AvailabilityStatus.TEXT_ONLY, friend.status)
        assertEquals(SleepState.UNKNOWN, friend.sleepState)
        assertNull(friend.sleepStateCalculatedAt)
    }

    @Test fun userAndStateDecodeServerFields() = runBlocking {
        reply(200, """{"id":"uuid","username":"alice","displayName":"Alice","timeZone":"Europe/Moscow","status":"DO_NOT_DISTURB"}""")
        assertEquals("Europe/Moscow", api.me().timeZone)
        reply(200, """{"state":"SLEEPING","confidence":0.9,"calculatedAt":"2026-10-01T00:00:00Z"}""")
        val state = api.getUserState()
        assertEquals(SleepState.SLEEPING, state.state)
        assertEquals(0.9, state.confidence, 0.0)
    }

    @Test fun scheduleUsesLocalTimeAndEnabledFlag() = runBlocking {
        reply(200, """{"sleepTime":"23:00:00","wakeTime":"07:00:00","enabled":false}""")
        val schedule = api.setSleepSchedule(SleepScheduleRequest("23:00", "07:00"))
        assertFalse(schedule.enabled)
        assertEquals("07:00:00", schedule.wakeTime)
        assertEquals("""{"sleepTime":"23:00","wakeTime":"07:00"}""", server.takeRequest().body.readUtf8())
    }

    @Test fun loginDecodesRotatingTokenPair() = runBlocking {
        reply(200, """{"accessToken":"access","refreshToken":"refresh","tokenType":"Bearer","expiresIn":900}""")
        val tokens = api.login(LoginRequest("alice", "password"))
        assertEquals("refresh", tokens.refreshToken)
        assertEquals(900L, tokens.expiresIn)
    }

    @Test fun motionAndPowerEventsUseTheExistingBatchContract() = runBlocking {
        reply(204)
        val types = listOf(DeviceEventType.MOTION, DeviceEventType.CHARGING_STARTED, DeviceEventType.CHARGING_STOPPED)
        val events = types.mapIndexed { index, type -> DeviceEventRequest(
            "00000000-0000-0000-0000-00000000000${index + 1}", type, "2026-10-02T00:00:00Z") }
        api.sendEventBatch(DeviceEventBatchRequest(events))
        val request = server.takeRequest()
        assertEquals("/api/v1/device-events/batch", request.path)
        val body = request.body.readUtf8()
        for (type in types) assertTrue(body.contains("\"type\":\"${type.name}\""))
        assertEquals(json.encodeToString(DeviceEventBatchRequest.serializer(), DeviceEventBatchRequest(events)), body)
    }
}
