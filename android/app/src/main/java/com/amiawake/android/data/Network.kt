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

class NetworkStack(private val sessionStore: SessionStore) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = runBlocking { sessionStore.current()?.accessToken }
            val request = if (token == null) chain.request() else chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
        .authenticator(SessionAuthenticator(sessionStore, json))
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    val api: AmIAwakeApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AmIAwakeApi::class.java)
}

private class SessionAuthenticator(
    private val sessionStore: SessionStore,
    private val json: Json,
) : Authenticator {
    private val refreshClient = OkHttpClient()
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? = synchronized(lock) {
        if (responseCount(response) >= 2) return null

        val session = runBlocking { sessionStore.current() } ?: return null
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (requestToken != null && requestToken != session.accessToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .build()
        }

        val body = json.encodeToString(RefreshRequest(session.refreshToken))
            .toRequestBody("application/json".toMediaType())
        val refreshRequest = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "api/v1/auth/refresh")
            .post(body)
            .build()

        val tokens = try {
            refreshClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) return@synchronized null
                val raw = refreshResponse.body?.string() ?: return@synchronized null
                json.decodeFromString<TokenResponse>(raw)
            }
        } catch (_: Exception) {
            return null
        }

        runBlocking { sessionStore.save(tokens) }
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

fun Throwable.userMessage(json: Json): String = when (this) {
    is HttpException -> {
        val raw = response()?.errorBody()?.string()
        val apiError = raw?.let { runCatching { json.decodeFromString<ApiErrorResponse>(it) }.getOrNull() }
        val backendMessage = apiError?.errors?.values?.firstOrNull() ?: apiError?.message.orEmpty()
        when {
            code() == 401 -> "Неверный логин или пароль"
            code() == 409 && backendMessage.contains("username", ignoreCase = true) -> "Такое имя пользователя уже занято"
            code() == 404 && backendMessage.contains("user", ignoreCase = true) -> "Пользователь с таким именем не найден"
            code() == 409 && backendMessage.contains("friend", ignoreCase = true) -> "Заявка уже отправлена или вы уже друзья"
            code() in 500..599 -> "Сервис временно недоступен. Попробуйте позже."
            backendMessage.isNotBlank() -> "Не удалось выполнить действие. Проверьте данные и попробуйте снова."
            else -> "Не удалось выполнить действие. Попробуйте снова."
        }
    }
    is IOException -> "Не удалось подключиться. Проверьте интернет."
    is IllegalArgumentException -> message ?: "Проверьте введённые данные"
    else -> "Что-то пошло не так. Попробуйте снова."
}
