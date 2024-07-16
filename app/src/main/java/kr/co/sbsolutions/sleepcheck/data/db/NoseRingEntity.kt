package kr.co.sbsolutions.sleepcheck.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "NoseRing")
data class NoseRingEntity(
    @PrimaryKey
    @ColumnInfo(name = "time")
    val time: String,
    @ColumnInfo(name = "inferenceTime")
    val inferenceTime: String,
    @ColumnInfo(name = "dataId")
    val dataId: Int,
)