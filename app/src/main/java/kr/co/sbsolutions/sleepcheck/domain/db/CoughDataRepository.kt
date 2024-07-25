package kr.co.sbsolutions.sleepcheck.domain.db

import kr.co.sbsolutions.sleepcheck.data.db.CoughEntity
import javax.inject.Inject

class CoughDataRepository @Inject constructor(private val dao: CoughDAO) {

    suspend fun insertCoughData(coughData: CoughEntity) {
        dao.insertCoughData(coughData)
    }

    fun getCoughData(dataId: Int): List<CoughEntity> {
        return dao.getCoughData(dataId)
    }

    fun removeCoughData(dataId: Int) {
        dao.removeCoughByDataId(dataId)
    }

    fun removeCoughData(){
        dao.removeCoughData()
    }
}