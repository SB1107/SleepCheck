package kr.co.sbsolutions.sleepcheck.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.BlueToothScanHelper
import kr.co.sbsolutions.sleepcheck.common.Cons.MINIMUM_UPLOAD_NUMBER
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.LogHelper
import kr.co.sbsolutions.sleepcheck.common.isElevenHoursPassed
import kr.co.sbsolutions.sleepcheck.common.pattern.DataFlowHelper
import kr.co.sbsolutions.sleepcheck.data.bluetooth.FirmwareData
import kr.co.sbsolutions.sleepcheck.data.firebasedb.FireBaseRealRepository
import kr.co.sbsolutions.sleepcheck.data.firebasedb.RealData
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.sleepcheck.domain.db.BreathingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.CoughDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.NoseRingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.model.SleepType
import kr.co.sbsolutions.sleepcheck.presenter.ForceConnectDeviceMessage
import kr.co.sbsolutions.sleepcheck.service.BLEService.Companion.MAX_RETRY
import kr.co.sbsolutions.sleepcheck.service.BLEService.Companion.TIME_OUT_MEASURE
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.util.Timer
import kotlin.concurrent.timerTask

@SuppressLint("MissingPermission")
class SBSensorBlueToothUseCase(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val bluetoothNetworkRepository: IBluetoothNetworkRepository,
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val settingDataRepository: SettingDataRepository,
    private val dataManager: DataManager,
    private val sbDataUploadingUseCase: SBDataUploadingUseCase,
    private val fireBaseRealRepository: FireBaseRealRepository,
    private val logHelper: ILogHelper,
    private val blueToothScanHelper: BlueToothScanHelper,
    private val packageName: String,
    private var noseRingUseCase: INoseRingHelper? = null,
    private  val noseRingDataRepository: NoseRingDataRepository,
    private val coughDataRepository: CoughDataRepository,
    private  val breathingDataRepository : BreathingDataRepository
) {
    private var timerOfDisconnection: Timer? = null
    private var context: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var startJob: Job
    private lateinit var stopJob: Job
    private lateinit var connectJob: Job
    private var timerOfStartMeasure: Timer? = null
    private var timerOfStopMeasure: Timer? = null
    private var retryCount = 0
    private var isStartAndStopCancel = false
    private var removedRealData: MutableStateFlow<RealData?> = MutableStateFlow(null)
    private var count: Int = 0
    private var connectCount = 0

    companion object {
        val bleGattList: MutableList<BluetoothGatt> = mutableListOf()
        private var timerOfTimeout: Timer? = null
    }


    init {
        lifecycleScope.launch {
            bluetoothNetworkRepository.sbSensorInfo.value.isResetGatt.collectLatest {
                if (it) {
                    logHelper.insertLog("isResetGatt collect")
                    disconnectDevice()
                }
            }
        }
    }

    fun resetBleGattList() {
        logHelper.insertLog("resetBleGattList call")
        bleGattList.map {
            it.disconnect()
            it.close()
        }
        bleGattList.clear()
    }

    fun setNoseRingUseCase(noseRingUseCase: NoseRingUseCase) {
        this.noseRingUseCase = noseRingUseCase
    }

    fun blueToothState(isEnabled: Boolean = false) {
        bluetoothNetworkRepository.changeBluetoothState(isEnabled)
    }

    fun callVibrationNotifications(intensity: Int) {
        bluetoothNetworkRepository.callVibrationNotifications(intensity)
    }

    fun getSbSensorChannel(): SharedFlow<SBSensorData> {
        return bluetoothNetworkRepository.sbSensorInfo.value.channel
    }

    fun setRemovedRealDataChange(isChange: RealData) {
        lifecycleScope.launch {
            removedRealData.emit(isChange)
        }
    }

    suspend fun fireBaseRemove() {
        settingDataRepository.getDataId()?.let {
            fireBaseRealRepository.remove(getSensorName(), it.toString())
        }
    }

    fun getRealDataRemoved(): StateFlow<RealData?> {
        return removedRealData
    }

    fun removeDataId() {
        lifecycleScope.launch(IO) {
            fireBaseRemove()
            bluetoothNetworkRepository.sbSensorInfo.value.dataId = null
        }
    }

    private fun uploadingFinish() {
        lifecycleScope.launch(IO) {
            fireBaseRealRepository.remove(getSensorName(), bluetoothNetworkRepository.sbSensorInfo.value.dataId.toString())
        }
    }

    fun connectedDevice(device: BluetoothDevice?) {
        timerOfDisconnection?.cancel()
        timerOfDisconnection = null
        bluetoothNetworkRepository.connectedDevice(device)
    }

    fun connectDevice(
        context: Context,
        bluetoothAdapter: BluetoothAdapter?,
        isForceBleDeviceConnect: Boolean = false,
        bluetoothState: BluetoothState = BluetoothState.Connecting,
        callback: ((ForceConnectDeviceMessage) -> Unit)? = null
    ) {
        this.context = context
        this.bluetoothAdapter = bluetoothAdapter
        if (isForceBleDeviceConnect && count <= MAX_RETRY) {
            logHelper.insertLog("reConnectDevice call")
            bluetoothNetworkRepository.reConnectDevice { isMaxCount ->
                if (isMaxCount) {
                    logHelper.insertLog("강제 연결 시도 하였으나 gatt 객체는 있으나 $MAX_RETRY 로인하여 디바이스 검색")
                    forceSbScanDevice(
                        context,
                        bluetoothAdapter,
                        bluetoothState = BluetoothState.DisconnectedNotIntent
                    )
                } else {
                    logHelper.insertLog("강제 연결 시도 하였으나 gatt 연결 부재 로 다시 connect 호출")
                    connectDevice(
                        context,
                        bluetoothAdapter,
                        isForceBleDeviceConnect = false,
                        bluetoothState = BluetoothState.DisconnectedNotIntent
                    )
                }
                count++
            }
            return
        }
        connectJob = lifecycleScope.launch {
            if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress == null) {
                lifecycleScope.launch {
                    val address =
                        dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_SOOM_SENSOR.type.toString())
                            .first()
                    address?.let {
                        bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress = address
                        connectDevice(
                            context,
                            bluetoothAdapter,
                            isForceBleDeviceConnect,
                            callback = callback
                        )
                        Log.e(TAG, "connectDevice: call2")
                    } ?: run {
                        disconnectDevice(context, bluetoothAdapter)
                    }
                }
                cancel()
                delay(100)
                return@launch
            }
            if (count <= MAX_RETRY) {
                count++
                //연결 시도가 중복되는 경우가 있어 gatt 연결이 다중으로 접속이되는경우가 있음
                // 디바이스 주소로 디바이스 객체를 가져와서 각트 에서 연결 상태 확인후 연결을 시도 한다.
                val device =
                    bluetoothAdapter?.getRemoteDevice(bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress)
                val bluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
//                        disconnectDevice()
//                        val address = bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress
                /*Log.d(TAG, "주소주소: $address ")
                Log.d(TAG, "이름이름: ${bluetoothNetworkRepository.sbSensorInfo.value.bluetoothName} ")*/

                val getDeviceName = bluetoothManager.getDevicesMatchingConnectionStates(
                    BluetoothProfile.GATT,
                    intArrayOf(BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING)
                ).filter { it.name == bluetoothNetworkRepository.sbSensorInfo.value.bluetoothName }
                if (getDeviceName.isEmpty()) {
                    logHelper.insertLog("연결된 디바이스가 없어서 다시 연결")
                    resetBleGattList()
                    val gatt = device?.connectGatt(
                        context,
                        true,
                        bluetoothNetworkRepository.getGattCallback(
                            bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice,
                            bluetoothState
                        )
                    )
                    gatt?.let { bleGattList.add(it) }
                    count = 0
                    getOneDataIdReadData()
                    timerOfDisconnection?.cancel()
                    callback?.invoke(ForceConnectDeviceMessage.SUCCESS_UPLOAD)
                } else {
                    logHelper.insertLog("PASS 연결된 디바이스 있다.")
                    timerOfDisconnection?.cancel()
                }
                timerOfDisconnection = Timer().apply {
                    schedule(timerTask {
                        if (isBleDeviceConnect().first.not()) {
                            logHelper.insertLog("!!재연결중 disconnectDevice")
                            lifecycleScope.launch(Dispatchers.Main) {
                                disconnectDevice(context, bluetoothAdapter)
                            }
                        }
                    }, 10000L)
                }
                Log.d(TAG, "connectDevice: $getDeviceName")
                Log.e(TAG, "connectDevice: call")
                bluetoothNetworkRepository.isSBSensorConnect()
            }

        }
    }

    private fun getOneDataIdReadData() {
        lifecycleScope.launch(IO) {
            fireBaseRealRepository.oneDataIdReadData(getSensorName(), getDataId().toString())
                .collectLatest {
                    bluetoothNetworkRepository.setRealData(it)
                }
        }
    }

    fun disconnectDevice() {
        context?.let {
            disconnectDevice(it, bluetoothAdapter)
        }
    }

    suspend fun hasSensor(): Boolean {
        return dataManager.getHasSensor().first()
    }

    private fun disconnectDevice(context: Context, bluetoothAdapter: BluetoothAdapter?) {
        bluetoothNetworkRepository.disconnectedDevice(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice)
        bluetoothNetworkRepository.releaseResource()

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
        }
    }

    fun checkWaitStart(callback: () -> Unit) {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.sbSensorInfo.collectLatest {
                Log.e(TAG, "checkWaitStart: ${it.bluetoothState}")
                if (it.bluetoothState == BluetoothState.Connected.WaitStart) {
                    callback.invoke()
                    cancel()
                    delay(100)
                    return@collectLatest
                }
            }
        }
    }

    private fun sleepDataCreate(dataId: Int, sleepType: SleepType) {
        lifecycleScope.launch(IO) {
            val sensorName = getSensorName()
            val userName = dataManager.getUserName().first() ?: ""
            // FIXME: 리얼데이터 베이스 처리
            fireBaseRealRepository.writeValue(sensorName, dataId, sleepType, userName)
            delay(2000)
            getOneDataIdReadData()
        }
    }

    fun startSBSensor(
        dataId: Int,
        sleepType: SleepType,
        hasSensor: Boolean = true,
        callback: () -> Unit
    ) {
        isStartAndStopCancel = false
        if (hasSensor) {
            sleepDataCreate(dataId, sleepType)
        }
        lifecycleScope.launch(IO) {
            settingDataRepository.setSleepTypeAndDataId(sleepType, dataId)
            logHelper.insertLog("CREATE -> dataID: $dataId   sleepType: $sleepType hasSensor: $hasSensor")
            dataManager.setHasSensor(hasSensor)
            sbSensorDBRepository.deleteAll()
            noseRingDataRepository.removeNoseRingData()
            coughDataRepository.removeCoughData()
            breathingDataRepository.removeBreathingData()
            bluetoothNetworkRepository.startNetworkSBSensor(dataId, sleepType, hasSensor).run {
                logHelper.insertLog("startNetworkSBSensor")
            }
            if (hasSensor) {
                startJob = lifecycleScope.launch {
                    timerOfStartMeasure?.cancel()
                    while (retryCount <= MAX_RETRY && isStartAndStopCancel.not()) {
                        delay(1500)
                        timerOfStartMeasure = Timer().apply {
                            schedule(timerTask {
                                bluetoothNetworkRepository.startNetworkSBSensor(dataId, sleepType, hasSensor = true)
                                logHelper.insertLog("timerOfStartMeasure - startNetworkSBSensor")
                            }, 0L)
                        }
                        retryCount += 1
                    }
                }
            }
            callback.invoke()
        }
    }

    fun waitStart() {
        if (::startJob.isInitialized) {
            startJob.cancel()
            logHelper.insertLog("waitStart() isInitialized")
        }
        isStartAndStopCancel = true
        retryCount = 0
        timerOfStartMeasure?.cancel()
        logHelper.insertLog { waitStart() }
    }

    fun finishSenor() {
        timerOfStopMeasure?.cancel()
        if (::stopJob.isInitialized) {
            stopJob.cancel()
        }
        isStartAndStopCancel = true
        retryCount = 0
        logHelper.insertLog("finishSenor ")
    }


    fun listenRegisterSBSensor() {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.listenRegisterSBSensor()
        }
    }

    fun getSbSensorInfo(): StateFlow<BluetoothInfo> {
        return bluetoothNetworkRepository.sbSensorInfo
    }

    fun listenDataFlowForceFinish() {
        bluetoothNetworkRepository.setDataFlowForceFinish {
            logHelper.registerJob("setDataFlowFinish", lifecycleScope.launch(IO) {
                DataFlowHelper(
                    sensorName = getSensorName(),
                    logHelper = logHelper,
                    coroutineScope = this,
                    settingDataRepository = settingDataRepository,
                    sbSensorDBRepository = sbSensorDBRepository,
                    bluetoothNetworkRepository = bluetoothNetworkRepository,
                    fireBaseRealRepository = fireBaseRealRepository
                ) { chainData ->
                    launch {
                        when (chainData.isSuccess) {
                            true -> {
                                val sensorName =
                                    dataManager.getBluetoothDeviceName(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice.type.name)
                                        .first() ?: ""
                                chainData.dataId?.let { id ->
                                    logHelper.insertLog("uploading:${bluetoothNetworkRepository.sbSensorInfo.value.sleepType} dataFlow 좀비 업로드")
                                    sbDataUploadingUseCase.uploading(
                                        packageName,
                                        sensorName,
                                        id,
                                        uploadSucceededCallback = {
                                            uploadingFinish()
                                        })
                                }
                                bluetoothNetworkRepository.setDataFlow(false)
                            }

                            false -> {
                                if (chainData.reasonMessage != "DataId 가없음") {
                                    sbDataUploadingUseCase.dataFlowPopUpShow()
                                }
                                bluetoothNetworkRepository.setDataFlow(false)
                            }
                        }
                    }
                }.execute()
            })
        }
    }

    fun releaseResource() {
        Log.d(TAG, "Serivce releaseResource: ")
        bluetoothNetworkRepository.releaseResource()
    }

    fun stopOperateDownloadSbSensor() {
        bluetoothNetworkRepository.operateDownloadSbSensor( isContinue = false)
    }

    fun startScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback {
            bluetoothNetworkRepository.sbSensorInfo.value.let {
                if (it.bluetoothState == BluetoothState.Connected.ReceivingRealtime) {
                    bluetoothNetworkRepository.operateDownloadSbSensor( isContinue = true)
                }
            }
        }
        setSnoreCountIncreaseCallback()
        timerOfTimeout?.cancel()

        lifecycleScope.launch(IO) {
            setStartTime()
            timerOfTimeout = Timer().apply {
                schedule(timerTask {
                    if (BLEService.getInstance()?.isForegroundServiceRunning() != true) {
                        return@timerTask
                    }
                    lifecycleScope.launch(IO) {
                        if (dataManager.getStartTime().first().isElevenHoursPassed()) {
                            logHelper.insertLog("12 시간 강제 종료")
                            stopSBSensor()
                            val forceClose = BLEService.getInstance()
                                ?.notifyPowerOff(BLEService.FinishState.FinishTimeOut) ?: false
                            bluetoothNetworkRepository.sbSensorInfo.value.let {
                                it.dataId?.let { dataId ->
                                    lifecycleScope.launch(IO) {
                                        sbDataUploadingUseCase.uploading(
                                            packageName,
                                            getSensorName(),
                                            dataId,
                                            isForced = true,
                                            uploadSucceededCallback = {
                                                uploadingFinish()
                                            })
                                    }
                                } ?: sbDataUploadingUseCase.getFinishForceCloseCallback()
                                    ?.invoke(forceClose)
                            }
                        }
                    }
                }, TIME_OUT_MEASURE)
            }
        }
    }

    private suspend fun setStartTime(time: Long = System.currentTimeMillis()) {
        dataManager.setStartTime(time)
    }


    fun isBleDeviceConnect(): Pair<Boolean, String> {
        return bluetoothNetworkRepository.isSBSensorConnect()
    }

    fun finishStop(callback: () -> Unit) {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.sbSensorInfo.collectLatest {
                if (it.bluetoothState == BluetoothState.Connected.Finish) {
                    callback.invoke()
                    cancel()
                    delay(100)
                    return@collectLatest
                }
            }
        }
    }

    fun stopSBSensor(isCancel: Boolean = false) {
        isStartAndStopCancel = false
        firebaseRemoveListener()
        logHelper.insertLog("stopSBSensor 코골이 시간: ${noseRingUseCase?.getSnoreTime()}  isCancel: $isCancel dataId: ${bluetoothNetworkRepository.sbSensorInfo.value.dataId}")
        if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.DisconnectedNotIntent) {
            logHelper.insertLog("bluetoothState: ${bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState}")
            lifecycleScope.launch(IO) {
                stopSBServiceForced(isCancel)
            }
        } else {
            bluetoothNetworkRepository.setSBSensorCancel(isCancel)
            if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState != BluetoothState.Unregistered) {
                bluetoothNetworkRepository.stopNetworkSBSensor(
                    noseRingUseCase?.getSnoreTime() ?: 0
                ) {
                    if (::stopJob.isInitialized) {
                        stopJob.cancel()
                    }
                    lifecycleScope.launch(IO) {
                        fireBaseRemove()
                    }
                }
                stopJob = lifecycleScope.launch {
                    timerOfStopMeasure?.cancel()
                    while (retryCount <= MAX_RETRY && isStartAndStopCancel.not()) {
                        delay(10000)
                        timerOfStopMeasure = Timer().apply {
                            schedule(timerTask {
                                bluetoothNetworkRepository.stopNetworkSBSensor(
                                    noseRingUseCase?.getSnoreTime() ?: 0
                                ) {
                                    if (::stopJob.isInitialized) {
                                        stopJob.cancel()
                                    }
                                    lifecycleScope.launch(IO) {
                                        fireBaseRemove()
                                    }
                                }
                                logHelper.insertLog("stopNetworkSBSensor")
                            }, 0L)
                        }
                        retryCount += 1
                    }
                    if (retryCount == MAX_RETRY) {
                        logHelper.insertLog("stop count 초과 강제종료 호출")
                        stopSBServiceForced(false)
                    }
                }

            } else {
                noSering(isCancel, true)
            }
        }
    }

    private fun noSering(isForce: Boolean, isCancel: Boolean, hasSensor: Boolean = true) {
        if (isForce) {
            sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(isForce)
            return
        }
        if (isCancel.not()) {
            bluetoothNetworkRepository.sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        logHelper.insertLog("isCancel.not: ${dataId} hasSensor: ${hasSensor} isCancel: ${isCancel}")
                        sbDataUploadingUseCase.uploading(
                            packageName,
                            getSensorName(),
                            dataId,
                            isFilePass = if (hasSensor) checkDataSize().first() else true,
                            uploadSucceededCallback = {
                                uploadingFinish()
                            })
                    }
                }
            }
        } else {
            sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(false)
        }
    }

    fun stopSBServiceForced(isCancel: Boolean = false) {
        logHelper.insertLog("${if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 강제 종료")
        logHelper.insertLog("stopSBServiceForced: $isCancel")
        stopScheduler()
        if (isCancel) {
            sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(true)
        } else {
            lifecycleScope.launch {
                if (bluetoothNetworkRepository.sbSensorInfo.value.dataId == null) {
                    logHelper.insertLog("sbSensorInfo dataId 없음")
                    val dataId = settingDataRepository.getDataId()
                    bluetoothNetworkRepository.sbSensorInfo.value.dataId = dataId
                    logHelper.insertLog("settingDataRepository dataId 복원")
                }
                forcedFlow()

            }
        }
    }

    fun firebaseRemoveListener() {
        logHelper.insertLog("firebaseRemoveListener")
        lifecycleScope.launch {
            fireBaseRealRepository.removeListener(getSensorName())
        }
    }

    fun stopScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback(null)
        timerOfTimeout?.cancel()
        bluetoothNetworkRepository.snoreCountIncrease(null)
        lifecycleScope.launch(IO) {
            setStartTime(0L)
        }
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
            bluetoothNetworkRepository.sbSensorInfo.value.dataId?.let { dataId ->
                val min = sbSensorDBRepository.getMinIndex(dataId)
                val max = sbSensorDBRepository.getMaxIndex(dataId)
                val size =
                    sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max).first()
                send(size < MINIMUM_UPLOAD_NUMBER)
                Log.d(TAG, "send(size < MINIMUM_UPLOAD_NUMBER):  ${size < MINIMUM_UPLOAD_NUMBER}")
                close()
            } ?: run {
                send(true)
                close()
            }
        }
        awaitClose()
    }

    private fun forcedFlow() {
        bluetoothNetworkRepository.sbSensorInfo.value.let {
            logHelper.insertLog("sbSensorInfo: ${it.bluetoothName}  ${it.dataId}")
            it.bluetoothName?.let { name ->
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        val max = sbSensorDBRepository.getMaxIndex(dataId)
                        val min = sbSensorDBRepository.getMinIndex(dataId)
                        val size =
                            sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)
                                .first()
                        logHelper.insertLog("forcedFlow - Index From $min~$max = ${max - min + 1} / Data Size : $size")
                        if ((max - min + 1) == size) {
                            logHelper.insertLog("(max - min + 1) == size)")
                            sbDataUploadingUseCase.uploading(
                                packageName,
                                getSensorName(),
                                dataId,
                                false,
                                isForced = true,
                                uploadSucceededCallback = {
                                    uploadingFinish()
                                })
                        } else {
                            logHelper.insertLog("(max - min + 1) != size)")
                            sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(true)
                        }
                    }
                } ?: {
                    sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(true)
                }
            } ?: {
                sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(true)
            }
        }
    }

    fun registerDownloadCallback() {
        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback { state ->
            val forceClose = BLEService.getInstance()?.notifyPowerOff(state) ?: false
            logHelper.insertLog("LastCallback -> $forceClose")
            logHelper.insertLog("LastCallback -> dataID: ${bluetoothNetworkRepository.sbSensorInfo.value.dataId}")
            bluetoothNetworkRepository.sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        sbDataUploadingUseCase.uploading(
                            packageName,
                            getSensorName(),
                            dataId,
                            forceClose,
                            uploadSucceededCallback = {
                                uploadingFinish()
                            })
                        logHelper.insertLog("uploading: register")
                    }
                } ?: sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(forceClose)
            }
        }
    }

    fun endNetworkSBSensor(isForcedClose: Boolean) {
        bluetoothNetworkRepository.endNetworkSBSensor(isForcedClose)
    }

    fun sendDownloadContinueCancel() {
        bluetoothNetworkRepository.sendDownloadContinueCancel()
    }

    fun forceDataFlowDataUploadCancel() {
        logHelper.registerJob("forceDataFlowDataUploadCancel", lifecycleScope.launch(IO) {
            sbSensorDBRepository.deleteAll()
            noseRingDataRepository.removeNoseRingData()
            coughDataRepository.removeCoughData()
            breathingDataRepository.removeBreathingData()
            sbDataUploadingUseCase.dataFlowPopUpDismiss()
        })
    }

    fun forceDataFlowDataUpload() {
        logHelper.registerJob("forceDataFlowDataUpload", lifecycleScope.launch(IO) {
            sbDataUploadingUseCase.dataFlowPopUpDismiss()
            DataFlowHelper(
                sensorName = getSensorName(),
                logHelper = logHelper,
                coroutineScope = this,
                settingDataRepository = settingDataRepository,
                sbSensorDBRepository = sbSensorDBRepository,
                bluetoothNetworkRepository = bluetoothNetworkRepository,
                fireBaseRealRepository = fireBaseRealRepository
            ) { chainData ->
                launch {
                    when (chainData.isSuccess) {
                        true -> {
                            val sensorName =
                                dataManager.getBluetoothDeviceName(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice.type.name)
                                    .first() ?: ""
                            chainData.dataId?.let { id ->
                                logHelper.insertLog("uploading:${bluetoothNetworkRepository.sbSensorInfo.value.sleepType} dataFlow 좀비 업로드")
                                sbDataUploadingUseCase.uploading(
                                    packageName,
                                    sensorName,
                                    id,
                                    uploadSucceededCallback = {
                                        uploadingFinish()
                                    })
                            }
                            bluetoothNetworkRepository.setDataFlow(false)
                        }

                        false -> {
                            bluetoothNetworkRepository.setDataFlow(false)
                        }
                    }
                }
            }.execute()
        })
    }

    fun noSensorSeringMeasurement(isForce: Boolean, isCancel: Boolean = false) {
        noSering(isForce, isCancel, false)
        noseRingUseCase?.stopAudioClassification()
    }

    suspend fun startSBService(
        context: Context,
        bluetoothAdapter: BluetoothAdapter?,
        callback: () -> Unit
    ) {
        val sleepType = settingDataRepository.getSleepType()
        settingDataRepository.getDataId()?.let {
            bluetoothNetworkRepository.sbSensorInfo.value.dataId = it
        }
        bluetoothNetworkRepository.sbSensorInfo.value.sleepType =
            if (sleepType == SleepType.Breathing.name) SleepType.Breathing else SleepType.NoSering
        logHelper.insertLog("${if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 시작")
        logHelper.insertLog("서비스 상태: ${bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState}")
        val hasSensor = dataManager.getHasSensor().first()
        if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.Registered) {
            logHelper.insertLog("서비스 재시작 Registered")
            if (hasSensor) {
                connectDevice(context, bluetoothAdapter, true)
                Log.e(TAG, "startSBService: 33")
            }
            callback.invoke()
        }
        if (sleepType == SleepType.NoSering.name && hasSensor.not()) {
            callback.invoke()
        }
    }

    fun getSleepType(): SleepType {
        return bluetoothNetworkRepository.sbSensorInfo.value.sleepType
    }

    fun setDataId() {
        lifecycleScope.launch(IO) {
            settingDataRepository.getDataId()?.let {
                bluetoothNetworkRepository.setDataId(it)
            }
        }
    }

    private fun setSnoreCountIncreaseCallback() {
        bluetoothNetworkRepository.snoreCountIncrease {
            noseRingUseCase?.snoreCountIncrease()
        }
    }

    fun deletePastList() {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.sbSensorInfo.value.dataId?.let {
                sbSensorDBRepository.deletePastList(
                    it
                )
            }
        }
    }

    suspend fun getSensorName(): String {
        return dataManager.getBluetoothDeviceName(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice.type.name)
            .first() ?: ""
    }

    fun unregisterDownloadCallback() {
        bluetoothNetworkRepository.setOnDownloadCompleteCallback(null)
        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback(null)
    }

    suspend fun getDataId(): Int {
        return settingDataRepository.getDataId() ?: -1
    }

    fun motorTest(intensity: Int) {
        bluetoothNetworkRepository.startMotorTest(intensity)
    }

    fun getFirmwareVersion(): Flow<FirmwareData?> {
        return bluetoothNetworkRepository.getFirmwareVersion()
    }

    fun forceSbScanDevice(
        context: Context,
        bluetoothAdapter: BluetoothAdapter?,
        bluetoothState: BluetoothState = BluetoothState.DisconnectedNotIntent,
        callback: ((ForceConnectDeviceMessage) -> Unit)? = null
    ) {
        var job: Job? = null
        lifecycleScope.let {
            it.launch {
                blueToothScanHelper.scanBLEDevices(it)
                logHelper.insertLog("디바이스 스캔 호출")
            }
            job = it.launch {
                launch {
                    blueToothScanHelper.isScanning.collectLatest { isScanning ->
                        if (isScanning.not()) {
                            if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress == null) {
                                val address = dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_SOOM_SENSOR.type.toString()).first()
                                address?.let {
                                    bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress = address
                                    logHelper.insertLog("bluetoothAddress 객체 없음 = $address 주입")
                                }
                            }
                            val device = blueToothScanHelper.scanList.value.firstOrNull { device ->
                                logHelper.insertLog("검색 ${device.name} = ${device.address}")
                                logHelper.insertLog("등록된 주소 = ${bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress}")
                                device.address == bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress
                            }
                            device?.let {
                                logHelper.insertLog("디바이스 찾기 완료")
                                connectDevice(
                                    context,
                                    bluetoothAdapter,
                                    bluetoothState = bluetoothState,
                                    callback = callback
                                )
                                job?.cancel()
                            } ?: run {
                                logHelper.insertLog("디바이스 찾기 실패")
                                if (connectCount == 0) {
                                    connectCount += 1
                                }else{
                                    logHelper.insertLog("디바이스 연결 실패 카운트 초과 강제 세션 맺기 호출")
                                    connectDevice(context, bluetoothAdapter, isForceBleDeviceConnect = false, bluetoothState = BluetoothState.DisconnectedNotIntent)
                                    job?.cancel()
                                    return@run
                                }
                                if (bluetoothNetworkRepository.isSBSensorConnect().first) {
                                    logHelper.insertLog("디바이스 연결됨")
                                    callback?.invoke(ForceConnectDeviceMessage.SUCCESS)
                                } else {
                                    callback?.invoke(
                                        ForceConnectDeviceMessage.FAIL(
                                            context.getString(
                                                R.string.sensor_disconnect_error2
                                            )
                                        )
                                    )
                                }
                                job?.cancel()
                            }
                        }
                    }
                }
            }

        }
    }

}