package kr.co.sbsolutions.newsoomirang.domain.db

import kr.co.sbsolutions.soomirang.db.LogData
import javax.inject.Inject

class LogDBDataRepository@Inject constructor(private val dao: LogDataDao) {

    suspend fun insertLogData(logData: LogData) {
        return dao.insertLogData(logData)
    }

    suspend fun logDeleteAll() {
        dao.getAllLogDataList()
    }
}