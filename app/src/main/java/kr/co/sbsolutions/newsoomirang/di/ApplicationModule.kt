package kr.co.sbsolutions.newsoomirang.di

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.AESHelper
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.LogWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.common.UploadWorker
import kr.co.sbsolutions.newsoomirang.common.UploadWorkerHelper
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
    fun provideUploadWorkHelper(workManager: WorkManager) = UploadWorkerHelper(workManager)

    @Provides
    fun provideTimeId() = "${BuildConfig.VERSION_NAME}  " + SimpleDateFormat("yy-MM-dd ", Locale.KOREA).format(System.currentTimeMillis())

    @Provides
    fun provideAESHelper() = AESHelper()

    @Provides
    fun provideTimeHelperManager() = TimeHelper()

    @Provides
    fun provideNoseRingManager() = NoseRingHelper()

    @Provides
    fun provideNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, Cons.NOTIFICATION_CHANNEL_ID).setAutoCancel(true).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher) // TODO: Notification 아이콘 작업
        .setContentTitle("숨이랑 기기 연결 대기 중").setPriority(NotificationCompat.PRIORITY_HIGH)
    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
