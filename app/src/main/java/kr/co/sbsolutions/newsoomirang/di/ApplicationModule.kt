package kr.co.sbsolutions.newsoomirang.di

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.common.AESHelper
import kr.co.sbsolutions.newsoomirang.common.LogWorker
import kr.co.sbsolutions.newsoomirang.common.LogWorkerHelper
import kr.co.sbsolutions.newsoomirang.data.db.SBSensorDataBase
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDataDao
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDao
import java.text.SimpleDateFormat
import java.util.Locale

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context) = SBSensorDataBase.getDatabase(context)

    @Provides
    fun provideLogDataDao(db: SBSensorDataBase) = db.logDataDao()

    @Provides
    fun provideSBSensorDAO(sbSensorDatabase: SBSensorDataBase): SBSensorDataDao = sbSensorDatabase.sbSensorDAO()

    @Provides
    fun provideSettingDAO(db: SBSensorDataBase): SettingDao = db.settingDao()

    @Provides
    fun provideBluetoothAdapter(@ApplicationContext context: Context) = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    @Provides
    fun provideFirebaseFireStore() = FirebaseFirestore.getInstance()

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)

    @Provides
    fun provideLogWorkHelper(workManager: WorkManager) = LogWorkerHelper(workManager)

    @Provides
    fun provideTimeId() = "앱 버전 : ${BuildConfig.VERSION_NAME}  " + SimpleDateFormat("yy-MM-dd ", Locale.KOREA).format(System.currentTimeMillis())

    @Provides
    fun provideAESHelper() = AESHelper()
}
