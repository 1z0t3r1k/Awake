package com.amiawake.android.data

import com.amiawake.android.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit

class NetworkStack(private val sessionStore: TokenStore, private val baseUrl: String = BuildConfig.API_BASE_URL) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val publicRequest = chain.request().url.encodedPath in PUBLIC_PATHS
            val session = if (publicRequest) null else runBlocking { sessionStore.current() }
            val request = if (session == null) chain.request() else chain.request().newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .tag(AuthSessionTag::class.java, AuthSessionTag(session.sessionId))
                .build()
            chain.proceed(request)
        }
        .authenticator(SessionAuthenticator(sessionStore, json, baseUrl))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val api: AmIAwakeApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AmIAwakeApi::class.java)
}

private val PUBLIC_PATHS = setOf("/api/v1/users", "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout")
private data class AuthSessionTag(val id: String)

private class SessionAuthenticator(
    private val sessionStore: TokenStore,
    private val json: Json,
    private val baseUrl: String,
) : Authenticator {
    private val refreshClient = OkHttpClient()
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? = synchronized(lock) {
        if (response.request.url.encodedPath in PUBLIC_PATHS) return null

        val session = runBlocking { sessionStore.current() } ?: return null
        if (response.request.tag(AuthSessionTag::class.java)?.id != session.sessionId) return null
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (responseCount(response) >= 2) {
            if (requestToken == session.accessToken) {
                runBlocking { sessionStore.clearIfCurrent(session.refreshToken) }
            }
            return null
        }
        if (requestToken != null && requestToken != session.accessToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .build()
        }

        val body = json.encodeToString(RefreshRequest(session.refreshToken))
            .toRequestBody("application/json".toMediaType())
        val refreshRequest = Request.Builder()
            .url(baseUrl + "api/v1/auth/refresh")
            .post(body)
            .build()

        val tokens = try {
            refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    if (refreshResponse.code == 401) {
                        runBlocking { sessionStore.clearIfCurrent(session.refreshToken) }
                    }
                    return@synchronized null
                }
                val raw = refreshResponse.body?.string() ?: return@synchronized null
                json.decodeFromString<TokenResponse>(raw)
            }
        } catch (_: Exception) {
            return null
        }

        if (!runBlocking { sessionStore.saveIfCurrent(session.refreshToken, tokens) }) return null
        response.request.newBuilder()
            .header("Authorization", "Bearer ${tokens.accessToken}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var previous = response.priorResponse
        while (previous != null) {
            count++
            previous = previous.priorResponse
        }
        return count
    }
}

fun Throwable.userMessage(json: Json, authenticating: Boolean = false): String = when (this) {
    is HttpException -> {
        val raw = response()?.errorBody()?.string()
        val apiError = raw?.let { runCatching { json.decodeFromString<ApiErrorResponse>(it) }.getOrNull() }
        val backendMessage = apiError?.errors?.values?.firstOrNull() ?: apiError?.message.orEmpty()
        when {
            code() == 401 -> if (authenticating) "Неверный логин или пароль" else "Не удалось подтвердить сессию. Проверьте подключение и повторите."
            code() == 409 && backendMessage.contains("username", ignoreCase = true) -> "Такое имя пользователя уже занято"
            code() == 404 && backendMessage.contains("user", ignoreCase = true) -> "Пользователь с таким именем не найден"
            code() == 409 && backendMessage.contains("friend", ignoreCase = true) -> "Заявка уже отправлена или вы уже друзья"
            code() == 403 -> "Нет доступа к этому действию"
            apiError?.errors?.containsKey("displayName") == true -> "Введите имя до 32 символов"
            apiError?.errors?.containsKey("username") == true -> "Имя пользователя: 3–32 символа, латинские буквы, цифры и _"
            apiError?.errors?.containsKey("password") == true -> "Пароль: от 8 до 256 символов"
            backendMessage.contains("sleep", true) && code() == 400 -> "Время сна и пробуждения должны отличаться"
            code() in 500..599 -> "Сервис временно недоступен. Попробуйте позже."
            backendMessage.isNotBlank() -> "Не удалось выполнить действие. Проверьте данные и попробуйте снова."
            else -> "Не удалось выполнить действие. Попробуйте снова."
        }
    }
    is IOException -> "Не удалось подключиться. Проверьте интернет."
    is IllegalArgumentException -> message ?: "Проверьте введённые данные"
    else -> "Что-то пошло не так. Попробуйте снова."
}
