package kr.co.sbsolutions.soomirang.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SLEEP_DATA" )
data class SBSensorData(
    @PrimaryKey
    @ColumnInfo(name = "id" ) val index: Int = 0,
    @ColumnInfo(name = "time") var time: String,
    @ColumnInfo(name = "capacitance") val capacitance: Int,
    @ColumnInfo(name = "calcAccX") val calcAccX: String,
    @ColumnInfo(name = "calcAccY") val calcAccY: String,
    @ColumnInfo(name = "calcAccZ") val calcAccZ: String,
    @ColumnInfo(name = "dataId") val dataId: Int,
){
    fun toArray() : Array<String>{
        return arrayOf(index.toString(), "$time", capacitance.toString(), calcAccX, calcAccY, calcAccZ,dataId.toString())
    }
}
