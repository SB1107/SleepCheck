package kr.co.sbsolutions.newsoomirang.common

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
        return context.dataStore.data.map { preferences ->
            preferences[KEY_TOKEN]
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_TOKEN] = token
        }
    }

    suspend fun deleteToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_TOKEN)
        }
    }

    fun getFcmToken() : Flow<String?> {
        return context.dataStore.data.map {preferences->
            preferences[KEY_FCM_TOKEN]
        }
    }
    suspend fun saveFcmToken(fcmToken: String){
        context.dataStore.edit {preferences ->
            preferences[KEY_FCM_TOKEN] = fcmToken
        }
    }

    suspend fun deleteFcmToken(){
        context.dataStore.edit {preferences->
            preferences.remove(KEY_FCM_TOKEN)
        }
    }
    fun isFirstAndEqualFcmToken() : Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IS_TOKEN_CHECK] ?: true
        }
    }

    suspend fun setDifferentValue(){
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_TOKEN_CHECK] = false
        }
    }

    fun getFcmTokenState() {
        context.dataStore.data.map{ preferences ->
            preferences[KEY_IS_TOKEN_CHECK]
        }
    }

}