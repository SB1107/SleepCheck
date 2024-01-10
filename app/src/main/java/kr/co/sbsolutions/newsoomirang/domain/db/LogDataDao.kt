package kr.co.sbsolutions.newsoomirang.domain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kr.co.sbsolutions.soomirang.db.LogData

@Dao
interface LogDataDao {
    @Insert
     fun insertLogData(logData: LogData)
    @Query("SELECT * FROM log_data")
    fun getAllLogDataList() : List<LogData>
}