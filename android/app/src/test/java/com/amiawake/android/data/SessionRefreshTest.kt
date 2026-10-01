package com.amiawake.android.data

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class SessionRefreshTest {
    private lateinit var server: MockWebServer
    private lateinit var store: MemoryTokenStore
    private lateinit var api: AmIAwakeApi

    @Before fun setUp() {
        server = MockWebServer()
        server.start()
        store = MemoryTokenStore(Session("old-access", "old-refresh", 0))
        api = NetworkStack(store, server.url("/").toString()).api
    }
    @After fun tearDown() { server.shutdown() }
    private fun reply(code: Int, body: String = "") = MockResponse().setResponseCode(code)
        .setHeader("Content-Type", "application/json").setBody(body)
    private val tokens = """{"accessToken":"new-access","refreshToken":"new-refresh","tokenType":"Bearer","expiresIn":900}"""
    private val user = """{"id":"uuid","username":"alice","displayName":"Alice","timeZone":"UTC","status":"AVAILABLE"}"""

    @Test fun expiredAccessTokenRotatesAndRetriesOriginalRequest() = runBlocking {
        server.enqueue(reply(401))
        server.enqueue(reply(200, tokens))
        server.enqueue(reply(200, user))
        assertEquals("alice", api.me().username)
        assertEquals("Bearer old-access", server.takeRequest().getHeader("Authorization"))
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertNull(refresh.getHeader("Authorization"))
        assertEquals("""{"refreshToken":"old-refresh"}""", refresh.body.readUtf8())
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
        assertEquals("new-refresh", store.value?.refreshToken)
    }

    @Test fun invalidRefreshClearsSession() = runBlocking {
        server.enqueue(reply(401))
        server.enqueue(reply(401))
        expectUnauthorized { api.me() }
        assertNull(store.value)
        assertEquals(2, server.requestCount)
    }

    @Test fun transientRefreshFailurePreservesSession() = runBlocking {
        server.enqueue(reply(401))
        server.enqueue(reply(503))
        expectUnauthorized { api.me() }
        assertEquals("old-refresh", store.value?.refreshToken)
    }

    @Test fun loginFailureNeverRefreshesExistingSession() = runBlocking {
        server.enqueue(reply(401))
        expectUnauthorized { api.login(LoginRequest("alice", "wrong-password")) }
        assertEquals(1, server.requestCount)
        assertNull(server.takeRequest().getHeader("Authorization"))
        assertEquals("old-refresh", store.value?.refreshToken)
    }

    @Test fun responseAfterLogoutCannotRestoreSession() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/auth/refresh" -> { store.value = null; reply(200, tokens) }
                else -> reply(401)
            }
        }
        expectUnauthorized { api.me() }
        assertNull(store.value)
        assertEquals(2, server.requestCount)
    }

    @Test fun failedOldRefreshCannotClearNewLogin() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/api/v1/auth/refresh" -> {
                    store.value = Session("other-access", "other-refresh", 900)
                    reply(401)
                }
                else -> reply(401)
            }
        }
        expectUnauthorized { api.me() }
        assertEquals("other-refresh", store.value?.refreshToken)
    }

    @Test fun rejectedRotatedAccessStopsRetryLoopAndClearsSession() = runBlocking {
        server.enqueue(reply(401))
        server.enqueue(reply(200, tokens))
        server.enqueue(reply(401))
        expectUnauthorized { api.me() }
        assertEquals(3, server.requestCount)
        assertNull(store.value)
    }

    @Test fun inFlightRequestNeverRetriesAsAnotherAccount() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                store.value = Session("other-access", "other-refresh", 900, "another-login")
                return reply(401)
            }
        }
        expectUnauthorized { api.me() }
        assertEquals(1, server.requestCount)
        assertEquals("other-refresh", store.value?.refreshToken)
    }

    private suspend fun expectUnauthorized(block: suspend () -> Unit) {
        try { block(); fail("Expected unauthorized") }
        catch (error: HttpException) { assertEquals(401, error.code()) }
    }

    private class MemoryTokenStore(@Volatile var value: Session?) : TokenStore {
        override suspend fun current(): Session? = value
        override suspend fun saveIfCurrent(expectedRefreshToken: String, tokens: TokenResponse): Boolean = synchronized(this) {
            if (value?.refreshToken != expectedRefreshToken) false else {
                value = Session(tokens.accessToken, tokens.refreshToken, tokens.expiresIn, value!!.sessionId)
                true
            }
        }
        override suspend fun clearIfCurrent(expectedRefreshToken: String) {
            synchronized(this) { if (value?.refreshToken == expectedRefreshToken) value = null }
        }
    }
}
