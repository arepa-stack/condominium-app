package com.example.condominio.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

class DataStoreTokenManager(
    private val dataStore: DataStore<Preferences>
) : TokenManager {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("jwt_token")
    }

    override suspend fun saveToken(token: String) {
        dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    override suspend fun getToken(): String? {
        return dataStore.data.first()[TOKEN_KEY]
    }

    override suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
        }
    }
}
