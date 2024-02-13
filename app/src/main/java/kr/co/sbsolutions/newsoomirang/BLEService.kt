package kr.co.sbsolutions.newsoomirang

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.*
import com.opencsv.CSVWriter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kr.co.sbsolutions.newsoomirang.common.BluetoothUtils
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.RequestHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
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
import kr.co.sbsolutions.soomirang.db.SBSensorData
import org.tensorflow.lite.support.label.Category
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
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

    val sbSensorInfo by lazy {
        bluetoothNetworkRepository.sbSensorInfo
    }
    val spo2SensorInfo by lazy {
        bluetoothNetworkRepository.spo2SensorInfo
    }
    val eegSensorInfo by lazy {
        bluetoothNetworkRepository.eegSensorInfo
    }

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var tokenManager: TokenManager

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        this.applicationContext?.getSystemService(BluetoothManager::class.java)?.run {
            return@run adapter
        }
    }

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


    lateinit var requestHelper: RequestHelper

    private val mBinder: IBinder = LocalBinder()
    private val errorMessage: MutableSharedFlow<String> = MutableSharedFlow()
    override fun onCreate() {
        super.onCreate()
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

        requestHelper = RequestHelper(lifecycleScope, dataManager = dataManager, tokenManager = tokenManager, errorMessage = errorMessage)
        lifecycleScope.launch(IO) {
            errorMessage.collectLatest {
                if (it.isEmpty().not()) {

                }
            }
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

    private val mFilter = IntentFilter().apply {
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        //addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }

    fun connectDevice(bluetoothInfo: BluetoothInfo) {
//        Log.d(TAG, "getCallback: ConnectDevice ")
        val device = bluetoothAdapter?.getRemoteDevice(bluetoothInfo.bluetoothAddress)

        bluetoothInfo.bluetoothGatt = device?.connectGatt(baseContext, true, bluetoothNetworkRepository.getGattCallback(bluetoothInfo.sbBluetoothDevice))

        timerOfDisconnection?.cancel()
        timerOfDisconnection = Timer().apply {
            schedule(timerTask {
                disconnectDevice(bluetoothInfo)
            }, 5000L)
        }
    }

    fun disconnectDevice(bluetoothInfo: BluetoothInfo) {
        bluetoothInfo.bluetoothGatt?.let { gatt ->
            BluetoothUtils.findResponseCharacteristic(gatt)?.let { char ->
                gatt.setCharacteristicNotification(char, false)
            }
            gatt.disconnect()
            gatt.close()
            bluetoothNetworkRepository.disconnectedDevice(SBBluetoothDevice.SB_SOOM_SENSOR)
        }

        bluetoothInfo.dataId = null
        bluetoothInfo.bluetoothGatt = null
    }

    private fun startTimer() {
        notVibrationNotifyChannelCreate()
        lifecycleScope.launch {
            launch {
                timeHelper.startTimer(this)
            }
//            launch {
//                timeHelper.measuringTimer.collectLatest {
//                    notificationBuilder.setContentText(String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third))
//                    notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build())
//
//                }
//            }
        }
    }

    private fun notVibrationNotifyChannelCreate() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)

    }

    fun stopTimer() {
        timeHelper.stopTimer()
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mReceiver)

        releaseResource()
        cancelJob()
        stopSelf()
    }

    private fun releaseResource() {
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
                            exportLastFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), forceClose, sleepType = it.sleepType, snoreTime = it.snoreTime)
                        }
                    } ?: finishService(-1, forceClose)
                }

            }, TIME_OUT_MEASURE)
        }

    }

    private fun stopScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback(null)
    }

    private fun registerDownloadCallback() {
        bluetoothNetworkRepository.setOnDownloadCompleteCallback {
            sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        Log.d(TAG, "uploading: register")
                        exportFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), it.sleepType, it.snoreTime)
                    }
                }
            }
        }

        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback { state ->
            val forceClose = notifyPowerOff(state)
            sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        exportLastFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), forceClose, sleepType = it.sleepType, snoreTime = it.snoreTime)
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
        when (intent?.action?.let { ActionMessage.getMessage(it) }) {
            ActionMessage.StartSBService -> {
                bluetoothNetworkRepository.insertLog("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 시작")
                noseRingHelper.clearData()
                notificationBuilder.setContentTitle("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 중")
//                notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID,notificationBuilder.build())
//                registerListenSBSensorState()
                listenChannelMessage()
                startScheduler()
                registerDownloadCallback()
                // uploadStart()
                //startNotification()
                try {
                    ServiceCompat.startForeground(
                        this, FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        } else {
                            0
                        },
                    ).apply { startTimer() }
                } catch (e: Exception) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        && e is ForegroundServiceStartNotAllowedException
                    ) {
                        bluetoothNetworkRepository.insertLog("서비스 시작 오류")
                    }
                }

            }

            ActionMessage.StopSBService -> {
                notificationBuilder.setContentTitle("측정 종료")
                bluetoothNetworkRepository.insertLog("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 종료")
                // TODO 1.Cancel Alarm Manager 2.UploadAPI(End)
//                unregisterListenSBSensorState()
                stopScheduler()
                bluetoothNetworkRepository.operateDownloadSbSensor(false)
            }

            ActionMessage.CancelSbService -> {
                bluetoothNetworkRepository.insertLog("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 취소")
                stopScheduler()
                finishService(-1, false)
                lifecycleScope.launch(IO) {
                    sbSensorInfo.value.dataId?.let { sbSensorDBRepository.deletePastList(it) }
                }

            }

            ActionMessage.StopSBServiceForced -> {
//                unregisterListenSBSensorState()
                bluetoothNetworkRepository.insertLog("${if (sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 강제 종료")
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
        return super.onStartCommand(intent, flags, startId)
    }

    private fun forcedFlow() {
        sbSensorInfo.value.let {
            it.bluetoothName?.let { name ->
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        val max = sbSensorDBRepository.getMaxIndex(dataId)
                        val min = sbSensorDBRepository.getMinIndex(dataId)
                        val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)
                        if ((max - min + 1) == size) {
                            exportLastFile(dataId, max, true, sleepType = it.sleepType, snoreTime = it.snoreTime)
                        } else {
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
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    fun startSBSensor(dataId: Int, sleepType: SleepType) {
        lifecycleScope.launch(IO) {
            sbSensorDBRepository.deleteAll()
            bluetoothNetworkRepository.startNetworkSBSensor(dataId, sleepType)
            settingDataRepository.setSleepType(sleepType)
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
        createNotificationChannel()
        stopTimer()

        stopAudioClassification()
//        if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.NoSering) {
//        }

        if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.DisconnectedNotIntent) {
            stopSBServiceForced(isCancel)
            return
        } else {
            bluetoothNetworkRepository.setSBSensorCancel(isCancel)
            bluetoothNetworkRepository.stopNetworkSBSensor((noseRingHelper.getSnoreTime() / 1000) / 60)
        }


//        if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.NoSering) {
//            stopAudioClassification()
//            bluetoothNetworkRepository.stopNetworkSBSensor(noseRingHelper.getSnoreTime())
//        } else {
//            bluetoothNetworkRepository.stopNetworkSBSensor()
//        }

    }

    private fun stopSBServiceForced(isCancel: Boolean = false) {
        stopScheduler()
        if (isCancel) {
            finishService(-1, true)
        } else {
            forcedFlow()
        }
    }

    private fun callVibrationNotifications(Intensity: Int) {
        bluetoothNetworkRepository.callVibrationNotifications(Intensity)
    }


    private fun exportFile(dataId: Int, max: Int, sleepType: SleepType, snoreTime: Long = 0) {
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

            if (size < 1000) {
                Log.d(TAG, "exportFile - data size 1000 미만 $size")
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
            uploading(dataId, file, sbList, sleepType = sleepType, snoreTime = snoreTime)
        }
    }

    private fun exportLastFile(dataId: Int, max: Int, isForcedClose: Boolean, sleepType: SleepType, snoreTime: Long = 0) {
        /*filesDir.listFiles { _, name ->
            name.endsWith(".csv")
        }?.map {
            Log.d(TAG, "delete File: ${it.name}")
            it.delete()
        }*/

        lifecycleScope.launch(IO) {
            val min = sbSensorDBRepository.getMinIndex(dataId)
            val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)

            Log.d(TAG, "exportLastFile - Index From $min~$max = ${max - min + 1} / Data Size : $size")

            if (size < 100) {
                Log.d(TAG, "exportLastFile - data size 1000 미만 : $size")
                finishService(dataId, isForcedClose)
                return@launch
            }

            val sbList = sbSensorDBRepository.getSelectedSensorDataListByIndex(dataId, min, max)
            val time = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(System.currentTimeMillis()))
            val filePath = "$filesDir/${time}($dataId).csv"
            val file = File(filePath)
            Log.d(TAG, "exportLastFile - make Start ${time}.csv")
            FileWriter(file).use { fw ->
                CSVWriter(fw).use { cw ->
//                    cw.writeNext(arrayOf("Index", "Time", "Capacitance", "calcAccX", "calcAccY", "calcAccZ", "accelerationX", "accelerationY", "accelerationZ", "moduleName", "deviceName"))
                    cw.writeNext(arrayOf("Index", "Time", "Capacitance", "calcAccX", "calcAccY", "calcAccZ"))
                    sbList.forEach { data ->
                        cw.writeNext(data.toArray())
                    }
                }
            }
            uploading(dataId, file, sbList, true, isForcedClose, sleepType, snoreTime)
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

        timerOfTimeout?.cancel()
        timerOfTimeout = null

        unregisterDownloadCallback()
//        endMeasure(dataId)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        bluetoothNetworkRepository.endNetworkSBSensor(isForcedClose)
        noseRingHelper.clearData()
    }

    /*private fun endMeasure(dataId: Int) {
        baseRequest({
            if(it != ApiResponse.Loading) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }, object: CoroutinesErrorHandler {
            override fun onError(message: String) { }
        }) {
            apneaUploadRepository.uploadEnd(UploadDataId(dataId))
        }

        *//*baseRequest(_responseEndUpload, object: BaseViewModel.CoroutinesErrorHandler {
            override fun onError(message: String) {
                _responseEndUpload.value = ApiResponse.Failure(ResultError.ErrNetwork)
            }
        }) { apneaUploadRepository.uploadEnd(UploadDataId(dataId)) }*//*
    }*/
    private fun uploading(dataId: Int, file: File, list: List<SBSensorData>, isLast: Boolean = false, isForcedClose: Boolean = false, sleepType: SleepType, snoreTime: Long = 0) {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.insertLog("서버 업로드 시작")
            request(request = { remoteAuthDataSource.postUploading(file = file, dataId = dataId, sleepType = sleepType, snoreTime = snoreTime) }
            ) { uploading(dataId, file, list, isLast, isForcedClose, sleepType, snoreTime) }.flowOn(IO).collectLatest {
                if (isLast) {
                    finishService(dataId, isForcedClose)
                }
                bluetoothNetworkRepository.insertLog("서버 업로드 종료")
                sbSensorDBRepository.deleteUploadedList(list)
                file.delete()
                Intent().also { intent ->
                    intent.setAction(Cons.NOTIFICATION_ACTION)
                    intent.setPackage(baseContext.packageName)
                    sendBroadcast(intent)
                    noseRingHelper.clearData()
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