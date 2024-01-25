package kr.co.sbsolutions.newsoomirang.domain.db

import kr.co.sbsolutions.newsoomirang.data.db.SettingData
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import javax.inject.Inject

class SettingDataRepository @Inject constructor(private val dao: SettingDao) {
    suspend fun insertSettingData(settingData: SettingData) {
        dao.insertSettingData(settingData = settingData)
    }

    suspend fun setSleepType(sleepType: SleepType) {
        var data: SettingData? = dao.getSettingData()
        if (data == null) {
            data = SettingData().copy(sleepType = sleepType.name)
        }
        insertSettingData(data)
    }

    suspend fun getSleepType(): String {
         return  dao.getSleepData() ?: SleepType.Breathing.name
    }

    suspend fun getSnoringOnOff(): Boolean {
        return dao.getSnoringOnOff() ?: true
    }

    suspend fun setSnoringOnOff(value: Boolean) {
        var data: SettingData? = dao.getSettingData()
        if (data == null) {
            data = SettingData().copy(snoringOnOff = value)
        }
        insertSettingData(data)
    }

    suspend fun setSnoringVibrationIntensity(Intensity: Int) {
        var data: SettingData? = dao.getSettingData()
        if (data == null) {
            data = SettingData().copy(snoringVibrationIntensity = Intensity)
        }
        insertSettingData(data)
    }

    suspend fun getSnoringVibrationIntensity(): Int {
        return dao.getSnoringVibrationIntensity() ?: 2
    }

}