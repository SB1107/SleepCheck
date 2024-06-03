package kr.co.sbsolutions.soomirang.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_data")
data class LogData(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo("log") val log: String
)