package kr.co.sbsolutions.newsoomirang.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType

@Entity(tableName = "Setting")
data class SettingData(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "sleepType")
    val sleepType: String = SleepType.Breathing.name,
    @ColumnInfo(name = "snoringOnOff")
    val snoringOnOff :Boolean = true,
    @ColumnInfo(name = "snoringVibrationIntensity")
    val snoringVibrationIntensity : Int = 2
)