package kr.co.sbsolutions.newsoomirang.domain.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kr.co.sbsolutions.soomirang.db.SBSensorData
import javax.inject.Inject

class SBSensorDBRepository @Inject constructor(private val dao: SBSensorDataDao) {
    val listAll = dao.getAllSensorDataList().flowOn(Dispatchers.IO).conflate()

    suspend fun insert(sbSensorData: SBSensorData) : Long {
        return dao.insertSensorData(sbSensorData)
    }

    suspend fun deleteAll() {
        dao.deleteSensorDataAll()
    }

    fun getSelectedSensorDataListByIndex(dataId: Int, min: Int, max: Int) : Flow<List<SBSensorData>> {
        return dao.getSelectedSensorDataListByIndex(dataId, min, max)
    }

    fun getSelectedSensorDataListCount(dataId: Int, min: Int, max: Int) : Flow<Int>{
        return dao.getSelectedSensorDataListCount(dataId, min, max)
    }
    fun getSelectedSensorDataListCount(dataId: Int) : Flow<Int>{
        return dao.getSelectedSensorDataListCount(dataId)
    }

    fun getMaxIndex(dataId: Int) : Int {
        return dao.getMaxIndex(dataId)
    }


    fun getMinIndex(dataId: Int) : Int {
        return dao.getMinIndex(dataId)
    }

    fun deleteRemainList(dataId: Int) {
        return dao.deleteSensorDataListByDataId(dataId)
    }


    fun deleteUploadedList(list: List<SBSensorData>) {
        return dao.deleteSensorDataList(list)
    }

    fun deletePastList(dataId: Int) {
        return dao.deletePastData(dataId)
    }

    fun getSensorDataByIndex(index: Int) : SBSensorData? {
        return dao.getSensorDataByIndex(index)
    }

    fun getSensorDataIdBy(dataId: Int) : Flow<List<SBSensorData>> {
        return  dao.getSensorDataIdBy(dataId)
    }

    fun getSensorDataIdByFirst(dataId: Int) : Flow<SBSensorData?> {
        return  dao.getSensorDataIdByFirst(dataId)
    }
    fun getSensorDataIdByLast(dataId: Int) : Flow<SBSensorData?> {
        return dao.getSensorDataIdByLast(dataId)
    }
    suspend fun updateSleepData(sleepData: SBSensorData) {
            return dao.updateSleepData(sleepData)
    }

    fun getAllSensorDataList() : Flow<List<SBSensorData>> {
        return dao.getAllSensorDataList()
    }



}