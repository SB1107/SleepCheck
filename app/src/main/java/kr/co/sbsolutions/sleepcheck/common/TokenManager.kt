package kr.co.sbsolutions.sleepcheck.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TokenManager(private val context: Context) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
        private val KEY_IS_TOKEN_CHECK = booleanPreferencesKey("is_token_check")
    }

    fun getToken(): Flow<String?> {
        return context.tokenStore.data.map { preferences ->
            preferences[KEY_TOKEN]
        }
    }

    suspend fun saveToken(token: String) {
        context.tokenStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
        }
    }

    suspend fun deleteToken() {
        context.tokenStore.edit { preferences ->
            preferences.remove(KEY_TOKEN)
        }
    }

    fun getFcmToken(): Flow<String?> {
        return context.tokenStore.data.map { preferences ->
            preferences[KEY_FCM_TOKEN]
        }
    }

    suspend fun saveFcmToken(fcmToken: String) {
        context.tokenStore.edit { preferences ->
            preferences[KEY_FCM_TOKEN] = fcmToken
        }
    }

    suspend fun deleteFcmToken() {
        context.tokenStore.edit { preferences ->
            preferences.remove(KEY_FCM_TOKEN)
        }
    }

    suspend fun setUpdateFcmToken() {
        context.tokenStore.edit { preferences ->
            preferences[KEY_IS_TOKEN_CHECK] = true
        }
    }

    suspend fun setDifferentValue() {
        context.tokenStore.edit { preferences ->
            preferences[KEY_IS_TOKEN_CHECK] = false
        }
    }

    suspend fun getTokenState(): Flow<Boolean> {
        return context.tokenStore.data.map { preferences ->
            preferences[KEY_IS_TOKEN_CHECK] ?: false
        }
    }
}