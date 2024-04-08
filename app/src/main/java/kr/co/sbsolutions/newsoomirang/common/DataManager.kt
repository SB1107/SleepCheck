package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataManager(private val context: Context) {
    companion object {
        private val KEY_IS_FIRST_EXECUTE = booleanPreferencesKey("is_first_execute")
        private val NOSE_RING_TIME = longPreferencesKey("nose_ring_time")
        private val APP_TIMER = intPreferencesKey("app_timer")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val SNS_TYPE = stringPreferencesKey("sns_type")

        private val IS_SENSOR = booleanPreferencesKey("is_sensor")

        private const val ADDRESS = "_address"
        private const val NAME = "_name"

    }



    fun isFirstExecute(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_IS_FIRST_EXECUTE] ?: true
        }
    }

    suspend fun setFirstExecuted() {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_EXECUTE] = false
        }
    }

    suspend fun saveBluetoothDevice(key: String, name: String, address: String): Boolean {
        val pref = context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key + NAME)] = name
            preferences[stringPreferencesKey(key + ADDRESS)] = address
        }
        return !pref[stringPreferencesKey(key + NAME)].isNullOrEmpty()
                && !pref[stringPreferencesKey(key + ADDRESS)].isNullOrEmpty()
    }

    suspend fun deleteBluetoothDevice(key: String): Boolean {
        val pref = context.dataStore.edit { preferences ->
            preferences.remove(stringPreferencesKey(key + NAME))
            preferences.remove(stringPreferencesKey(key + ADDRESS))
        }

        return pref[stringPreferencesKey(key + NAME)].isNullOrEmpty()
                && pref[stringPreferencesKey(key + ADDRESS)].isNullOrEmpty()
    }

    fun getBluetoothDeviceName(key: String): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key + NAME)]
        }
    }

    fun getBluetoothDeviceAddress(key: String): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey(key + ADDRESS)]
        }
    }

    suspend fun saveSNSType(snsType: String) {
        context.dataStore.edit { preferences ->
            preferences[SNS_TYPE] = snsType
        }
    }

    fun getSnsTypeName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[SNS_TYPE]
        }
    }

    suspend fun saveUserName(userName: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = userName
        }
    }

    fun getUserName(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME]
        }
    }

    suspend fun deleteUserName() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_NAME)
        }
    }

    suspend fun setHasSensor(hasSensor: Boolean = true) {
        context.moveStore.edit { preferences ->
            preferences[IS_SENSOR] = hasSensor
        }
    }

    fun getHasSensor(): Flow<Boolean> {
        return context.moveStore.data.map { preferences ->
            preferences[IS_SENSOR] ?: true
        }
    }


    suspend fun setTimer(timer: Int) {
        context.moveStore.edit { preferences ->
            preferences[APP_TIMER] = timer
        }
    }

    fun getTimer(): Flow<Int?> {
        return context.moveStore.data.map { preferences ->
            preferences[APP_TIMER]
        }
    }
    suspend fun setNoseRingTimer(time: Long) {
        context.moveStore.edit { preferences ->
            preferences[NOSE_RING_TIME] = time
        }
    }


    fun getNoseRingTimer(): Flow<Long?> {
        return context.moveStore.data.map { preferences ->
            preferences[NOSE_RING_TIME]
        }
    }
}