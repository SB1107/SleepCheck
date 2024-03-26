package kr.co.sbsolutions.newsoomirang.domain.db

import kr.co.sbsolutions.newsoomirang.data.db.SettingData
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import javax.inject.Inject

class SettingDataRepository @Inject constructor(private val dao: SettingDao) {
    suspend fun insertSettingData(settingData: SettingData) {
        dao.insertSettingData(settingData = settingData)
    }

    suspend fun insertDataId(settingData: SettingData) {
        dao.insertDataId(dataId = settingData)
    }

    suspend fun getDataId(): Int {
        return  dao.getDataId() ?: -1
    }



    suspend fun setSleepType(sleepType: SleepType) {
        var data: SettingData? = dao.getSettingData()
        data = data?.copy(sleepType = sleepType.name) ?: SettingData().copy(sleepType = sleepType.name)
        insertSettingData(data)
    }

    suspend fun setDataId(id: Int) {
        var dataId: SettingData? = dao.getSettingData()
        dataId = dataId?.copy(dataId = id) ?: SettingData().copy(-1)
        insertSettingData(dataId)
    }

    suspend fun getSleepType(): String {
         return  dao.getSleepData() ?: SleepType.Breathing.name
    }

    suspend fun getSnoringOnOff(): Boolean {
        return dao.getSnoringOnOff() ?: true
    }

    suspend fun setSnoringOnOff(value: Boolean) {
        var data: SettingData? = dao.getSettingData()
        data = data?.copy(snoringOnOff = value) ?: SettingData().copy(snoringOnOff = value)
        insertSettingData(data)
    }

    suspend fun setSnoringVibrationIntensity(intensity: Int) {
        var data: SettingData? = dao.getSettingData()
        data = data?.copy(snoringVibrationIntensity = intensity) ?: SettingData().copy(snoringVibrationIntensity = intensity)
        insertSettingData(data)
    }

    suspend fun getSnoringVibrationIntensity(): Int {
        return dao.getSnoringVibrationIntensity() ?: 2
    }

}