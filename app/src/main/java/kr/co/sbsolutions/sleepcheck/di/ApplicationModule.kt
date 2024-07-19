package kr.co.sbsolutions.sleepcheck.di

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.sleepcheck.BuildConfig
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.AESHelper
import kr.co.sbsolutions.sleepcheck.common.BlueToothScanHelper
import kr.co.sbsolutions.sleepcheck.common.Cons
import kr.co.sbsolutions.sleepcheck.common.CoroutineScopeHandler
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.LogHelper
import kr.co.sbsolutions.sleepcheck.common.LogWorkerHelper
import kr.co.sbsolutions.sleepcheck.common.NoseRingHelper
import kr.co.sbsolutions.sleepcheck.common.ServiceLiveCheckWorkerHelper
import kr.co.sbsolutions.sleepcheck.common.TimeHelper
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.common.UploadWorkerHelper
import kr.co.sbsolutions.sleepcheck.data.db.SBSensorDataBase
import kr.co.sbsolutions.sleepcheck.data.firebasedb.FireBaseRealRepository
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.sleepcheck.domain.db.BreathingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.CoughDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.NoseRingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.sleepcheck.domain.db.SBSensorDataDao
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDao
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.firmware.FirmwareHelper
import kr.co.sbsolutions.sleepcheck.service.BLEServiceHelper
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
    fun provideNoseRingDAO(db: SBSensorDataBase) = db.noseRingDAO()

    @Provides
    fun provideCoughDAO(db: SBSensorDataBase) = db.coughDAO()

    @Provides
    fun provideBreathingDAO(db: SBSensorDataBase) = db.breathingDAO()

    @Provides
    fun provideFirebaseFireStore() = FirebaseFirestore.getInstance()

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)

    @Provides
    fun provideLogWorkHelper(workManager: WorkManager) = LogWorkerHelper(workManager)

    @Provides
    fun provideUploadWorkHelper(workManager: WorkManager) = UploadWorkerHelper(workManager)

    @Provides
    fun provideServiceLiveWorkHelper(workManager: WorkManager) = ServiceLiveCheckWorkerHelper(workManager)

    @Provides
    fun provideTimeId() = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})  " + SimpleDateFormat("yy-MM-dd ", Locale.KOREA).format(System.currentTimeMillis())

    @Provides
    fun provideAESHelper() = AESHelper()

    @Provides
    fun provideTimeHelperManager(logHelper: LogHelper) = TimeHelper(logHelper)

    @Provides
    fun provideNoseRingManager() = NoseRingHelper()

    @Provides
    fun provideCoroutineScopeHandler() = CoroutineScopeHandler()

    @Provides
    fun provideRealDataBaseDB() = Firebase.database

    @Provides
    fun provideRealDatabaseRepository(realDatabase: FirebaseDatabase, logHelper: LogHelper) = FireBaseRealRepository(realDatabase, logHelper = logHelper)

    @Provides
    fun provideBlueToothScanHelper(@ApplicationContext context: Context) = BlueToothScanHelper(context)

    @Provides
    fun provideFirmwareHelper(remoteAuthDataSource: RemoteAuthDataSource, dataManager: DataManager, tokenManager: TokenManager) = FirmwareHelper(remoteAuthDataSource, dataManager, tokenManager)

    @Provides
    fun provideBLEServiceHelper(
        dataManager: DataManager, tokenManager: TokenManager,
        bluetoothNetworkRepository: IBluetoothNetworkRepository,
        sbSensorDBRepository: SBSensorDBRepository,
        settingDataRepository: SettingDataRepository,
        timeHelper: TimeHelper,
        noseRingHelper: NoseRingHelper,
        logHelper: LogHelper,
        uploadWorkerHelper: UploadWorkerHelper,
        notificationBuilder: NotificationCompat.Builder,
        notificationManager: NotificationManager,
        fireBaseRealRepository: FireBaseRealRepository,
        blueToothScanHelper: BlueToothScanHelper,
        noseRingDataRepository: NoseRingDataRepository,
        coughDataRepository: CoughDataRepository,
        breathingDataRepository: BreathingDataRepository
    ) = BLEServiceHelper(
        dataManager, tokenManager, bluetoothNetworkRepository, sbSensorDBRepository,
        settingDataRepository, timeHelper, noseRingHelper, logHelper, uploadWorkerHelper, fireBaseRealRepository,
        notificationBuilder, notificationManager, blueToothScanHelper,noseRingDataRepository,coughDataRepository,
        breathingDataRepository
    )

    @Provides
    fun provideLogHelper(logWorkerHelper: LogWorkerHelper, coroutineScopeHandler: CoroutineScopeHandler) = LogHelper(logWorkerHelper, coroutineScopeHandler)

    @Provides
    fun provideNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, Cons.NOTIFICATION_CHANNEL_ID).setAutoCancel(true).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher) // TODO: Notification 아이콘 작업
        .setContentTitle("숨이랑 기기 연결 대기 중").setPriority(NotificationCompat.PRIORITY_HIGH)

    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
