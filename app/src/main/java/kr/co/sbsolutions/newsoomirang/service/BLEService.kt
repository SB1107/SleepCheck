package kr.co.sbsolutions.newsoomirang.service

import android.annotation.SuppressLint
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import com.opencsv.CSVWriter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.MINIMUM_UPLOAD_NUMBER
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataFlowLogHelper
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.RequestHelper
import kr.co.sbsolutions.newsoomirang.common.ServiceLiveCheckWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.UploadData
import kr.co.sbsolutions.newsoomirang.common.UploadWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.pattern.DataFlowHelper
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.audio.AudioClassificationHelper
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity
import kr.co.sbsolutions.newsoomirang.presenter.splash.SplashActivity
import kr.co.sbsolutions.soomirang.db.SBSensorData
import org.tensorflow.lite.support.label.Category
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.timerTask

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class BLEService : LifecycleService() {

    sealed interface FinishState {
        object FinishNormal : FinishState
        object FinishPowerOff : FinishState
        object FinishBatteryLow : FinishState
        object FinishTimeOut : FinishState
    }

    companion object {
        const val DATA_ID = "dataIDSBSensor"

        const val FOREGROUND_SERVICE_NOTIFICATION_ID = 1
        const val POWER_OFF_NOTIFICATION_ID = 1001
        const val TIME_OUT_NOTIFICATION_ID = 1002
        private const val INTERVAL_UPLOAD_TIME = 15L
        const val TIME_OUT_MEASURE: Long = 12 * 60 * 60 * 1000L
        const val UPLOADING: String = "uploading"
        const val FINISH: String = "finish"
        const val MAX_RETRY = 3
        private var instance: BLEService? = null
        fun getInstance(): BLEService? {
            return instance
        }
    }


    @Inject
    lateinit var bleServiceHelper: BLEServiceHelper

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var bluetoothNetworkRepository: IBluetoothNetworkRepository

    @Inject
    lateinit var remoteAuthDataSource: RemoteAuthDataSource

    @Inject
    lateinit var sbSensorDBRepository: SBSensorDBRepository

    @Inject
    lateinit var settingDataRepository: SettingDataRepository

    @Inject
    lateinit var logHelper: LogHelper


    @Inject
    lateinit var serviceLiveCheckWorkerHelper: ServiceLiveCheckWorkerHelper


    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        this.applicationContext?.getSystemService(BluetoothManager::class.java)?.run {
            return@run adapter
        }
    }
    private lateinit var requestHelper: RequestHelper

    private val mBinder: IBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        instance = this
        logHelper.insertLog("bleOnCreate")
        bleServiceHelper.setLifecycleScope(this@BLEService, this.lifecycleScope, this@BLEService, baseContext.packageName)
        bluetoothAdapter?.let {
            bleServiceHelper.blueToothState(bluetoothAdapter?.isEnabled ?: false)
        }
        registerReceiver(mReceiver, mFilter)
        createNotificationChannel()
        requestHelper =
            RequestHelper(lifecycleScope, dataManager = dataManager, tokenManager = tokenManager)
                .apply {
                    setLogWorkerHelper(logHelper)
                }
        setDataFlowFinish()
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            bleServiceHelper.blueToothState(false)
                        }

                        BluetoothAdapter.STATE_ON -> {
                            bleServiceHelper.blueToothState(true)
                        }

                        BluetoothAdapter.STATE_TURNING_OFF -> {}
                        BluetoothAdapter.STATE_TURNING_ON -> {}
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    bleServiceHelper.blueToothConnectedDevice(device)
                    logHelper.insertLog("onReceive: ACTION_ACL_CONNECTED")
                    //Log.d(TAG, "[RCV] ACTION_ACL_CONNECTED ${device?.name} / ${device?.address}")
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    //Log.d(TAG, "[RCV] ACTION_ACL_DISCONNECTED ${device?.name} / ${device?.address}")
                }
            }
        }
    }

    private val mFilter = IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
