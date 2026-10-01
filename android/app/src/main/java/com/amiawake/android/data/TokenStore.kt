package com.amiawake.android.data

interface TokenStore {
    suspend fun current(): Session?
    suspend fun saveIfCurrent(expectedRefreshToken: String, tokens: TokenResponse): Boolean
    suspend fun clearIfCurrent(expectedRefreshToken: String)
}
