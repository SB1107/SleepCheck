package kr.co.sbsolutions.sleepcheck.domain.db

import kr.co.sbsolutions.sleepcheck.data.db.NoseRingEntity
import javax.inject.Inject

class NoseRingDataRepository @Inject constructor(private val dao: NoseRingDAO) {
    suspend fun insertNoseRingData(noseRingData: NoseRingEntity) {
        dao.insertNoseRingData(noseRingData)
    }

    fun getNoseRingData(dataId: Int): List<NoseRingEntity> {
        return dao.getNoseRingData(dataId)
    }

    fun removeNoseRingData(dataId: Int) {
        dao.removeNoseRingDataByDataId(dataId)
    }

    fun removeNoseRingData(){
        dao.removeNoseRingData()
    }
}