package kr.co.sbsolutions.newsoomirang.di

import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDataDao
import kr.co.sbsolutions.soomirang.db.SBSensorDataBase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object  ApplicationModule {
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context) = SBSensorDataBase.getDatabase(context)

    @Provides
    fun provideLogDataDao(db : SBSensorDataBase) = db.logDataDao()
    @Singleton
    @Provides
    fun provideSBSensorDAO(sbSensorDatabase: SBSensorDataBase) : SBSensorDataDao = sbSensorDatabase.sbSensorDAO()

    @Singleton
    @Provides
    fun provideBluetoothAdapter(@ApplicationContext context: Context) = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

}
