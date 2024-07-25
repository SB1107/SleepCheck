package kr.co.sbsolutions.sleepcheck.domain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kr.co.sbsolutions.sleepcheck.data.db.NoseRingEntity

@Dao
interface NoseRingDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNoseRingData(noseRingData: NoseRingEntity)

    @Query("SELECT * FROM NoseRing where dataId=:dataId order by time ASC")
    fun getNoseRingData(dataId: Int): List<NoseRingEntity>

    @Query("DELETE FROM NoseRing where dataId=:dataId")
    fun removeNoseRingDataByDataId(dataId: Int)

    @Query("DELETE FROM NoseRing")
    fun removeNoseRingData()
}

