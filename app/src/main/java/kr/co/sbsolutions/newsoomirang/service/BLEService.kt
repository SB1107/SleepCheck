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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.RequestHelper
import kr.co.sbsolutions.newsoomirang.common.ServiceLiveCheckWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.bluetooth.FirmwareData
import kr.co.sbsolutions.newsoomirang.data.firebasedb.RealData
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity
import kr.co.sbsolutions.newsoomirang.presenter.splash.SplashActivity
import javax.inject.Inject

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
    lateinit var dataManager: DataManager

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var bluetoothNetworkRepository: IBluetoothNetworkRepository

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
        bleServiceHelper.uploadingFinishForceCloseCallback { forceClose ->
            finishService(forceClose)
        }
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

    fun connectDevice(isForceBleDeviceConnect: Boolean = false) {
        Log.e(TAG, "connectDevice: service", )
        bleServiceHelper.sbConnectDevice(baseContext, bluetoothAdapter, isForceBleDeviceConnect = isForceBleDeviceConnect)
    }

    fun disconnectDevice() {
        bleServiceHelper.sbDisconnectDevice()
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
        bleServiceHelper.startScheduler()
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
        Log.d(TAG, "onStartCommand: 0")
        when (intent?.action?.let { ActionMessage.getMessage(it) }) {

            ActionMessage.StartSBService -> {
                logHelper.insertLog("StartSBService")
                lifecycleScope.launch(IO) {
                    serviceLiveWorkCheck()
                    startSetting()
                    bleServiceHelper.startSBService(baseContext, bluetoothAdapter)
                }
            }

            ActionMessage.StopSBService -> {
                logHelper.insertLog("StopSBService")
                bleServiceHelper.stopSBService()
                serviceLiveCheckWorkerHelper.cancelWork()
            }

            ActionMessage.CancelSbService -> {
                logHelper.insertLog("CancelSbService")
                bleServiceHelper.cancelSbService()
                serviceLiveCheckWorkerHelper.cancelWork()
                finishService(false)
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
                this@BLEService, FOREGROUND_SERVICE_NOTIFICATION_ID, bleServiceHelper.getNotificationBuilder().build(),
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
        serviceLiveCheckWorkerHelper.cancelWork()

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
        bleServiceHelper.getNotificationManager().createNotificationChannel(channel)
    }

    suspend fun checkDataSize(): Flow<Boolean> {
        return bleServiceHelper.checkDataSize()
    }

    fun removeDataId() {
        bleServiceHelper.removeDataId()
    }

    fun startSBSensor(dataId: Int, sleepType: SleepType, hasSensor: Boolean = true) {
        bleServiceHelper.startSBSensor(dataId, sleepType, hasSensor)
    }

    fun noSensorSeringMeasurement(isForce: Boolean = false, isCancel: Boolean = false , callback: () -> Unit) {
        bleServiceHelper.noSensorSeringMeasurement(isForce, isCancel, callback)
        serviceLiveCheckWorkerHelper.cancelWork()
    }

    fun stopSBSensor(isCancel: Boolean = false, callback : () -> Unit) {
        bleServiceHelper.getNotificationManager().getNotificationChannel(NOTIFICATION_CHANNEL_ID).enableVibration(true)
        serviceLiveCheckWorkerHelper.cancelWork()
        bleServiceHelper.stopSBSensor(isCancel, callback)
    }

    fun motorTest(intensity: Int) {
        bleServiceHelper.motorTest(intensity)
    }

    fun forceStopBreathing() {
        bleServiceHelper.getNotificationManager().getNotificationChannel(NOTIFICATION_CHANNEL_ID).enableVibration(true)
        bleServiceHelper.cancelSbService(true)
        serviceLiveCheckWorkerHelper.cancelWork()
        finishService(true)
    }

    fun getRealDataRemoved(): StateFlow<RealData?> {
        return bleServiceHelper.getRealDataRemoved()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return mBinder
    }

    private fun finishService(isForcedClose: Boolean) {
        Log.d(TAG, "finishService: 11")
        bleServiceHelper.finishService(isForcedClose)
        serviceLiveCheckWorkerHelper.cancelWork()
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
        return bleServiceHelper.isBleDeviceConnect()
    }


    fun isForegroundServiceRunning(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.activeNotifications.find { it.id == FOREGROUND_SERVICE_NOTIFICATION_ID } != null
    }

    fun forceDataFlowDataUploadCancel() {
        bleServiceHelper.forceDataFlowDataUploadCancel()
    }

    fun forceDataFlowDataUpload() {
        bleServiceHelper.forceDataFlowDataUpload()
    }

    fun getSbSensorInfo(): StateFlow<BluetoothInfo>? {
        return bleServiceHelper.getSbSensorInfo()
    }

    fun getDataFlowPopUp(): StateFlow<Boolean>? {
        return bleServiceHelper.getDataFlowPopUp()
    }
    fun getUploadFailError(): SharedFlow<String>?{
        return  bleServiceHelper.getUploadFailError()
    }

    fun getTimeHelper(): SharedFlow<Triple<Int, Int, Int>> {
        return bleServiceHelper.getTimeHelper()
    }

    fun getTime(): Int {
        return bleServiceHelper.getTime()
    }

    fun getFirmwareVersion() :Flow<FirmwareData?>{
        return bleServiceHelper.getFirmwareVersion()
    }
}