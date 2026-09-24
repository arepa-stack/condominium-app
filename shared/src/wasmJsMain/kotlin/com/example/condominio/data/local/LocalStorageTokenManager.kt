package com.example.condominio.data.local

import kotlinx.browser.localStorage

// Bearer token in localStorage: readable by any script on the origin (XSS risk).
// Acceptable while the app renders no third-party HTML; switch to an HttpOnly cookie if the API supports it.
class LocalStorageTokenManager : TokenManager {
    override suspend fun saveToken(token: String) = localStorage.setItem(KEY, token)
    override suspend fun getToken(): String? = localStorage.getItem(KEY)
    override suspend fun clearToken() = localStorage.removeItem(KEY)

    private companion object {
        const val KEY = "jwt_token"
    }
}
