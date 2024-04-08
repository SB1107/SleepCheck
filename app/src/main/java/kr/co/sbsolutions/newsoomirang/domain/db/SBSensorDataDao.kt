package kr.co.sbsolutions.newsoomirang.domain.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.soomirang.db.SBSensorData

@Dao
interface SBSensorDataDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSensorData(sbSensorData: SBSensorData): Long

    @Query("DELETE FROM SLEEP_DATA")
    fun deleteSensorDataAll()

    @Query("SELECT * FROM SLEEP_DATA order by `index`  ASC")
    fun getAllSensorDataList(): Flow<List<SBSensorData>>

    @Transaction
    @Query("SELECT * FROM SLEEP_DATA where dataId=:dataId AND `index` >= :min AND `index` <= :max order by `index` ASC")
    fun getSelectedSensorDataListByIndex(dataId: Int, min: Int, max: Int): Flow<List<SBSensorData>>

    @Query("SELECT COUNT(*) FROM SLEEP_DATA where dataId=:dataId AND `index` >= :min AND `index` <= :max order by time ASC")
    fun getSelectedSensorDataListCount(dataId: Int, min: Int, max: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM SLEEP_DATA where dataId=:dataId order by time ASC")
    fun getSelectedSensorDataListCount(dataId: Int): Flow<Int>

    @Transaction
    @Query("SELECT `index` FROM SLEEP_DATA where dataId=:dataId order by `index` DESC LIMIT 1")

    fun getMaxIndex(dataId: Int): Int
    @Transaction
    @Query("SELECT `index` FROM SLEEP_DATA where dataId=:dataId order by `index` ASC LIMIT 1")
    fun getMinIndex(dataId: Int): Int

    @Delete
    fun deleteSensorDataList(list: List<SBSensorData>)

    @Query("DELETE FROM SLEEP_DATA where dataId=:dataId")
    fun deleteSensorDataListByDataId(dataId: Int)

    @Query("DELETE FROM SLEEP_DATA where dataId < :dataId")
    fun deletePastData(dataId: Int)

    @Query("Select * from SLEEP_DATA where `index`=:index order by `index` DESC LIMIT 1")
    fun getSensorDataByIndex(index: Int): SBSensorData?

    @Transaction
    @Query("Select * from SLEEP_DATA where dataId=:dataId")
    fun getSensorDataIdBy(dataId: Int): Flow<List<SBSensorData>>

    @Query("Select * from SLEEP_DATA where dataId=:dataId order by `index` ASC LIMIT 1")
    fun getSensorDataIdByFirst(dataId: Int): Flow<SBSensorData?>

    @Query("Select * from SLEEP_DATA where dataId=:dataId order by `index` DESC LIMIT 1")
    fun getSensorDataIdByLast(dataId: Int): Flow<SBSensorData?>

    @Update
    suspend fun updateSleepData(sleepData: SBSensorData)

}