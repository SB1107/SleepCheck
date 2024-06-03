package kr.co.sbsolutions.sleepcheck.domain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kr.co.sbsolutions.soomirang.db.LogData

@Dao
interface LogDataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
     fun insertLogData(logData: LogData)
    @Query("SELECT * FROM log_data")
    fun getAllLogDataList() : List<LogData>
}