package kr.co.sbsolutions.newsoomirang.domain.db

import kr.co.sbsolutions.newsoomirang.data.db.SettingData
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import javax.inject.Inject

class SettingDataRepository @Inject constructor(private val dao: SettingDao) {
    suspend fun insertSettingData(settingData: SettingData) {
        dao.insertSettingData(settingData = settingData)
    }

    suspend fun setSleepTypeAndDataId(sleepType: SleepType, dataId: Int) {
        var data: SettingData? = dao.getSettingData()
        data = data?.copy(sleepType = sleepType.name, dataId = dataId) ?: SettingData().copy(sleepType = sleepType.name, dataId = dataId)
        insertSettingData(data)
    }
    suspend fun setSleepType(sleepType: SleepType) {
        var data: SettingData? = dao.getSettingData()
        data = data?.copy(sleepType = sleepType.name) ?: SettingData().copy(sleepType = sleepType.name)
        insertSettingData(data)
    }
    suspend fun setDataId(dataId: Int?){
        var data: SettingData? = dao.getSettingData()
        data = data?.copy(dataId = dataId) ?: SettingData().copy(dataId = dataId)
        insertSettingData(data)
    }

    suspend fun getSleepType(): String {
         return  dao.getSleepData() ?: SleepType.Breathing.name
    }
    suspend fun getDataId() : Int? {
        return  dao.getDataId()
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
        return dao.getSnoringVibrationIntensity() ?: 0
    }

}