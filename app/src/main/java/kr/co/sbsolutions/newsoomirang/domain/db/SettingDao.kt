package kr.co.sbsolutions.newsoomirang.domain.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kr.co.sbsolutions.newsoomirang.data.db.SettingData
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType

@Dao
interface SettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSettingData(settingData: SettingData)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDataId(dataId: SettingData)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSleepData(sleepType: SleepType){

    }
    @Query("SELECT * FROM Setting  LIMIT 1")
    fun getSettingData() : SettingData?

    @Query("SELECT sleepType FROM Setting  LIMIT 1")
    fun getSleepData() : String?
    @Query("SELECT dataId FROM Setting  LIMIT 1")
    fun getDataId() : Int?

    @Query("SELECT snoringOnOff FROM Setting  LIMIT 1")
    fun getSnoringOnOff(): Boolean?

    @Query("SELECT dataId FROM Setting  LIMIT 1")
    fun getDataId():Int?

    @Query("SELECT snoringVibrationIntensity FROM Setting  LIMIT 1")
    fun getSnoringVibrationIntensity(): Int?

}