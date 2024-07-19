package kr.co.sbsolutions.sleepcheck.domain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kr.co.sbsolutions.sleepcheck.data.db.CoughEntity

@Dao
interface CoughDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCoughData(noseRingData: CoughEntity)

    @Query("SELECT * FROM Cough where dataId=:dataId order by time ASC")
    fun getCoughData(dataId: Int): List<CoughEntity>

    @Query("DELETE FROM Cough where dataId=:dataId")
    fun removeCoughByDataId(dataId: Int)

    @Query("DELETE FROM Cough")
    fun removeCoughData()
}