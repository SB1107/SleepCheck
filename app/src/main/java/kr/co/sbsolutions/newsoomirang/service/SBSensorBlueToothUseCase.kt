package kr.co.sbsolutions.newsoomirang.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.MINIMUM_UPLOAD_NUMBER
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.pattern.DataFlowHelper
import kr.co.sbsolutions.newsoomirang.data.firebasedb.FireBaseRealRepository
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.service.BLEService.Companion.MAX_RETRY
import kr.co.sbsolutions.newsoomirang.service.BLEService.Companion.TIME_OUT_MEASURE
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
    private val logHelper: LogHelper,
    private val packageName: String
) {
    private var timerOfDisconnection: Timer? = null
    private var context: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var timerOfTimeout: Timer? = null
    private lateinit var startJob: Job
    private lateinit var stopJob: Job
    private var timerOfStartMeasure: Timer? = null
    private var timerOfStopMeasure: Timer? = null
    private var retryCount = 0
    private var noseRingUseCase: NoseRingUseCase? = null
    var isStartAndStopCancel = false

    fun setNoseRingUseCase(noseRingUseCase: NoseRingUseCase) {
        this.noseRingUseCase = noseRingUseCase
    }

    fun blueToothState(isEnabled: Boolean = false) {
        bluetoothNetworkRepository.changeBluetoothState(isEnabled)
    }

    fun callVibrationNotifications(intensity: Int) {
        bluetoothNetworkRepository.callVibrationNotifications(intensity)
    }

    fun getSbSensorChannel(): Channel<SBSensorData> {
        return bluetoothNetworkRepository.sbSensorInfo.value.channel
    }

    fun setRealDataChange(isChange: Boolean) {
        bluetoothNetworkRepository.setIsDataChange(isChange)
    }

    suspend fun fireBaseRemove() {
        settingDataRepository.getDataId()?.let {
            fireBaseRealRepository.remove(getSensorName(), it.toString())
            bluetoothNetworkRepository.setRealDataRemove(true)
        }
    }

    fun removeDataId() {
        lifecycleScope.launch(IO) {
            fireBaseRemove()
            bluetoothNetworkRepository.sbSensorInfo.value.dataId = null
            bluetoothNetworkRepository.setRealDataRemove(true)
            bluetoothNetworkRepository.setIsDataChange(true)
        }
    }

    fun uploadingFinish() {
        lifecycleScope.launch(IO) {
            fireBaseRealRepository.remove(getSensorName(), bluetoothNetworkRepository.sbSensorInfo.value.dataId.toString())
        }
    }

    @Deprecated("삭제됨?")
    fun isDataFlowState(): Boolean {
        return bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.Connected.DataFlow ||
                bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.Connected.DataFlowUploadFinish
    }

    fun connectedDevice(device: BluetoothDevice?) {
        timerOfDisconnection?.cancel()
        timerOfDisconnection = null
        bluetoothNetworkRepository.connectedDevice(device)
    }

    fun connectDevice(context: Context, bluetoothAdapter: BluetoothAdapter?, isForceBleDeviceConnect: Boolean = false) {
        this.context = context
        this.bluetoothAdapter = bluetoothAdapter
        val device = bluetoothAdapter?.getRemoteDevice(bluetoothNetworkRepository.sbSensorInfo.value.bluetoothAddress)
        device?.connectGatt(context, true, bluetoothNetworkRepository.getGattCallback(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice))
        if (isForceBleDeviceConnect) {
            bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState = BluetoothState.DisconnectedNotIntent
            return
        }
        timerOfDisconnection?.cancel()
        timerOfDisconnection = Timer().apply {
            schedule(timerTask {
                Log.e(TAG, "connectDevice: ")
                logHelper.insertLog("!!재연결중 disconnectDevice")
                lifecycleScope.launch(Dispatchers.Main) {
                    disconnectDevice(context, bluetoothAdapter)
                }
            }, 10000L)
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
        bluetoothNetworkRepository.disconnectedDevice(SBBluetoothDevice.SB_SOOM_SENSOR)
        bluetoothNetworkRepository.releaseResource()

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
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
        }
    }

    fun startSBSensor(dataId: Int, sleepType: SleepType, hasSensor: Boolean = true) {
        isStartAndStopCancel = false
        sleepDataCreate(dataId, sleepType)
        lifecycleScope.launch(IO) {
            settingDataRepository.setSleepTypeAndDataId(sleepType, dataId)
            logHelper.insertLog("CREATE -> dataID: $dataId   sleepType: $sleepType hasSensor: $hasSensor")
            dataManager.setHasSensor(hasSensor)
            sbSensorDBRepository.deleteAll()
            if (hasSensor) {
                bluetoothNetworkRepository.startNetworkSBSensor(dataId, sleepType).run {
                    logHelper.insertLog("startNetworkSBSensor")
                }
                startJob = lifecycleScope.launch {
                    timerOfStartMeasure?.cancel()
                    while (retryCount >= MAX_RETRY || isStartAndStopCancel.not()) {
                        delay(1500)
                        timerOfStartMeasure = Timer().apply {
                            schedule(timerTask {
                                bluetoothNetworkRepository.startNetworkSBSensor(dataId, sleepType)
                                logHelper.insertLog("timerOfStartMeasure - startNetworkSBSensor")
                            }, 0L)
                        }
                        retryCount += 1
                    }
                }
            }
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
        logHelper.insertLog { finishSenor() }
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
                    sensorName = getSensorName(), logHelper = logHelper, coroutineScope = this,
                    settingDataRepository = settingDataRepository, sbSensorDBRepository = sbSensorDBRepository,
                    bluetoothNetworkRepository = bluetoothNetworkRepository,
                    fireBaseRealRepository = fireBaseRealRepository
                ) { chainData ->
                    launch {
                        when (chainData.isSuccess) {
                            true -> {
                                val sensorName = dataManager.getBluetoothDeviceName(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice.type.name).first() ?: ""
                                chainData.dataId?.let { id ->
                                    logHelper.insertLog("uploading:${bluetoothNetworkRepository.sbSensorInfo.value.sleepType} dataFlow 좀비 업로드")
                                    sbDataUploadingUseCase.uploading(packageName, sensorName, id)
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
        bluetoothNetworkRepository.operateDownloadSbSensor(false)
    }

    fun startScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback {
            bluetoothNetworkRepository.sbSensorInfo.value.let {
                if (it.bluetoothState == BluetoothState.Connected.ReceivingRealtime) {
                    bluetoothNetworkRepository.operateDownloadSbSensor(true)
                }
            }
        }
        timerOfTimeout?.cancel()
        timerOfTimeout = Timer().apply {
            schedule(timerTask {
                stopSBSensor()
                val forceClose = BLEService.getInstance()?.notifyPowerOff(BLEService.FinishState.FinishTimeOut) ?: false
                bluetoothNetworkRepository.sbSensorInfo.value.let {
                    it.dataId?.let { dataId ->
                        lifecycleScope.launch(IO) {
                            sbDataUploadingUseCase.uploading(packageName, getSensorName(), dataId)
                        }
                    } ?: sbDataUploadingUseCase.getFinishForceCloseCallback()?.invoke(forceClose)
                }

            }, TIME_OUT_MEASURE)
        }
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
                }
            }
        }
    }

    fun stopSBSensor(isCancel: Boolean = false) {
        isStartAndStopCancel = false
        logHelper.insertLog("stopSBSensor 코골이 시간: ${noseRingUseCase?.getSnoreTime()}  isCancel: $isCancel dataId: ${bluetoothNetworkRepository.sbSensorInfo.value.dataId}")
        if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.DisconnectedNotIntent) {
            logHelper.insertLog("bluetoothState: ${bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState}")
            lifecycleScope.launch(IO) {
                stopSBServiceForced(isCancel)
            }
        } else {
            bluetoothNetworkRepository.setSBSensorCancel(isCancel)
            if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState != BluetoothState.Unregistered) {
                bluetoothNetworkRepository.stopNetworkSBSensor(noseRingUseCase?.getSnoreTime() ?: 0)
                stopJob = lifecycleScope.launch {
                    timerOfStopMeasure?.cancel()
                    while (retryCount >= MAX_RETRY || isStartAndStopCancel.not()) {
                        delay(1500)
                        timerOfStopMeasure = Timer().apply {
                            schedule(timerTask {
                                bluetoothNetworkRepository.stopNetworkSBSensor(noseRingUseCase?.getSnoreTime() ?: 0)
                                logHelper.insertLog("stopNetworkSBSensor")
                            }, 0L)
                        }
                        retryCount += 1
                    }
                }

            } else {
                noSering(isCancel, true)
            }
        }
    }

    private fun noSering(isCancel: Boolean, hasSensor: Boolean = true) {
        if (isCancel.not()) {
            bluetoothNetworkRepository.sbSensorInfo.value.let {
                it.dataId?.let { dataId ->
                    lifecycleScope.launch(IO) {
                        logHelper.insertLog("isCancel.not: ${dataId} hasSensor: ${hasSensor} isCancel: ${isCancel}")
                        sbDataUploadingUseCase.uploading(packageName, getSensorName(), dataId, isFilePass = if (hasSensor) checkDataSize().first() else true)
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
            forcedFlow()
        }
    }


    fun stopScheduler() {
        bluetoothNetworkRepository.setOnUploadCallback(null)
        timerOfTimeout?.cancel()
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
                val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max).first()
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
                        val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max).first()
                        logHelper.insertLog("forcedFlow - Index From $min~$max = ${max - min + 1} / Data Size : $size")
                        if ((max - min + 1) == size) {
                            logHelper.insertLog("(max - min + 1) == size)")
                            sbDataUploadingUseCase.uploading(packageName, getSensorName(), dataId, false)
                        } else {
                            logHelper.insertLog("(max - min + 1) == size)")
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
                        sbDataUploadingUseCase.uploading(packageName, getSensorName(), dataId, forceClose)
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
            sbDataUploadingUseCase.dataFlowPopUpDismiss()
        })
    }

    fun forceDataFlowDataUpload() {
        logHelper.registerJob("forceDataFlowDataUpload", lifecycleScope.launch(IO) {
            sbDataUploadingUseCase.dataFlowPopUpDismiss()
            DataFlowHelper(
                sensorName = getSensorName(),
                logHelper = logHelper, coroutineScope = this,
                settingDataRepository = settingDataRepository, sbSensorDBRepository = sbSensorDBRepository,
                bluetoothNetworkRepository = bluetoothNetworkRepository,
                fireBaseRealRepository = fireBaseRealRepository
            ) { chainData ->
                launch {
                    when (chainData.isSuccess) {
                        true -> {
                            val sensorName = dataManager.getBluetoothDeviceName(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice.type.name).first() ?: ""
                            chainData.dataId?.let { id ->
                                logHelper.insertLog("uploading:${bluetoothNetworkRepository.sbSensorInfo.value.sleepType} dataFlow 좀비 업로드")
                                sbDataUploadingUseCase.uploading(packageName, sensorName, id)
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

    fun noSensorSeringMeasurement(isCancel: Boolean = false) {
        noSering(isCancel, false)
        noseRingUseCase?.stopAudioClassification()
    }

    suspend fun startSBService(context: Context, bluetoothAdapter: BluetoothAdapter?) {
        val sleepType = settingDataRepository.getSleepType()
        settingDataRepository.getDataId()?.let {
            bluetoothNetworkRepository.sbSensorInfo.value.dataId = it
        }
        bluetoothNetworkRepository.sbSensorInfo.value.sleepType = if (sleepType == SleepType.Breathing.name) SleepType.Breathing else SleepType.NoSering
        logHelper.insertLog("${if (bluetoothNetworkRepository.sbSensorInfo.value.sleepType == SleepType.Breathing) "호흡" else "코골이"} 측정 시작")
        delay(1000)
        if (bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.Registered) {
            val hasSensor = dataManager.getHasSensor().first()
            if (hasSensor) {
                connectDevice(context, bluetoothAdapter, true)
            }
        }
    }

    fun getSleepType(): SleepType {
        return bluetoothNetworkRepository.sbSensorInfo.value.sleepType
    }

    fun setDataId() {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.sbSensorInfo.value.dataId = settingDataRepository.getDataId()
        }

    }

    fun isBlueToothStateRegistered(): Boolean {
        return bluetoothNetworkRepository.sbSensorInfo.value.bluetoothState == BluetoothState.Registered
    }

    fun deletePastList() {
        lifecycleScope.launch(IO) {
            bluetoothNetworkRepository.sbSensorInfo.value.dataId?.let { sbSensorDBRepository.deletePastList(it) }
        }
    }

    suspend fun getSensorName(): String {
        return dataManager.getBluetoothDeviceName(bluetoothNetworkRepository.sbSensorInfo.value.sbBluetoothDevice.type.name).first() ?: ""
    }

    fun unregisterDownloadCallback() {
        bluetoothNetworkRepository.setOnDownloadCompleteCallback(null)
        bluetoothNetworkRepository.setOnLastDownloadCompleteCallback(null)
    }

    suspend fun getDataId(): Int {
        return settingDataRepository.getDataId() ?: -1
    }

}