package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataManager(private val context: Context) {
    companion object {
        private val KEY_IS_FIRST_EXECUTE = booleanPreferencesKey("is_first_execute")
        private val KEY_DATA_ID = intPreferencesKey("data_id")
        private val APP_UPDATE_CHECK = stringPreferencesKey("app_update_check")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val SNS_TYPE = stringPreferencesKey("sns_type")

        private const val ADDRESS = "_address"
        private const val NAME = "_name"
    }

    suspend fun setDataId(dataId: Int) {
        context.dataStore.edit {preferences ->
            preferences[KEY_DATA_ID] = dataId
        }
    }

    suspend fun resetDataId() {
        context.dataStore.edit {preferences->
            preferences.remove(KEY_DATA_ID)
        }
    }

    fun getDataId() : Flow<Int?> {
        return context.dataStore.data.map {preferences->
            preferences[KEY_DATA_ID]
        }
    }

    fun isFirstExecute() : Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IS_FIRST_EXECUTE] ?: true
        }
    }

    suspend fun setFirstExecuted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_EXECUTE] = false
        }
    }

    suspend fun saveBluetoothDevice(key: String, name: String, address: String) : Boolean {
        val pref = context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key + NAME)] = name
            preferences[stringPreferencesKey(key + ADDRESS)] = address
        }
        return !pref[stringPreferencesKey(key + NAME)].isNullOrEmpty()
            && !pref[stringPreferencesKey(key + ADDRESS)].isNullOrEmpty()
    }
    suspend fun deleteBluetoothDevice(key: String) : Boolean {
        val pref = context.dataStore.edit { preferences->
            preferences.remove(stringPreferencesKey(key + NAME))
            preferences.remove(stringPreferencesKey(key + ADDRESS))
        }

        return pref[stringPreferencesKey(key + NAME)].isNullOrEmpty()
            && pref[stringPreferencesKey(key + ADDRESS)].isNullOrEmpty()
    }

    fun getBluetoothDeviceName(key: String) : Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key + NAME)]
        }
    }
    fun getBluetoothDeviceAddress(key: String) : Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key + ADDRESS)]
        }
    }

    suspend fun saveUserName(userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = userName
        }
    }

    fun getUserName() : Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME]
        }
    }

    suspend fun deleteUserName() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_NAME)
        }
    }

    suspend fun saveUpdateVersionCheck(version: String) {
        context.dataStore.edit {preferences ->
            preferences[APP_UPDATE_CHECK] = version
        }
    }

    fun getUpdateVersionCheck() : Flow<String?> {
        return context.dataStore.data.map {preferences->
            preferences[APP_UPDATE_CHECK]
        }
    }
}