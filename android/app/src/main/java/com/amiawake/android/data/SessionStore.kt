package com.amiawake.android.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.sessionDataStore by preferencesDataStore("session")

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val sessionId: String = "legacy",
)

class SessionStore(private val context: Context) : TokenStore {
    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val expiresAt = longPreferencesKey("expires_at")
        val sessionId = stringPreferencesKey("session_id")
    }

    val session: Flow<Session?> = context.sessionDataStore.data.map { preferences ->
        val access = preferences[Keys.accessToken]
        val refresh = preferences[Keys.refreshToken]
        if (access == null || refresh == null) null else Session(
            accessToken = access,
            refreshToken = refresh,
            expiresAtEpochSeconds = preferences[Keys.expiresAt] ?: 0L,
            sessionId = preferences[Keys.sessionId] ?: "legacy",
        )
    }

    override suspend fun current(): Session? = session.first()

    suspend fun save(tokens: TokenResponse) {
        context.sessionDataStore.edit {
            it[Keys.accessToken] = tokens.accessToken
            it[Keys.refreshToken] = tokens.refreshToken
            it[Keys.expiresAt] = System.currentTimeMillis() / 1000 + tokens.expiresIn
            it[Keys.sessionId] = UUID.randomUUID().toString()
        }
    }

    override suspend fun saveIfCurrent(expectedRefreshToken: String, tokens: TokenResponse): Boolean {
        var saved = false
        context.sessionDataStore.edit {
            if (it[Keys.refreshToken] == expectedRefreshToken) {
                it[Keys.accessToken] = tokens.accessToken
                it[Keys.refreshToken] = tokens.refreshToken
                it[Keys.expiresAt] = System.currentTimeMillis() / 1000 + tokens.expiresIn
                saved = true
            }
        }
        return saved
    }

    override suspend fun clearIfCurrent(expectedRefreshToken: String) {
        context.sessionDataStore.edit {
            if (it[Keys.refreshToken] == expectedRefreshToken) it.clear()
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
