package kr.co.sbsolutions.newsoomirang.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.co.sbsolutions.newsoomirang.domain.db.LogDataDao
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDataDao
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDao
import kr.co.sbsolutions.soomirang.db.LogData
import kr.co.sbsolutions.soomirang.db.SBSensorData

@Database(entities = [SBSensorData::class, LogData::class, SettingData::class] , version = 2)
abstract class SBSensorDataBase  : RoomDatabase(){
    abstract  fun logDataDao() : LogDataDao
    abstract fun sbSensorDAO() : SBSensorDataDao
    abstract fun settingDao() : SettingDao
    companion object {
        @Volatile
        private  var instance : SBSensorDataBase? = null
        fun getDatabase(context:  Context) : SBSensorDataBase = instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE New_Setting (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, sleepType TEXT NOT NULL, snoringOnOff INTEGER NOT NULL, snoringVibrationIntensity INTEGER NOT NULL, dataId INTEGER)")
                db.execSQL(
                    "INSERT INTO New_Setting (id, sleepType, snoringOnOff, snoringVibrationIntensity, dataId) " +
                            "SELECT id, sleepType, snoringOnOff, snoringVibrationIntensity,null FROM Setting"
                )
                db.execSQL("DROP TABLE Setting")
                db.execSQL("ALTER TABLE New_Setting RENAME TO Setting")

            }

        }

        private fun buildDatabase(appContext: Context) = Room.databaseBuilder(appContext, SBSensorDataBase::class.java, "sb_sensor.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

}