package kr.co.sbsolutions.newsoomirang

import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.opencsv.CSVWriter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kr.co.sbsolutions.newsoomirang.common.BluetoothUtils
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.db.LogDBDataRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity
import kr.co.sbsolutions.soomirang.db.SBSensorData
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.domain.bluetooth.repository.IBluetoothNetworkRepository
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
        private var instance: BLEService? = null
        fun getInstance(): BLEService? {
            return instance
        }

        var isServiceStarted: Boolean = false
        private val _sbSensorInfo: MutableStateFlow<BluetoothInfo> = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR))
//        private val _sbSensorInfShard: MutableSharedFlow<BluetoothInfo> = MutableSharedFlow(replay = 1, extraBufferCapacity = 1)
        val sbSensorInfo:  SharedFlow<BluetoothInfo> = _sbSensorInfo

//        private  val  _spo2SensorInfo : MutableStateFlow<BluetoothInfo> = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SPO2_SENSOR))
//        val spo2SensorInfo :StateFlow<BluetoothInfo> = _spo2SensorInfo
//        private  val  _eegSensorInfo : MutableStateFlow<BluetoothInfo> = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_EEG_SENSOR))
//        val eegSensorInfo :StateFlow<BluetoothInfo> = _eegSensorInfo
    }
    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var dataManager: DataManager

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    @Inject
    lateinit var bluetoothNetworkRepository: IBluetoothNetworkRepository

//    @Inject
//    lateinit var apneaUploadRepository: IApneaUploadRepository

    @Inject
    lateinit var sbSensorDBRepository: SBSensorDBRepository

    @Inject
    lateinit var logDBDataRepository: LogDBDataRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        bluetoothNetworkRepository.changeBluetoothState(bluetoothAdapter.isEnabled)
        registerReceiver(mReceiver, mFilter)

//        lifecycleScope.launch {
//            bluetoothNetworkRepository.listenRegisterSpO2Sensor()
//        }
//        lifecycleScope.launch {
//            bluetoothNetworkRepository.listenRegisterEEGSensor()
//        }

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
        Log.d(TAG, "getCallback: ConnectDevice ")
        val device = bluetoothAdapter.getRemoteDevice(bluetoothInfo.bluetoothAddress)

        bluetoothInfo.bluetoothGatt = device.connectGatt(baseContext, true, bluetoothNetworkRepository.getGattCallback(bluetoothInfo.sbBluetoothDevice))

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
            bluetoothNetworkRepository.disconnectedDevice(bluetoothInfo.sbBluetoothDevice)
        }

        bluetoothInfo.dataId = null
        bluetoothInfo.bluetoothGatt = null
        bluetoothInfo.currentData = null
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
            _sbSensorInfo.value?.let {
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
                _sbSensorInfo.value.let {
                    it.dataId?.let { dataId ->
                        lifecycleScope.launch(IO) {
                            exportLastFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), forceClose)
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
            _sbSensorInfo.value?.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        Log.d(TAG, "uploading: register")
                        exportFile(dataId, sbSensorDBRepository.getMaxIndex(dataId))
                    }
                }
            }
        }

        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback { state ->
            val forceClose = notifyPowerOff(state)
            _sbSensorInfo.value?.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        exportLastFile(dataId, sbSensorDBRepository.getMaxIndex(dataId), forceClose)
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
//                registerListenSBSensorState()
                listenChannelMessage()
                startScheduler()
                registerDownloadCallback()
                // uploadStart()
                //startNotification()
                createNotificationChannel()
                startForeground(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build())

                lifecycleScope.launch {
                    bluetoothNetworkRepository.listenRegisterSBSensor(_sbSensorInfo)

                }
            }

            ActionMessage.StopSBService -> {
                // TODO 1.Cancel Alarm Manager 2.UploadAPI(End)
//                unregisterListenSBSensorState()
                stopScheduler()
                bluetoothNetworkRepository.operateDownloadSbSensor(false)
            }

            ActionMessage.StopSBServiceForced -> {
//                unregisterListenSBSensorState()
                stopScheduler()
                forcedFlow()
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
        _sbSensorInfo.value?.let {
            it.bluetoothName?.let { name ->
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        val max = sbSensorDBRepository.getMaxIndex(dataId)
                        val min = sbSensorDBRepository.getMinIndex(dataId)
                        val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)
                        if ((max - min + 1) == size) {
                            exportLastFile(dataId, max, true)
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
        val channel =  NotificationChannel(
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }
    fun startSBSensor(dataId: Int) {
        // TODO Release 주석 해제
        /*lifecycleScope.launch(IO) {
            sbSensorDBRepository.deletePastList(dataId)
        }*/
        bluetoothNetworkRepository.startNetworkSBSensor(dataId)
    }

    fun stopSBSensor() {
        bluetoothNetworkRepository.stopNetworkSBSensor()
    }


    private fun exportFile(dataId: Int, max: Int) {
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
            uploading(dataId, file, sbList)
        }
    }

    private fun exportLastFile(dataId: Int, max: Int, isForcedClose: Boolean) {
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
            uploading(dataId, file, sbList, true, isForcedClose)
        }
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
    private fun uploading(dataId: Int, file: File, list: List<SBSensorData>, isLast: Boolean = false, isForcedClose: Boolean = false) {
//        baseRequest( {
//            Log.d(TAG, "uploading - Result: $it")
//            when(it) {
//                is ApiResponse.Failure -> {
//                    if(isLast) {
//                        finishService(dataId, isForcedClose)
//                    }
//                }
//                ApiResponse.Loading -> {
//
//                }
//                ApiResponse.ReAuthorize -> {
//
//                }
//                is ApiResponse.Success -> {
//                    lifecycleScope.launch(IO) {
//                        sbSensorDBRepository.deleteUploadedList(list)
//                        file.delete()
//                    }
//                    if(isLast) {
//                        finishService(dataId, isForcedClose)
//                    }
//                 }
//             }
//        }, object: CoroutinesErrorHandler {
//            override fun onError(message: String) {
//                if(isLast) {
//                    finishService(dataId, isForcedClose)
//                }
//            }
//        }) {
//            Log.d(TAG, "uploading: uploading")
//            apneaUploadRepository.uploading(file, dataId)
//        }
    }

    private var timerOfDisconnection: Timer? = null
    private var timerOfReconnection: Timer? = null
    private var timerOfTimeout: Timer? = null

    private fun listenChannelMessage() {
        lifecycleScope.launch(IO) {
            _sbSensorInfo.value.channel.consumeEach {
                sbSensorDBRepository.insert(it)
            }
        }
    }


    private var mJob: Job? = null

    private fun cancelJob() {
        mJob?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }

    fun isForegroundServiceRunning(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.activeNotifications.find { it.id == FOREGROUND_SERVICE_NOTIFICATION_ID } != null
    }

    private fun <T> baseRequest(callback: (T) -> Unit, errorHandler: CoroutinesErrorHandler, request: () -> Flow<T>) {
        mJob = lifecycleScope.launch(IO + CoroutineExceptionHandler { _, error ->
            lifecycleScope.launch(Dispatchers.Main) {
                errorHandler.onError(error.localizedMessage ?: "Error occured! Please try again.")
            }
        }) {
            request().collect {
                withContext(Dispatchers.Main) {
                    callback(it)
                }
            }
        }
    }

    interface CoroutinesErrorHandler {
        fun onError(message: String)
    }
}