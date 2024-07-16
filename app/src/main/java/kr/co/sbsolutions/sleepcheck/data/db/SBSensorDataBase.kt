package kr.co.sbsolutions.sleepcheck.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kr.co.sbsolutions.sleepcheck.domain.db.LogDataDao
import kr.co.sbsolutions.sleepcheck.domain.db.NoseRingDAO
import kr.co.sbsolutions.sleepcheck.domain.db.SBSensorDataDao
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDao
import kr.co.sbsolutions.soomirang.db.LogData
import kr.co.sbsolutions.soomirang.db.SBSensorData

@Database(entities = [SBSensorData::class,LogData::class, SettingData::class, NoseRingEntity::class] , version = 2)
abstract class SBSensorDataBase  : RoomDatabase(){
    abstract  fun logDataDao() : LogDataDao
    abstract fun sbSensorDAO() : SBSensorDataDao
    abstract fun settingDao() : SettingDao
    abstract fun noseRingDAO() : NoseRingDAO

    companion object {
        @Volatile
        private  var instance : SBSensorDataBase? = null
        fun getDatabase(context:  Context) : SBSensorDataBase = instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }

        private  val MIGRATION_1_2 = object  :Migration(1,2){
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE NoseRing ( time TEXT PRIMARY KEY NOT NULL , inferenceTime TEXT NOT NULL, dataId INTEGER  NOT NULL DEFAULT -1)")
            }

        }
        private fun buildDatabase(appContext: Context) = Room.databaseBuilder(appContext, SBSensorDataBase::class.java, "sb_sensor.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

}