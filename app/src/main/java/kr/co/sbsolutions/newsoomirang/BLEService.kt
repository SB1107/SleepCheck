package kr.co.sbsolutions.newsoomirang

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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.RequestHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.UploadWorkerHelper
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
        private const val TIME_OUT_MEASURE: Long = 12 * 60 * 60 * 1000L
        const val UPLOADING: String = "uploading"
        const val FINISH: String = "finish"

    }

    private val audioHelper: AudioClassificationHelper by lazy {
        AudioClassificationHelper(this, object : AudioClassificationHelper.AudioClassificationListener {
            override fun onError(error: String?) {
            }

            override fun onResult(results: List<Category?>?, inferenceTime: Long?) {
                noseRingHelper.noSeringResult(results, inferenceTime)
            }
        })
    }

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
    lateinit var timeHelper: TimeHelper

    @Inject
    lateinit var noseRingHelper: NoseRingHelper

    @Inject
    lateinit var logHelper: LogHelper

    @Inject
    lateinit var uploadWorkerHelper: UploadWorkerHelper

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        this.applicationContext?.getSystemService(BluetoothManager::class.java)?.run {
            return@run adapter
        }
    }

    val sbSensorInfo by lazy {
        bluetoothNetworkRepository.sbSensorInfo
    }
    val spo2SensorInfo by lazy {
        bluetoothNetworkRepository.spo2SensorInfo
    }
    val eegSensorInfo by lazy {
        bluetoothNetworkRepository.eegSensorInfo
    }
    lateinit var requestHelper: RequestHelper

    private val mBinder: IBinder = LocalBinder()

    private val _resultMessage: MutableStateFlow<String?> = MutableStateFlow(null)

    override fun onCreate() {
        super.onCreate()
        logHelper.insertLog("bleOnCreate")

        lifecycleScope.launch(IO) {
            timeHelper.measuringTimer.collectLatest {
                notificationBuilder.setContentText(String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third))
                notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build())
            }
        }

        noseRingHelper.setCallVibrationNotifications {
            lifecycleScope.launch(IO) {
                val onOff = settingDataRepository.getSnoringOnOff()
                if (onOff) {
                    callVibrationNotifications(settingDataRepository.getSnoringVibrationIntensity())
                }
            }
        }
        bluetoothAdapter?.let {
            bluetoothNetworkRepository.changeBluetoothState(it.isEnabled)
        }

        registerReceiver(mReceiver, mFilter)

        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.listenRegisterSBSensor()
        }
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.listenRegisterSpO2Sensor()
        }
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.listenRegisterEEGSensor()
        }
        createNotificationChannel()

        requestHelper = RequestHelper(lifecycleScope, dataManager = dataManager, tokenManager = tokenManager)
            .apply {
                setLogWorkerHelper(logHelper)
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
                            bluetoothNetworkRepository.changeBluetoothState(false)
                        }

                        BluetoothAdapter.STATE_ON -> {
                            bluetoothNetworkRepository.changeBluetoothState(true)
                        }

                        BluetoothAdapter.STATE_TURNING_OFF -> {}
                        BluetoothAdapter.STATE_TURNING_ON -> {}
                    }
                }

                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.e(TAG, "onReceive: ACTION_ACL_CONNECTED")
                    timerOfDisconnection?.cancel()
                    timerOfDisconnection = null
                    bluetoothNetworkRepository.connectedDevice(device)
                    //Log.d(TAG, "[RCV] ACTION_ACL_CONNECTED ${device?.name} / ${device?.address}")
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    //Log.d(TAG, "[RCV] ACTION_ACL_DISCONNECTED ${device?.name} / ${device?.address}")
                }
            }
        }
    }

    fun timerOfDisconnection() {
        timerOfDisconnection?.cancel()
        timerOfDisconnection = null
    }

    private val mFilter = IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        //addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }

    fun connectDevice(bluetoothInfo: BluetoothInfo) {
//        Log.d(TAG, "getCallback: ConnectDevice ")
        val device = bluetoothAdapter?.getRemoteDevice(bluetoothInfo.bluetoothAddress)

        bluetoothInfo.bluetoothGatt = device?.connectGatt(baseContext, true, bluetoothNetworkRepository.getGattCallback(bluetoothInfo.sbBluetoothDevice))
        Log.d(TAG, "getCallback: ${bluetoothInfo.bluetoothGatt} ")
        timerOfDisconnection?.cancel()
        timerOfDisconnection = Timer().apply {
            schedule(timerTask {
                Log.e(TAG, "connectDevice: ")
                logHelper.insertLog("!!재연결중 disconnectDevice")
                lifecycleScope.launch(Dispatchers.Main) {
                    disconnectDevice()
                }
            }, 10000L)
        }
    }

    fun disconnectDevice() {
            bluetoothNetworkRepository.disconnectedDevice(SBBluetoothDevice.SB_SOOM_SENSOR)
            bluetoothNetworkRepository.releaseResource()

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val gattDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        for (device in gattDevices) {
            // BluetoothAdapter 객체를 가져옵니다.

            // BluetoothDevice 객체를 가져옵니다.
            val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)

            // 본딩되어 있지 않으면 본딩을 시작합니다.
            if (bluetoothDevice?.bondState != BluetoothDevice.BOND_BONDED) {
                bluetoothAdapter?.startDiscovery()
                bluetoothDevice?.createBond()
                bluetoothAdapter?.cancelDiscovery()
            }

            Log.i(TAG, "Connected device11: ${device.address}")
            Log.i(TAG, "Connected device11: ${device.bondState}")
        }
    }

    private fun startTimer() {
        notVibrationNotifyChannelCreate()
        lifecycleScope.launch { timeHelper.startTimer(this) }
    }

    private fun notVibrationNotifyChannelCreate() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)

    }

    private fun stopTimer() {
        timeHelper.stopTimer()
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
        bluetoothNetworkRepository.releaseResource()
    }

    /*
    // Job Scheduler 미사용 - Doze Issue
    private val jobScheduler : JobScheduler by lazy {
        getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
    }
    */

    private fun startScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback {
            sbSensorInfo.value.let {
                if (it.bluetoothState == BluetoothState.Connected.ReceivingRealtime) {
                    bluetoothNetworkRepository.operateDownloadSbSensor(true)
                }
            }
        }

        timerOfTimeout?.cancel()
        timerOfTimeout = Timer().apply {
            schedule(timerTask {
                stopSBSensor()
                val forceClose = notifyPowerOff(FinishState.FinishTimeOut)
                sbSensorInfo.value.let {
                    it.dataId?.let { dataId ->
                        lifecycleScope.launch(IO) {
                            uploadWorker(dataId, forceClose, it.sleepType, it.snoreTime)
//                            exportLastFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), forceClose, sleepType = it.sleepType, snoreTime = it.snoreTime)
                        }
                    } ?: finishService(-1, forceClose)
                }

            }, TIME_OUT_MEASURE)
        }

    }

    private suspend fun uploadWorker(dataId: Int, forceClose: Boolean, sleepType: SleepType, snoreTime: Long = 0, isFilePass: Boolean = false) {
        _resultMessage.emit(UPLOADING)
        lifecycleScope.launch(Dispatchers.Main) {
            val sensorName = dataManager.getBluetoothDeviceName(sbSensorInfo.value.sbBluetoothDevice.type.name).first() ?: ""
            uploadWorkerHelper.uploadData(baseContext.packageName, dataId, sleepType = sleepType, snoreTime = snoreTime, sensorName = sensorName, isFilePass = isFilePass)
                .observe(this@BLEService) { workInfo: WorkInfo? ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> {}
                            WorkInfo.State.RUNNING -> {}
                            WorkInfo.State.FAILED -> {
                                lifecycleScope.launch(IO) {
                                    val reason = workInfo.outputData.getString("reason")
                                    logHelper.insertLog("서버 업로드 실패 - ${workInfo.outputData.keyValueMap}")
                                    Log.e(TAG, "서버 업로드 실패 - ${workInfo.outputData.keyValueMap}")
                                    if (reason == null) {
                                        uploadWorker(dataId, forceClose, sleepType, snoreTime, isFilePass)
                                    }
                                }
                            }

                            WorkInfo.State.BLOCKED -> {}
                            WorkInfo.State.CANCELLED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서버 업로드 취소}")
                                }
                            }

                            WorkInfo.State.SUCCEEDED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서버 업로드 종료")
                                    _resultMessage.emit(FINISH)
                                    finishService(dataId, forceClose)
                                }
                            }
                        }
                    }
                }
        }

    }

    private fun stopScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback(null)
    }

    private fun registerDownloadCallback() {
        bluetoothNetworkRepository.setOnDownloadCompleteCallback {
            logHelper.insertLog("setOnDownloadCompleteCallback")
            sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        Log.d(TAG, "uploading: register")
                        logHelper.insertLog("uploading: register")
                        val sensorName = dataManager.getBluetoothDeviceName(sbSensorInfo.value.sbBluetoothDevice.type.name).first() ?: ""
                        exportFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), it.sleepType, it.snoreTime, sensorName)
                    }
                } ?: logHelper.insertLog("sbSensorInfo.dataId is null")
            }
        }

        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback { state ->
            val forceClose = notifyPowerOff(state)
            logHelper.insertLog("LastCallback -> $forceClose")
            logHelper.insertLog("LastCallback -> dataID: ${sbSensorInfo.value.dataId}")
            sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        logHelper.insertLog("uploading: register")
                        _resultMessage.emit(UPLOADING)
                        uploadWorker(dataId, forceClose, it.sleepType, it.snoreTime)
//                        exportLastFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), forceClose, sleepType = it.sleepType, snoreTime = it.snoreTime)
                    }
                } ?: finishService(-1, forceClose)
            }
        }
    }

    private fun notifyPowerOff(state: FinishState): Boolean {
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

    private fun unregisterDownloadCallback() {
        bluetoothNetworkRepository.setOnDownloadCompleteCallback(null)
        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         super.onStartCommand(intent, flags, startId)
        when (intent?.action?.let { ActionMessage.getMessage(it) }) {
            ActionMessage.StartSBService -> {
                lifecycleScope.launch(IO) {
                    val sleepType = settingDataRepository.getSleepType()
                    settingDataRepository.getDataId()?.let {
                        sbSensorInfo.value.dataId = it
                    }
                    sbSensorInfo.value.sleepType = if (sleepType == SleepType.Breathing.name) SleepType.Breathing else SleepType.NoSering
                    logHelper.insertLog("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 시작")
                    notificationBuilder.setContentTitle("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 중")
                    val pendingIntent = PendingIntent.getActivity(
                        this@BLEService, NOTIFICATION_ID, Intent(this@BLEService, SplashActivity::class.java).apply {
                            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("data", sbSensorInfo.value.sleepType.ordinal)
                        }, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        else PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    notificationBuilder.setContentIntent(pendingIntent)

//                notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID,notificationBuilder.build())
//                registerListenSBSensorState()
                    listenChannelMessage()
                    startScheduler()
                    registerDownloadCallback()
                    // uploadStart()
                    //startNotification()
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
            }

            ActionMessage.StopSBService -> {
                val message = "${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 종료"
                notificationBuilder.setContentTitle(message)
                logHelper.insertLog(message)
                // TODO 1.Cancel Alarm Manager 2.UploadAPI(End)
//                unregisterListenSBSensorState()
                stopScheduler()
                bluetoothNetworkRepository.operateDownloadSbSensor(false)
            }

            ActionMessage.CancelSbService -> {
                val message = "${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 취소"
                logHelper.insertLog(message)
                stopScheduler()
                finishService(-1, false)
                lifecycleScope.launch(IO) {
                    sbSensorInfo.value.dataId?.let { sbSensorDBRepository.deletePastList(it) }
                }

            }

            ActionMessage.StopSBServiceForced -> {
//                unregisterListenSBSensorState()
                logHelper.insertLog("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 강제 종료")
                stopSBServiceForced()
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

    private fun forcedFlow() {
        sbSensorInfo.value.let {
            logHelper.insertLog("sbSensorInfo: ${it.bluetoothName}  ${it.dataId}")
            it.bluetoothName?.let { name ->
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        val max = sbSensorDBRepository.getMaxIndex(dataId)
                        val min = sbSensorDBRepository.getMinIndex(dataId)
                        val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)
                        logHelper.insertLog("forcedFlow - Index From $min~$max = ${max - min + 1} / Data Size : $size")
                        if ((max - min + 1) == size) {
                            logHelper.insertLog("(max - min + 1) == size)")
                            _resultMessage.emit(UPLOADING)
                            uploadWorker(dataId, false, it.sleepType, it.snoreTime)
//                            uploadWorkerHelper.uploadData(dataId ,false, sleepType = it.sleepType, snoreTime = it.snoreTime)
//                            exportLastFile(dataId, max, true, sleepType = it.sleepType, snoreTime = it.snoreTime)
                        } else {
                            logHelper.insertLog("(max - min + 1) == size)")
                            finishService(dataId, true)
                        }
                    }
                } ?: {
                    finishService(-1, true)
                }
            } ?: {
                finishService(-1, true)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    suspend fun checkDataSize() = callbackFlow {

        lifecycleScope.launch(IO) {
            if (settingDataRepository.getSleepType() == SleepType.NoSering.name) {
                if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.DisconnectedNotIntent || bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.Unregistered) {
                    send(false)
                    close()
                    return@launch
                }
            }
            sbSensorInfo.value.dataId?.let { dataId ->
                val min = sbSensorDBRepository.getMinIndex(dataId)
                val max = sbSensorDBRepository.getMaxIndex(dataId)
                val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)
                send(size < 1000)
                close()
            } ?: run {
                send(true)
                close()
            }
        }
        awaitClose()
    }

    fun startSBSensor(dataId: Int, sleepType: SleepType) {
        lifecycleScope.launch(IO) {
            sbSensorDBRepository.deleteAll()
            bluetoothNetworkRepository.startNetworkSBSensor(dataId, sleepType)
            settingDataRepository.setSleepTypeAndDataId(sleepType, dataId)
            logHelper.insertLog("CREATE -> dataID: $dataId   sleepType: $sleepType ")
        }
        startTimer()
        audioHelper.startAudioClassification()
//        if (sleepType == SleepType.NoSering) {
//        }
    }

    private fun stopAudioClassification() {
        audioHelper.stopAudioClassification()
    }

    fun stopSBSensor(isCancel: Boolean = false) {
        notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID).enableVibration(true)
        stopTimer()

        stopAudioClassification()
//        if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.NoSering) {
//        }

        logHelper.insertLog("코골이 시간: ${noseRingHelper.getSnoreTime()}   $isCancel")
        Log.d(TAG, "코골이 시간: ${noseRingHelper.getSnoreTime()}")

        if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.DisconnectedNotIntent) {
            logHelper.insertLog("bluetoothState: ${bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState}")
            lifecycleScope.launch(IO) {
                if (settingDataRepository.getSleepType() == SleepType.NoSering.name) {
                    noSering(isCancel)
                } else {
                    stopSBServiceForced(isCancel)
                }
            }
        } else {
            bluetoothNetworkRepository.setSBSensorCancel(isCancel)
            if (sbSensorInfo.value.bluetoothState != BluetoothState.Unregistered) {
                bluetoothNetworkRepository.stopNetworkSBSensor((noseRingHelper.getSnoreTime() / 1000) / 60)
            } else {
                noSering(isCancel)
            }
        }


//        if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.NoSering) {
//            stopAudioClassification()
//            bluetoothNetworkRepository.stopNetworkSBSensor(noseRingHelper.getSnoreTime())
//        } else {
//            bluetoothNetworkRepository.stopNetworkSBSensor()
//        }

    }

    private fun noSering(isCancel: Boolean) {
        if (isCancel.not()) {
            sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        logHelper.insertLog("isCancel.not: ${dataId}")
                        uploadWorker(dataId, false, it.sleepType, (noseRingHelper.getSnoreTime() / 1000) / 60, true)
                    }
                }
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun stopSBServiceForced(isCancel: Boolean = false) {
        logHelper.insertLog("stopSBServiceForced: ${isCancel}")
        stopScheduler()
        if (isCancel) {
            finishService(-1, true)
        } else {
            forcedFlow()
        }
    }

    private fun callVibrationNotifications(intensity: Int) {
        bluetoothNetworkRepository.callVibrationNotifications(intensity)
    }


    private fun exportFile(dataId: Int, max: Int, sleepType: SleepType, snoreTime: Long = 0, sensorName: String) {
        /*filesDir.listFiles { _, name ->
            name.endsWith(".csv")
        }?.map {
            Log.d(TAG, "delete File: ${it.name}")
            it.delete()
        }*/
        lifecycleScope.launch(IO) {
            //val max = sbSensorDBRepository.getMaxIndex(dataId, name)
            val min = sbSensorDBRepository.getMinIndex(dataId)
            val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)

            Log.d(TAG, "exportFile - Index From $min~$max = ${max - min + 1} / Data Size : $size")
            logHelper.insertLog("exportFile - Size : $size")

            if (size < 1000) {
                Log.d(TAG, "exportFile - data size 1000 미만 $size")
                logHelper.insertLog("exportFile -  size 1000 미만 $size")
                return@launch
            }

            val sbList = sbSensorDBRepository.getSelectedSensorDataListByIndex(dataId, min, max)
            val time = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(System.currentTimeMillis()))
            val filePath = "$filesDir/${time}($dataId).csv"
            val file = File(filePath)
            Log.d(TAG, "exportFile - make Start ${time}.csv")
            FileWriter(file).use { fw ->
                CSVWriter(fw).use { cw ->
//                    cw.writeNext(arrayOf("Index", "Time", "Capacitance", "calcAccX", "calcAccY", "calcAccZ", "accelerationX", "accelerationY", "accelerationZ", "moduleName", "deviceName"))
                    cw.writeNext(arrayOf("Index", "Time", "Capacitance", "calcAccX", "calcAccY", "calcAccZ"))
                    sbList.forEach { data ->
                        cw.writeNext(data.toArray())
                    }
                }
            }
            Log.d(TAG, "uploading: exportFile")
            logHelper.insertLog("uploading: exportFile")
            uploading(dataId, file, sbList, sleepType = sleepType, snoreTime = snoreTime, sensorName = sensorName)
        }
    }


    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return mBinder
    }

    private fun finishService(dataId: Int, isForcedClose: Boolean) {
        // TODO Release 주석 해제
        /*lifecycleScope.launch(IO) {
              sbSensorDBRepository.deleteRemainList(dataId)
        }*/
        lifecycleScope.launch(IO) {
            _resultMessage.emit(null)
        }
        timerOfTimeout?.cancel()
        timerOfTimeout = null

        unregisterDownloadCallback()
//        endMeasure(dataId)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        bluetoothNetworkRepository.endNetworkSBSensor(isForcedClose)
        noseRingHelper.clearData()
        logHelper.insertLog("finishService")
    }

    private fun uploading(dataId: Int, file: File?, list: List<SBSensorData>, isLast: Boolean = false, isForcedClose: Boolean = false, sleepType: SleepType, snoreTime: Long = 0, sensorName: String) {

        lifecycleScope.launch(IO) {
            _resultMessage.emit(UPLOADING)
            logHelper.insertLog("서버 업로드 시작")
            Intent().also { intent ->
                intent.setAction(Cons.NOTIFICATION_ACTION)
                intent.setPackage(baseContext.packageName)
                sendBroadcast(intent)
            }
            request(request = { remoteAuthDataSource.postUploading(file = file, dataId = dataId, sleepType = sleepType, snoreTime = snoreTime, sensorName = sensorName) }
            ) { uploading(dataId, file, list, isLast, isForcedClose, sleepType, snoreTime, sensorName) }.flowOn(IO).collectLatest {
                logHelper.insertLog("서버 업로드 종료")
                sbSensorDBRepository.deleteUploadedList(list)
                file?.delete()
                _resultMessage.emit(FINISH)

                if (isLast) {
                    finishService(dataId, isForcedClose)
                }
            }
        }
    }

    private var timerOfDisconnection: Timer? = null
    private var timerOfReconnection: Timer? = null
    private var timerOfTimeout: Timer? = null

    private fun listenChannelMessage() {
        lifecycleScope.launch(IO) {
            withContext(IO) {
                sbSensorInfo.value.channel.consumeEach {
                    sbSensorDBRepository.insert(it)
                }
            }
        }
    }


    inner class LocalBinder : Binder() {
        val service: BLEService
            get() = this@BLEService
    }

    fun getResultMessage(): String? {
        return _resultMessage.value
    }

    private fun cancelJob() {
        requestHelper.netWorkCancel()

    }

    fun isForegroundServiceRunning(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.activeNotifications.find { it.id == FOREGROUND_SERVICE_NOTIFICATION_ID } != null
    }

    private suspend fun <T : BaseEntity> request(request: () -> Flow<ApiResponse<T>>, errorHandler: RequestHelper.CoroutinesErrorHandler): Flow<T> {
        return requestHelper.request(request, errorHandler)
    }

}