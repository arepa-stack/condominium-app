package com.example.condominio.data.local

/** Persists the JWT. Android: DataStore; web: localStorage. */
interface TokenManager {
    suspend fun saveToken(token: String)
    suspend fun getToken(): String?
    suspend fun clearToken()
}