//        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }

    fun connectDevice(bluetoothInfo: BluetoothInfo) {
        bleServiceHelper.sbConnectDevice(baseContext, bluetoothAdapter)

    }

    fun disconnectDevice() {
        bleServiceHelper.sbDisconnectDevice()
    }

    private fun startTimer() {
        bleServiceHelper.startTimer()
    }


    private fun stopTimer() {
        bleServiceHelper.stopTimer()
    }

    override fun onDestroy() {
        unregisterReceiver(mReceiver)
        Log.e(TAG, "onDestroy: ")
        releaseResource()
        cancelJob()
        stopSelf()
        logHelper.insertLog("BLEService onDestroy")
        super.onDestroy()
    }

    private fun releaseResource() {
        Log.d(TAG, "Serivce releaseResource: ")
        bleServiceHelper.releaseResource()
    }

    private fun startScheduler() {
        bleServiceHelper.startScheduler(notifyPowerOff(FinishState.FinishTimeOut))
    }


    private fun registerDownloadCallback() {
//        bluetoothNetworkRepository.setOnDownloadCompleteCallback {
//            logHelper.insertLog("setOnDownloadCompleteCallback")
//            sbSensorInfo.value.let {
//                it.dataId?.let { dataId ->
//                    lifecycleScope.launch(IO) {
//                        Log.d(TAG, "uploading: register")
//                        logHelper.insertLog("uploading: register")
//                        val sensorName = dataManager.getBluetoothDeviceName(sbSensorInfo.value.sbBluetoothDevice.type.name).first() ?: ""
//                        exportFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), it.sleepType, it.snoreTime, sensorName)
//                    }
//                } ?: logHelper.insertLog("sbSensorInfo.dataId is null")
//            }
//        }
        bleServiceHelper.registerDownloadCallback()
    }

    fun notifyPowerOff(state: FinishState): Boolean {
        val param = when (state) {
            FinishState.FinishBatteryLow -> Pair(POWER_OFF_NOTIFICATION_ID, getString(R.string.device_power_off_battery_low))
            FinishState.FinishPowerOff -> Pair(POWER_OFF_NOTIFICATION_ID, getString(R.string.device_power_off_force_off))
            FinishState.FinishTimeOut -> Pair(TIME_OUT_NOTIFICATION_ID, getString(R.string.device_measure_timeout))
            FinishState.FinishNormal -> return false
        }
        NotificationCompat.Builder(this, getString(R.string.notification_channel_id)).apply {
            setSmallIcon(R.mipmap.ic_launcher)
            setContentTitle(getString(R.string.device_power_off_title))
            setContentText(param.second)
            priority = NotificationCompat.PRIORITY_LOW
            setCategory(Notification.CATEGORY_ERROR)
            setAutoCancel(true)
            setContentIntent(
                PendingIntent.getActivity(
                    this@BLEService,
                    param.first,
                    Intent(this@BLEService, MainActivity::class.java),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    else PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            setStyle(NotificationCompat.BigTextStyle().bigText(param.second))
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(param.first, build())
        }
        return true
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action?.let { ActionMessage.getMessage(it) }) {

            ActionMessage.StartSBService -> {
                lifecycleScope.launch(IO) {
                    serviceLiveWorkCheck()
                    bleServiceHelper.startSBService(baseContext, bluetoothAdapter)
                    startSetting()
                }
            }

            ActionMessage.StopSBService -> {
                bleServiceHelper.stopSBService()
                serviceLiveCheckWorkerHelper.cancelWork()
            }

            ActionMessage.CancelSbService -> {
                bleServiceHelper.cancelSbService()
                serviceLiveCheckWorkerHelper.cancelWork()
                finishService(-1, false)
            }

            ActionMessage.StopSBServiceForced -> {
                bleServiceHelper.stopSBServiceForced()
            }

            ActionMessage.OperateRealtimeSBSensor -> {
                bluetoothNetworkRepository.operateRealtimeSBSensor()
            }

            ActionMessage.OperateDelayedSBSensor -> {
                bluetoothNetworkRepository.operateDelayedSBSensor()
            }

            ActionMessage.OperateDownloadSBSensor -> {
                bluetoothNetworkRepository.operateDownloadSbSensor(false)
            }

            ActionMessage.OperateDeleteSectorSBSensor -> {
                bluetoothNetworkRepository.operateDeleteSbSensor(false)
            }

            ActionMessage.OperateDeleteAllSBSensor -> {
                bluetoothNetworkRepository.operateDeleteSbSensor(true)
            }
            /*
            // Job Scheduler 미사용 - Doze Issue
            ActionMessage.UploadToServerSBSensor -> { processUpload() }
            */
            null -> {}
        }
        return START_REDELIVER_INTENT
    }

    private fun startSetting() {
        val pendingIntent = PendingIntent.getActivity(
            this@BLEService, NOTIFICATION_ID, Intent(this@BLEService, SplashActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("data", bleServiceHelper.getSleepType().ordinal)
            }, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        bleServiceHelper.setContentIntent(pendingIntent)
        startScheduler()
        registerDownloadCallback()
        try {
            ServiceCompat.startForeground(
                this@BLEService, FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                },
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                logHelper.insertLog("서비스 시작 오류")
            }
        }
    }

    private fun serviceLiveWorkCheck() {
        lifecycleScope.launch(Dispatchers.Main) {
            serviceLiveCheckWorkerHelper.serviceLiveCheck()
                .observe(this@BLEService) { workInfo: WorkInfo? ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> {
                                Log.e(TAG, "serviceLiveWorkCheck: ENQUEUED")
                            }

                            WorkInfo.State.RUNNING -> {
                                Log.e(TAG, "serviceLiveWorkCheck: RUNNING")
                            }

                            WorkInfo.State.FAILED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서비스 살아있는지 확인 워커 - ${workInfo.outputData.keyValueMap}")
                                }
                            }

                            WorkInfo.State.BLOCKED, WorkInfo.State.SUCCEEDED -> {
                                Log.e(TAG, "serviceLiveWorkCheck: SUCCEEDED")
                            }

                            WorkInfo.State.CANCELLED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서비스 살아있는지 확인 취소 - ${workInfo.outputData.keyValueMap}")
                                }
                            }
                        }
                    }
                }
        }
    }


    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun checkDataSize(): Flow<Boolean> {
        return bleServiceHelper.checkDataSize()
    }

    fun waitStart() {
        bleServiceHelper.waitStart()
    }

    fun startSBSensor(dataId: Int, sleepType: SleepType, hasSensor: Boolean = true) {
        bleServiceHelper.startSBSensor(dataId, sleepType, hasSensor)
    }

    private fun stopAudioClassification() {
        bleServiceHelper.stopAudioClassification()
    }

    fun finishSenor() {
        bleServiceHelper.finishSenor()
    }

    fun noSensorSeringMeasurement(isCancel: Boolean = false) {
        bleServiceHelper.noSensorSeringMeasurement(isCancel)
    }

    fun stopSBSensor(isCancel: Boolean = false) {
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).enableVibration(true)
        serviceLiveCheckWorkerHelper.cancelWork()
        bleServiceHelper.stopSBSensor(isCancel)

    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return mBinder
    }

    private fun finishService(dataId: Int, isForcedClose: Boolean) {
        bleServiceHelper.finishService(isForcedClose)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    inner class LocalBinder : Binder() {
        val service: BLEService
            get() = this@BLEService
    }

    fun getResultMessage(): String? {
        return bleServiceHelper.getResultMessage()
    }

    private fun cancelJob() {
        requestHelper.netWorkCancel()

    }

    fun isBleDeviceConnect(): Pair<Boolean, String> {
        return bluetoothNetworkRepository.isSBSensorConnect()
    }


    fun isForegroundServiceRunning(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.activeNotifications.find { it.id == FOREGROUND_SERVICE_NOTIFICATION_ID } != null
    }

    private suspend fun <T : BaseEntity> request(request: () -> Flow<ApiResponse<T>>, errorHandler: RequestHelper.CoroutinesErrorHandler): Flow<T> {
        return requestHelper.request(request, errorHandler)
    }

    fun forceDataFlowDataUploadCancel() {
        bleServiceHelper.forceDataFlowDataUploadCancel()
    }

    fun forceDataFlowDataUpload() {
        bleServiceHelper.forceDataFlowDataUpload()
    }

    private fun setDataFlowFinish() {
        serviceLiveCheckWorkerHelper.cancelWork()
        bleServiceHelper.uploadingFinishForceCloseCallback { forceClose ->
            finishService(-1, forceClose)
        }
    }

    fun getSbSensorInfo(): StateFlow<BluetoothInfo> {
        return bleServiceHelper.getSbSensorInfo()
    }

    fun getDataFlowPopUp(): StateFlow<Boolean> {
        return bleServiceHelper.getDataFlowPopUp()
    }

    fun getTimeHelper(): SharedFlow<Triple<Int, Int, Int>> {
        return bleServiceHelper.getTimeHelper()
    }

    fun getTime(): Int {
        return bleServiceHelper.getTime()
    }
}