package kr.co.sbsolutions.sleepcheck.domain.db

import kr.co.sbsolutions.sleepcheck.data.db.BreathingEntity
import javax.inject.Inject

class BreathingDataRepository @Inject constructor(private val dao: BreathingDAO) {

    suspend fun insertBreathingData(coughData: BreathingEntity) {
        dao.insertBreathingData(coughData)
    }

    fun getBreathingData(dataId: Int): List<BreathingEntity> {
        return dao.getBreathingData(dataId)
    }

    fun removeBreathingData(dataId: Int) {
        dao.removeBreathingByDataId(dataId)
    }

    fun removeBreathingData(){
        dao.removeBreathingData()
    }
}