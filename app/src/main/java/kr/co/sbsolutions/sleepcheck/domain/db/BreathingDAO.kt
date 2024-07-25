package kr.co.sbsolutions.sleepcheck.domain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kr.co.sbsolutions.sleepcheck.data.db.BreathingEntity

@Dao
interface BreathingDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBreathingData(noseRingData: BreathingEntity)

    @Query("SELECT * FROM Breathing where dataId=:dataId order by time ASC")
    fun getBreathingData(dataId: Int): List<BreathingEntity>

    @Query("DELETE FROM Breathing where dataId=:dataId")
    fun removeBreathingByDataId(dataId: Int)

    @Query("DELETE FROM Breathing")
    fun removeBreathingData()
}