package kr.co.sbsolutions.newsoomirang.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kr.co.sbsolutions.newsoomirang.common.AESHelper
import kr.co.sbsolutions.newsoomirang.common.BluetoothUtils
import kr.co.sbsolutions.newsoomirang.common.Cons.CLIENT_CHARACTERISTIC_CONFIG
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.hexToString
import kr.co.sbsolutions.newsoomirang.common.prefixToHex
import kr.co.sbsolutions.newsoomirang.data.firebasedb.RealData
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.service.BLEService
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BluetoothNetworkRepository @Inject constructor(
    private val dataManager: DataManager,
    private val settingDataRepository: SettingDataRepository,
    private val logHelper: LogHelper,
    private val aesHelper: AESHelper
) : IBluetoothNetworkRepository {
    private val logCoroutine = CoroutineScope(Dispatchers.IO)


    override val sbSensorInfo: StateFlow<BluetoothInfo> = _sbSensorInfo.asStateFlow()

    override val spo2SensorInfo: StateFlow<BluetoothInfo> = _spo2SensorInfo.asStateFlow()

    override val eegSensorInfo: StateFlow<BluetoothInfo> = _eegSensorInfo.asStateFlow()
    private val _sbSensorFirmwareInfo: MutableSharedFlow<FirmwareData?> = MutableSharedFlow()

    private val defaultPrefix = "FE9B8003"
    private var isEncrypt = false
    private lateinit var sendDownloadContinueJob: Job


    companion object {
        private var isSBSensorConnect: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(Pair(false, ""))
        private val _sbSensorInfo = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR))
        private val _spo2SensorInfo = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SPO2_SENSOR))
        private val _eegSensorInfo = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_EEG_SENSOR))
        private var reConnectCount = 0
    }

    override suspend fun listenRegisterSBSensor() {

        dataManager.getBluetoothDeviceName(_sbSensorInfo.value.sbBluetoothDevice.type.toString())
            .zip(dataManager.getBluetoothDeviceAddress(_sbSensorInfo.value.sbBluetoothDevice.type.toString()))
            { name, address ->
                Log.d(TAG, "listenRegisterSBSensor: $name $address")
                _sbSensorInfo.update { it.copy(bluetoothName = name, bluetoothAddress = address) }
                !name.isNullOrEmpty() && !address.isNullOrEmpty()
            }.collect { registered ->
                //등록이 안되어 있는 상태 에서 같은 이벤트가 들어오면 무시

                val result = _sbSensorInfo.updateAndGet { it.copy(bluetoothState = if (registered) BluetoothState.Registered else BluetoothState.Unregistered) }
                logHelper.insertLog("listenRegisterSBSensor -> ${result.bluetoothState}")
                Log.e(TAG, "listenRegisterSBSensor: 2")

//                when {
//                    _sbSensorInfo.value.bluetoothState == BluetoothState.Unregistered && registered.not() || _sbSensorInfo.value.bluetoothState == BluetoothState.Registered && registered -> {
//                        Log.e(TAG, "listenRegisterSBSensor: 1")
//                    }
//
//                    _sbSensorInfo.value.bluetoothState == BluetoothState.Unregistered && registered -> {
//                        val result = _sbSensorInfo.updateAndGet { it.copy(bluetoothState = BluetoothState.Registered) }
//                        logHelper.insertLog(result.bluetoothState)
//                        Log.e(TAG, "listenRegisterSBSensor: 2")
//                    }
//
//                    _sbSensorInfo.value.bluetoothState == BluetoothState.Registered && registered.not()
//                            || _sbSensorInfo.value.bluetoothState == BluetoothState.DisconnectedByUser && registered.not()
//                            || _sbSensorInfo.value.bluetoothState == BluetoothState.Connected.Ready && registered.not() -> {
//                        val result = _sbSensorInfo.updateAndGet { it.copy(bluetoothState = BluetoothState.Unregistered) }
//                        logHelper.insertLog(result.bluetoothState)
//                        Log.e(TAG, "listenRegisterSBSensor: 3")
//                    }
//
//                    else -> {
//                        Log.e(TAG, "listenRegisterSBSensor: 4")
//                        Log.e(TAG, "state =${_sbSensorInfo.value.bluetoothState}")
////                        val result = _sbSensorInfo.updateAndGet { it.copy(bluetoothState = if (registered) BluetoothState.Registered else BluetoothState.Unregistered) }
////                        insertLog(result.bluetoothState)
//                    }
//                }
            }


    }

    override fun reConnectDevice(callback: ((isMaxCount : Boolean) -> Unit)) {
        logCoroutine.launch {
            _sbSensorInfo.value.bluetoothGatt?.let {
                logHelper.insertLog("reconnectDevice gatt 재접속 실행")
                reConnectCount += 1
                if (reConnectCount >= BLEService.MAX_RETRY ) {
                    logHelper.insertLog("reconnectDevice gatt 재접속 ${BLEService.MAX_RETRY} 도달로 인하여 다시 connect callback")
                    callback.invoke(true)
                    reConnectCount  = 0
                  return@launch
                }
                isConnect(it, _sbSensorInfo) { isSuccessfully ->
                    logHelper.insertLog("reconnectDevice isSuccessfully = $isSuccessfully")
                }
            } ?: run {
                logHelper.insertLog("gatt 연결 접속 정보 없음 ")
                callback.invoke(false)
            }

        }
    }

    override fun setSBSensorCancel(isCancel: Boolean) {
        _sbSensorInfo.update {
            it.copy(cancelCheck = isCancel)
        }
    }

    override fun setDataFlow(isDataFlow: Boolean, currentCount: Int, totalCount: Int) {
        dataFlowMaxCount = totalCount
        _sbSensorInfo.value.isDataFlow.update { it.copy(isDataFlow = isDataFlow, currentCount = currentCount, totalCount = totalCount) }
    }

    override fun isSBSensorConnect(): Pair<Boolean, String> {
        return isSBSensorConnect.value
    }

    override suspend fun listenRegisterSpO2Sensor() {
//        dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SPO2_SENSOR.toString())
//            .zip(dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_SPO2_SENSOR.toString()))
//            { name, address ->
//                _spo2SensorInfo.value?.let {
//                    it.bluetoothName = name
//                    it.bluetoothAddress = address
//                }
////                Log.d(TAG, "[SPO2_SENSOR] NAME: $name, ADDR: $address")
//                !name.isNullOrEmpty() && !address.isNullOrEmpty()
//            }.collect { registered->
//                _spo2SensorInfo.value?.let {
//                    it.bluetoothState = if(registered) BluetoothState.Registered else BluetoothState.Unregistered
//                    _spo2SensorInfo.tryEmit(it)
//                }
//            }
    }

    override suspend fun listenRegisterEEGSensor() {
//        dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_EEG_SENSOR.toString())
//            .zip(dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_EEG_SENSOR.toString()))
//            { name, address ->
//                _eegSensorInfo.value?.let {
//                    it.bluetoothName = name
//                    it.bluetoothAddress = address
//                }
////                Log.d(TAG, "[EEG_SENSOR] NAME: $name, ADDR: $address")
//                !name.isNullOrEmpty() && !address.isNullOrEmpty()
//            }.collect { registered->
//                _eegSensorInfo.value?.let {
//                    it.bluetoothState = if(registered) BluetoothState.Registered else BluetoothState.Unregistered
//                    _eegSensorInfo.tryEmit(it)
//                }
//            }
    }

    override fun getDeviceAddress(sbBluetoothDevice: SBBluetoothDevice): String? {
        return when (sbBluetoothDevice) {
            SBBluetoothDevice.SB_EEG_SENSOR -> {
                _eegSensorInfo.value.bluetoothAddress
//                flowData.value.bluetoothAddress
            }

            SBBluetoothDevice.SB_SOOM_SENSOR -> {
                _sbSensorInfo.value.bluetoothAddress
            }

            SBBluetoothDevice.SB_SPO2_SENSOR -> {
                _spo2SensorInfo.value.bluetoothAddress
            }
        }
    }

    override fun connectedDevice(device: BluetoothDevice?) {
        when (device?.address) {
            _sbSensorInfo.value.bluetoothAddress ->
                _sbSensorInfo

            _spo2SensorInfo.value.bluetoothAddress ->
                _spo2SensorInfo

            _eegSensorInfo.value.bluetoothAddress ->
                _eegSensorInfo

            else -> {
                Log.d(TAG, "주소가 다르다.: ")
                return
            }
        }.apply {
            logHelper.insertLog("connectedDevice: ${value.bluetoothState}")
            val result = updateAndGet {
                it.copy(
                    bluetoothState =
                    if (it.bluetoothState == BluetoothState.DisconnectedNotIntent) {
                        BluetoothState.Connected.Reconnected
                    } else {
                        BluetoothState.Connected.Init
                    }
                )
            }
            logHelper.insertLog(result.bluetoothState)
        }
    }

    private fun disconnectedDevice(gatt: BluetoothGatt) {
        when (gatt.device.address) {
            _sbSensorInfo.value.bluetoothAddress -> _sbSensorInfo

            _spo2SensorInfo.value.bluetoothAddress -> _spo2SensorInfo

            _eegSensorInfo.value.bluetoothAddress -> _eegSensorInfo

            else -> {
                Log.d(TAG, "disconnectedDevice = 없음")
                return
            }
        }.apply {
            value.let { bi ->
                when (bi.bluetoothState) {
                    BluetoothState.Connected.Ready,
                    BluetoothState.Connected.ReceivingDelayed,
                    BluetoothState.Connected.Reconnected,
                    BluetoothState.Connected.ReceivingRealtime,
                    BluetoothState.Connected.SendDelayed,
                    BluetoothState.Connected.SendDelete,
                    BluetoothState.Connected.SendDownload,
                    BluetoothState.Connected.SendDownloadContinue,
                    BluetoothState.Connected.SendRealtime,
                    BluetoothState.Connected.SendStart,
                    BluetoothState.Connected.SendStop,
                    BluetoothState.Connected.ForceEnd,
                    BluetoothState.Connected.End,
                    BluetoothState.Connected.MotCtrlSet,
                    BluetoothState.Connected.WaitStart -> {
                        Log.d(TAG, "NotIntent = NotIntent")
                        this.update { it.copy(bluetoothState = BluetoothState.DisconnectedNotIntent) }
                        logHelper.insertLog(BluetoothState.DisconnectedNotIntent)
                    }

                    else -> {
                        Log.d(TAG, "disconnect = disconnect")
                        this.update { it.copy(bluetoothGatt = null, bluetoothState = BluetoothState.DisconnectedByUser) }
                        logHelper.insertLog("disconnectedDevice -> DisconnectedByUser")
                        logCoroutine.launch {
                            logHelper.insertLog("isResetGatt call")
                            bi.isResetGatt.emit(true)
                        }
                    }
                }
            }
        }
    }

    override fun changeBluetoothState(isOn: Boolean) {
        BluetoothInfo.isOn = isOn
        _sbSensorInfo.apply { update { it.copy() } }
        _spo2SensorInfo.apply { update { it.copy() } }
        _eegSensorInfo.apply { update { it.copy() } }
        Log.e(TAG, "changeBluetoothState: isOn = ${isOn}")
        releaseResource()
    }

    override fun disconnectedDevice(sbBluetoothDevice: SBBluetoothDevice) {
        when (sbBluetoothDevice) {
            SBBluetoothDevice.SB_SOOM_SENSOR ->
                _sbSensorInfo

            SBBluetoothDevice.SB_SPO2_SENSOR ->
                _spo2SensorInfo

            SBBluetoothDevice.SB_EEG_SENSOR ->
                _eegSensorInfo
        }.apply {
            value.let { info ->
                if (info.bluetoothState != BluetoothState.Unregistered) {
                    val result = updateAndGet { it.copy(bluetoothState = BluetoothState.DisconnectedByUser, batteryInfo = null) }
                    logHelper.insertLog(result.bluetoothState)
                }
            }
        }
    }

    override fun releaseResource() {
        _sbSensorInfo.value.apply {
            try {
//                if (bluetoothState != BluetoothState.Unregistered) {
//                    bluetoothState = BluetoothState.DisconnectedByUser
//                    //                Log.d(TAG, "disconnectedDevice: 2")
//                }

                bluetoothGatt?.let {
                    it.setCharacteristicNotification(BluetoothUtils.findResponseCharacteristic(it), false)
                    it.disconnect()
                    it.close()
                    Log.e(TAG, "releaseResource: ")
                }
                _sbSensorInfo.update {
                    it.copy(
                        dataId = null, bluetoothGatt = null
                    )
                }
            } catch (_e: Exception) {
                logHelper.insertLog("_sbSensorInfo ${_e.message.toString()}")
            }
        }
        _spo2SensorInfo.value.apply {
            try {
                if (bluetoothState != BluetoothState.Unregistered) {
                    bluetoothState = BluetoothState.DisconnectedByUser
                    //                Log.d(TAG, "disconnectedDevice: 3")
                }
                bluetoothGatt?.let {
                    it.setCharacteristicNotification(BluetoothUtils.findResponseCharacteristic(it), false)
                    it.disconnect()
                    it.close()
                }
                dataId = null
                bluetoothGatt = null
            } catch (_e: Exception) {
                logHelper.insertLog("_spo2SensorInfo ${_e.message.toString()}")
            }
        }
//
        _eegSensorInfo.value.apply {
            try {
                if (bluetoothState != BluetoothState.Unregistered) {
                    bluetoothState = BluetoothState.DisconnectedByUser
                    //                Log.d(TAG, "disconnectedDevice: 4")
                }
                bluetoothGatt?.let {
                    it.setCharacteristicNotification(BluetoothUtils.findResponseCharacteristic(it), false)
                    it.disconnect()
                    it.close()
                }
                dataId = null
                bluetoothGatt = null
            } catch (_e: Exception) {
                logHelper.insertLog("_eegSensorInfo ${_e.message.toString()}")
            }
        }
        Log.d(TAG, "releaseResource: ")
    }

    override fun setRealData(realData: RealData?) {
        if (realData == null) {
            logCoroutine.launch {
                _sbSensorInfo.value.realData.emit(null)
            }
            return
        }
        val data = _sbSensorInfo.value.realData.updateAndGet {
            it?.copy(
                sensorName = realData.sensorName,
                dataId = realData.dataId,
                userName = realData.userName,
                sleepType = realData.sleepType,
                timeStamp = realData.timeStamp,
            )
        } ?: let {
            logCoroutine.launch {
                _sbSensorInfo.value.realData.emit(realData)
                logHelper.insertLog("setRealData = $realData")
            }
        }


    }

    override fun startNetworkSBSensor(dataId: Int, sleepType: SleepType) {
        val module = if (sleepType == SleepType.Breathing) AppToModule.BreathingOperateStart else AppToModule.NoSeringOperateStart
        if (_sbSensorInfo.value.bluetoothState == BluetoothState.Unregistered) {
            _sbSensorInfo.update { it.copy(dataId = dataId, sleepType = sleepType, snoreTime = 0) }
            return
        }

        if (_sbSensorInfo.value.bluetoothState != BluetoothState.Unregistered) {
            writeData(_sbSensorInfo.value.bluetoothGatt, module) { state ->
                _sbSensorInfo.update { it.copy(dataId = dataId, bluetoothState = state, sleepType = sleepType, snoreTime = 0) }
                logHelper.insertLog(state)
            }
            return
        }
    }

    private var stopCallBack: (() -> Unit)? = null
    override fun stopNetworkSBSensor(snoreTime: Long, callback: (() -> Unit)) {
        val module = if (_sbSensorInfo.value.sleepType == SleepType.Breathing) AppToModule.BreathingOperateStop else AppToModule.NoSeringOperateStop
        stopCallBack = callback
        writeData(_sbSensorInfo.value.bluetoothGatt, module) { state ->
            logHelper.insertLog("stopNetworkSBSensor: $state   ${module.getName()}  snoreTime: $snoreTime")
            _sbSensorInfo.update { it.copy(bluetoothState = state, snoreTime = snoreTime) }
            logHelper.insertLog(state)

        }
    }

    override fun endNetworkSBSensor(isForcedClose: Boolean) {

        val log: String = when (_sbSensorInfo.value.bluetoothState) {
            BluetoothState.DisconnectedNotIntent -> {
                "DisconnectedNotIntent ForceEnd"
            }

            BluetoothState.DisconnectedByUser -> {
                "DisconnectedByUser ForceEnd"
            }

            else -> {
                if (isForcedClose) "ForceEnd" else "End"
            }
        }
        logHelper.insertLog(log)

        _sbSensorInfo.update {
            it.copy(
                bluetoothState =
                when (_sbSensorInfo.value.bluetoothState) {
                    BluetoothState.DisconnectedNotIntent ->
                        BluetoothState.DisconnectedNotIntent


                    BluetoothState.DisconnectedByUser ->
                        BluetoothState.DisconnectedByUser

                    BluetoothState.Unregistered ->
                        BluetoothState.Unregistered


                    else ->
                        if (isForcedClose) BluetoothState.Connected.ForceEnd else BluetoothState.Connected.End

                }
            )
        }
//        logHelper.insertLog(result.bluetoothState)
//        _sbSensorInfo.value.let {
//            if (isForcedClose) {
//                it.bluetoothState = BluetoothState.Connected.ForceEnd
//            } else {
//                it.bluetoothState = BluetoothState.Connected.End
//            }
//            _sbSensorInfo.tryEmit(it)
//            insertLog(it.bluetoothState)
//        }
    }

    override fun operateRealtimeSBSensor() {
        writeData(_sbSensorInfo.value.bluetoothGatt, AppToModule.OperateChangeProcessRealtime) { state ->
            _sbSensorInfo.update { it.copy(bluetoothState = state) }
            logHelper.insertLog(state)
//            _sbSensorInfo.value?.let {
//                it.bluetoothState = state
//                _sbSensorInfo.tryEmit(it)
//                insertLog(it.bluetoothState)
//            }
        }
    }

    override fun operateDelayedSBSensor() {
        writeData(_sbSensorInfo.value.bluetoothGatt, AppToModule.OperateChangeProcessDelayed) { state ->
            _sbSensorInfo.update { it.copy(bluetoothState = state) }
            logHelper.insertLog(state)
        }
    }

    override fun operateDownloadSbSensor(isContinue: Boolean) {
        writeData(_sbSensorInfo.value.bluetoothGatt, if (isContinue) AppToModule.OperateDownloadContinue else AppToModule.OperateDownload) { state ->
            _sbSensorInfo.update { it.copy(bluetoothState = state) }
            logHelper.insertLog(state)
        }
    }

    override var downloadCompleteCallback: (() -> Unit)? = null

    @Deprecated("사용안함", ReplaceWith("downloadCompleteCallback = callback"))
    override fun setOnDownloadCompleteCallback(callback: (() -> Unit)?) {
        downloadCompleteCallback = callback
    }

    override var lastDownloadCompleteCallback: ((state: BLEService.FinishState) -> Unit)? = null

    override fun setOnLastDownloadCompleteCallback(callback: ((state: BLEService.FinishState) -> Unit)?) {
        lastDownloadCompleteCallback = callback
    }

    private var dataFlowCallback: (() -> Unit)? = null

    override fun setDataFlowForceFinish(callBack: (() -> Unit)?) {
        dataFlowCallback = callBack
    }

    private var snoreCountIncreaseCallBack: (() -> Unit)? = null

    override fun snoreCountIncrease(callBack: (() -> Unit)?) {
        this.snoreCountIncreaseCallBack = callBack
    }

    private var lastIndex: Boolean = false
    private var dataFlowMaxCount = 0
    override fun setLastIndexCk(data: Boolean) {
        this.lastIndex = data
    }

    override fun getFirmwareVersion() = callbackFlow {
        _sbSensorInfo.value.bluetoothGatt?.let { writeResponse(it, AppToModuleResponse.FirmwareOperate) }
        withTimeoutOrNull(3000L) {
            _sbSensorFirmwareInfo.collectLatest {
                trySend(it)
                cancel()
                delay(100)
                close()
            }
        } ?: run {
            trySend(null)
            close()
            cancel()
        }
        awaitClose()
    }

    override fun getDataFlowMaxCount(): Int {
        return dataFlowMaxCount
    }

    override var uploadCallback: (() -> Unit)? = null
    override fun setOnUploadCallback(callback: (() -> Unit)?) {
        uploadCallback = callback
    }

    override fun operateDeleteSbSensor(isAllDelete: Boolean) {
        writeData(_sbSensorInfo.value.bluetoothGatt, if (isAllDelete) AppToModule.OperateDeleteAll else AppToModule.OperateDeleteSector) { state ->
            _sbSensorInfo.update { it.copy(bluetoothState = state) }
            logHelper.insertLog(state)
        }
    }


    override fun startNetworkSpO2Sensor() {
        // TODO write command
    }

    override fun startNetworkEEGSensor() {
        // TODO write command
    }

    private fun sendDownloadContinue(gatt: BluetoothGatt, innerData: MutableStateFlow<BluetoothInfo>) {
        if (::sendDownloadContinueJob.isInitialized) {
            sendDownloadContinueJob.cancel()
        }
        sendDownloadContinueJob = logCoroutine.launch {
            logHelper.insertLog("sendDownloadContinue 등록")
            while (true) {
                delay(300000)
                isConnect(gatt, innerData) { isConnected ->
                    if (isConnected.not()) {
                        return@isConnect
                    }
                    // Todo: callback 형태에서는  상태를 변경해야되기 때문에 상태 변경시  기기 연결 해제 일경우  문제가 발생 되기 때문에 Response 형태로 요청함
                    writeResponse(gatt, AppToModuleResponse.OperateDownloadJob)
                    logHelper.insertLog("OperateDownloadJob")

                }
            }
        }
    }

    private suspend fun isConnect(gatt: BluetoothGatt, innerData: MutableStateFlow<BluetoothInfo>, callback: ((Boolean) -> Unit)) {
        if (isSBSensorConnect.value.first) {
            callback.invoke(true)
            return
        }
        innerData.update { it.copy(bluetoothState = BluetoothState.DisconnectedNotIntent) }
        sendDownloadContinueCancel()
        val isSuccessfully = gatt.connect()
        if (isSuccessfully) {
            logHelper.insertLog("isConnect -> 요청 성공")
            callback.invoke(true)
            delay(100)
        } else {
            delay(10000)
            if (innerData.value.bluetoothState == BluetoothState.DisconnectedNotIntent) {
                logHelper.insertLog("isConnect -> 요청 실패 재귀")
                isConnect(gatt, innerData, callback)
            }
        }
    }

    override fun sendDownloadContinueCancel() {
        if (::sendDownloadContinueJob.isInitialized) {
            sendDownloadContinueJob.cancel()
            logHelper.insertLog("sendDownloadContinue 등록 취소")
        }
    }

    override fun setDataId(dataId: Int) {
        _sbSensorInfo.update { it.copy(dataId = dataId) }
    }

    private fun writeResponse(gatt: BluetoothGatt, command: AppToModuleResponse) {
        val cmd = BluetoothUtils.findCommandCharacteristic(gatt) ?: return
        logCoroutine.launch {
            val byteArr = encryptByteArray(isEncrypt, command.getCommandByteArr())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(cmd, byteArr, WRITE_TYPE_DEFAULT)
            } else {
                cmd.value = byteArr
                gatt.writeCharacteristic(cmd)
            }

            Log.d("<--- App To Device", command.getCommandByteArr().hexToString())
        }

    }

    private fun writeData(gatt: BluetoothGatt?, command: AppToModule, stateCallback: ((BluetoothState) -> Unit)?) {
        gatt?.let {
            stateCallback?.invoke(command.getState())

            val cmd = BluetoothUtils.findCommandCharacteristic(gatt)
            if (cmd == null) {
                stateCallback?.invoke(BluetoothState.Connected.Ready)
                return
            }
            logCoroutine.launch {
                val byteArr = encryptByteArray(isEncrypt, command.getCommandByteArr())
//            cmd.value = byteArr
                var tryCount = 0

                var result: Boolean
                do {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val writeResult = gatt.writeCharacteristic(cmd, byteArr, WRITE_TYPE_DEFAULT)
                        result = when (writeResult) {
                            BluetoothStatusCodes.SUCCESS -> {
                                if (tryCount != 0) {
                                    logHelper.insertLog("${command.getName()} =  SUCCESS ")
                                }
                                true
                            }

                            BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND -> {
                                if (tryCount <= 2) {
                                    logHelper.insertLog("ERROR_PROFILE_SERVICE_NOT_BOUND")
                                    tryCount++
                                }
                                errorProfile(gatt, command, tryCount).first()
                            }

                            else -> {
                                delay(1000)
                                if (tryCount <= 2) {
                                    logHelper.insertLog("${command.getName()} = writeError: $writeResult")
                                    tryCount++
                                }
                                false
                            }
                        }
                    } else {
                        cmd.value = byteArr
                        result = gatt.writeCharacteristic(cmd)
                    }
                    if (result) {
                        Log.d("<--- App To Device", command.getCommandByteArr().hexToString())
                    }

                } while (!result)
            }
        } ?: stateCallback?.invoke(BluetoothState.DisconnectedNotIntent)
    }

    private fun errorProfile(gatt: BluetoothGatt, module: AppToModule, count: Int) = callbackFlow {
        gatt.disconnect()
        if (count <= 2) {
            logHelper.insertLog("gatt disconnect 됨")
            logHelper.insertLog("AppToModule = ${module}")
        }

        isSBSensorConnect.collectLatest { (isConnect, name) ->
            run {
                if (isConnect.not()) {
                    if (count <= 2) {
                        logHelper.insertLog("gatt disconnect 됨")
                    }
                    val isSuccessfully = gatt.connect()
                    if (isSuccessfully) {
                        send(false)
                        Log.e(TAG, "errorProfile: gatt connect 됨")
                        cancel()
                        return@collectLatest
                    }
                    if (count <= 2) {
                        logHelper.insertLog("gatt connect 시도")
                    }

                }
            }
        }
        awaitClose()
    }

    override fun stopNetworkSpO2Sensor() {}

    override fun stopNetworkEEGSensor() {}
    override fun startMotorTest(intensity: Int) {
        val module = when (intensity) {
            2 -> {
                AppToModule.OperateMotorTestStrong
            }

            1 -> {
                AppToModule.OperateMotorTestNormal
            }

            else -> {
                AppToModule.OperateMotorTestWeak
            }
        }
        if (_sbSensorInfo.value.bluetoothState != BluetoothState.Unregistered
            && _sbSensorInfo.value.bluetoothState != BluetoothState.Connected.ReceivingRealtime
            || _sbSensorInfo.value.bluetoothState != BluetoothState.Connected.DataFlow
        ) {
            writeData(_sbSensorInfo.value.bluetoothGatt, module) { state ->
                logHelper.insertLog(state)
            }
        }
    }

    override fun callVibrationNotifications(intensity: Int) {
        val module = when (intensity) {
            2 -> {
                AppToModule.VibrationNotificationsStrong
            }

            1 -> {
                AppToModule.VibrationNotificationsNormal
            }

            else -> {
                AppToModule.VibrationNotificationsWeak
            }
        }
        if (sbSensorInfo.value.sleepType == SleepType.NoSering) {
            if (_sbSensorInfo.value.bluetoothState != BluetoothState.Unregistered) {
                writeData(_sbSensorInfo.value.bluetoothGatt, module) { state ->
                    _sbSensorInfo.update { it.copy(bluetoothState = state) }
                    logHelper.insertLog(state)
                }
            }

        }

    }

    private fun encryptByteArray(isEncrypt: Boolean, value: ByteArray): ByteArray {
//        Log.d(TAG, "encryptByteArray: $isEncrypt")
//        Log.d(TAG, "encryptByteArray: ${value.hexToString()}")
        return if (isEncrypt.not()) aesHelper.encryptAES128(value) else value
    }

    private fun decryptByteArray(value: ByteArray): ByteArray {
        isEncrypt = value.hexToString().prefixToHex() == defaultPrefix
//        Log.d(TAG, "value: ${value.hexToString().prefixToHex()}")
//        Log.d(TAG, "encryptPrefix: ${defaultPrefix}")
//        Log.d(TAG, "decryptByteArray: $isEncrypt")
        return if (isEncrypt.not()) aesHelper.decryptAES128(value) else value
    }


    override fun getGattCallback(sbBluetoothDevice: SBBluetoothDevice, bluetoothState: BluetoothState): BluetoothGattCallback = getCallback(sbBluetoothDevice, bluetoothState)


    //////////////////////////////////////////////////////
    /////                                            /////
    /////           BluetoothGattCallback            /////
    /////                                            /////
    //////////////////////////////////////////////////////
    private fun getCallback(sbBluetoothDevice: SBBluetoothDevice, bluetoothState: BluetoothState) = object : BluetoothGattCallback() {
        private val UPLOAD_COUNT_INTERVAL = 300 * 3
        private val DATA_INTERVAL = 9

        private var uploadCallbackQuotient = -1

        private var safetyMode = 0
        private val SAFETY_STANDARD = 50

        private val DOWNLOAD_RETRY_INTERVAL = 5
        private val DOWNLOAD_RETRY_COUNT = 3
        private var downloadContinueCount = 0
        private var dataFlowCurrentCount = 0


        private val innerData = when (sbBluetoothDevice) {
            SBBluetoothDevice.SB_SOOM_SENSOR -> _sbSensorInfo
            SBBluetoothDevice.SB_SPO2_SENSOR -> _spo2SensorInfo
            SBBluetoothDevice.SB_EEG_SENSOR -> _eegSensorInfo
        }

        private val accFormatter = DecimalFormat("#.####").apply { roundingMode = RoundingMode.HALF_UP }

        //        private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        private var startTime: Long = 0L
        private val coroutine = CoroutineScope(Dispatchers.IO)

        init {
            Log.d(TAG, "getCallback: Connecting ")
            val result = innerData.updateAndGet {
                it.copy(bluetoothState = bluetoothState)
            }
            logHelper.insertLog("BluetoothNetwork = ${result.bluetoothState}")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status == BluetoothGatt.GATT_FAILURE) {
                logHelper.insertLog("onConnectionStateChange: GATT_FAILURE ${gatt.device.name} - ${gatt.device.address}")
                logCoroutine.launch {
                    isSBSensorConnect.emit(Pair(false, gatt.device.name))
                }
                disconnectedDevice(gatt)
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                logHelper.insertLog("onConnectionStateChange: NOT GATT_SUCCESS status = $status -${gatt.device.name} - ${gatt.device.address}")
                logCoroutine.launch {
                    isSBSensorConnect.emit(Pair(false, gatt.device.name))
                }
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHORIZATION) {
                    innerData.update { it.copy(bluetoothState = BluetoothState.DisconnectedNotIntent) }
                    val isSuccessfully = gatt.connect()
                    logHelper.insertLog("onConnectionStateChange: NOT GATT AUTHORIZATION connect = $isSuccessfully ")
                    return
                }
                disconnectedDevice(gatt)
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                logHelper.insertLog("onConnectionStateChange: CONNECTED ${gatt.device.name} - ${gatt.device.address}")
//                BluetoothNetworkRepository.gatt = gatt
                // FIXME: 살려줘
                /*coroutine.launch {
                    val deviceName = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
                    val deviceAddress = dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
                    Log.d(TAG, "onConnectionStateChange: ${deviceName}")
                    Log.d(TAG, "onConnectionStateChange: ${deviceAddress}")
                    if (deviceName.isNullOrEmpty() &&
                        deviceAddress.isNullOrEmpty()) {
                        Log.d(TAG, "onConnectionStateChange: 탄다탄다 33333333")
                        //연결 끊기
                        gatt.close()
                        delay(1500)
                        _sbSensorInfo.value.bluetoothState = BluetoothState.Unregistered

                        // 복구
                        *//*_sbSensorInfo.value.bluetoothGatt = gatt
                        _sbSensorInfo.value.bluetoothName = gatt.device.name
                        _sbSensorInfo.value.bluetoothAddress = gatt.device.address
                        coroutine.launch {
                            dataManager.saveBluetoothDevice(SBBluetoothDevice.SB_SOOM_SENSOR.type.name,gatt.device.name, gatt.device.address)
                        }*//*
                    }
                }*/
                reConnectCount = 0
                gatt.discoverServices()
                innerData.update { it.copy(bluetoothGatt = gatt) }
                logCoroutine.launch {
                    isSBSensorConnect.emit(Pair(true, gatt.device.name))
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logHelper.insertLog("onConnectionStateChange: DISCONNECTED ${gatt.device.name} - ${gatt.device.address}")
                logCoroutine.launch {
                    isSBSensorConnect.emit(Pair(false, gatt.device.name))
                }
                disconnectedDevice(gatt)
                return
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[NR] onServicesDiscovered: NOT GATT_SUCCESS  status = $status - ${gatt.device.name} / ${gatt.device.address}")
                disconnectedDevice(gatt)
                return
            }

            Log.d(TAG, "[NR] onServicesDiscovered: SUCCESS ${gatt.device.name} / ${gatt.device.address}")
            startNotification(gatt)
        }

        private fun startNotification(bleGatt: BluetoothGatt) {
            // find command characteristics from the GATT server
            val respCharacteristic = BluetoothUtils.findResponseCharacteristic(bleGatt)

            if (respCharacteristic == null) {
                Log.d(TAG, "[NR] orespCharacteristic")
                disconnectedDevice(bleGatt)
                return
            }

            // READ
            bleGatt.setCharacteristicNotification(respCharacteristic, true)
            // UUID for notification
            val descriptor: BluetoothGattDescriptor = respCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))

            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            innerData.value.bluetoothGatt?.writeDescriptor(descriptor)
        }

        private fun stopNotification(bleGatt: BluetoothGatt) {
            val respCharacteristic = bleGatt.let { BluetoothUtils.findResponseCharacteristic(it) }
            bleGatt.setCharacteristicNotification(respCharacteristic, false)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return
            }
            super.onCharacteristicChanged(gatt, characteristic)
            readData(gatt, characteristic.value)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            readData(gatt, value)
        }

        private fun readData(gatt: BluetoothGatt, readValue: ByteArray) {
            logCoroutine.launch {
//                Log.d("---> Device To App", readValue.hexToString())
                val value = decryptByteArray(readValue)
                Log.d("---> Device To App1", value.hexToString())

//            Log.d("--- Current State", "${(String.format("%02X", value[4])).getCommand()}")
                when ((String.format("%02X", value[4])).getCommand()) {
                    ModuleToApp.StartStopACK, ModuleToApp.NoSeringStopACK -> {
                        innerData.value.let { info ->
                            when (info.bluetoothState) {
                                BluetoothState.Connected.SendStart -> {
                                    uploadCallbackQuotient = 0
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.WaitStart) }
//                                it.bluetoothState = BluetoothState.Connected.WaitStart
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> WaitStart")
                                }

                                BluetoothState.Connected.SendStop -> {
                                    uploadCallbackQuotient = -1
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.Finish) }
//                                it.bluetoothState = BluetoothState.Connected.Finish
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> Finish")
                                    sendDownloadContinueCancel()
                                    stopCallBack?.invoke()
                                }

                                BluetoothState.Connected.DataFlow -> {
//                                writeData(gatt, AppToModule.OperateDeleteSector, null)
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.Ready) }
//                                it.bluetoothState = BluetoothState.Connected.SendDelete
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> Ready")
                                }

                                else -> {

                                }
                            }
                        }
                    }

                    ModuleToApp.RealtimeData -> {
                        innerData.value.let { info ->
                            when (info.bluetoothState) {
                                BluetoothState.Connected.WaitStart -> {
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> ReceivingRealtime")

                                    startTime = System.currentTimeMillis()
                                    sendDownloadContinue(gatt, innerData)
                                }

                                BluetoothState.Connected.ReceivingDelayed, BluetoothState.Connected.Reconnected -> {
                                    safetyMode = 0
                                    uploadCallbackQuotient = 0
                                    sendDownloadContinue(gatt, innerData)
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
                                    logHelper.insertLog("${info.bluetoothState} -> ReceivingRealtime")
                                }

                                BluetoothState.Connected.Init,
                                BluetoothState.Connected.Ready -> {
                                    Log.d(TAG, "DATAID1111111: ${innerData.value.realData.value?.dataId}")
                                    innerData.value.realData.value?.let {
                                        innerData.update { it.copy(bluetoothState = BluetoothState.Connected.DataFlow) }
                                        setDataFlow(false)
                                        //                                it.bluetoothState = BluetoothState.Connected.DataFlow
                                        //                                innerData.tryEmit(it)
                                        logHelper.insertLog("${info.bluetoothState} -> BluetoothState.Connected.DataFlow")

                                    }
                                }

                                BluetoothState.Connected.DataFlow -> {
                                    writeData(_sbSensorInfo.value.bluetoothGatt, AppToModule.OperateDataFlowDownload) { state ->
                                        _sbSensorInfo.update { it.copy(bluetoothState = state) }
                                        setDataFlow(true, dataFlowCurrentCount, dataFlowMaxCount)
                                        logHelper.insertLog(state)
                                    }
                                }

                                /*BluetoothState.Connected.DataFlowUploadFinish -> {
                                    dataFlowCallback?.invoke()
                                    setDataFlow(true, dataFlowCurrentCount, dataFlowMaxCount)
                                    coroutine.launch {
                                        launch {
                                            settingDataRepository.getSleepType().let {
                                                when (it) {
                                                    SleepType.NoSering.name -> {
                                                        writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.NoSeringOperateStop, stateCallback = null)
                                                        Log.d(TAG, "DataFlow: 코골이 종료 ")
                                                    }

                                                    else -> {
                                                        writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.BreathingOperateStop, stateCallback = null)
                                                        Log.d(TAG, "DataFlow: 호흡 종료 ")
                                                    }
                                                }
                                            }
                                            *//*?: launch {
                                            // FIXME: 하드웨어와 DataFlow 상황에서 강제종료에 대해 논의해야함.!! 중요!!
                                            writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.BreathingOperateStop, stateCallback = null)
                                            delay(1000)
                                            writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.NoSeringOperateStop, stateCallback = null)
                                        }*//*

                                        }
                                    }
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.Ready) }
//                                it.bluetoothState = BluetoothState.Connected.DataFlow
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> BluetoothState.Connected.DataFlow")
                                }*/

                                BluetoothState.Connected.SendDownloadContinue -> {
                                    downloadContinueCount++
                                    if (downloadContinueCount % DOWNLOAD_RETRY_INTERVAL == 0) {
                                        if (downloadContinueCount >= DOWNLOAD_RETRY_COUNT * DOWNLOAD_RETRY_INTERVAL) {
                                            innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                        it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                        innerData.tryEmit(it)
                                            logHelper.insertLog("${info.bluetoothState} -> ReceivingRealtime")
                                            downloadContinueCount = 0
                                        } else {
                                            writeData(gatt, AppToModule.OperateDownloadContinue) { state ->
                                                _sbSensorInfo.value.let { info ->
                                                    innerData.update { it.copy(bluetoothState = state) }
//                                                info.bluetoothState = state
//                                                _sbSensorInfo.tryEmit(info)
                                                    logHelper.insertLog(state)
                                                }
                                            }
                                        }
                                    }
                                }

                                BluetoothState.Connected.ReceivingRealtime -> {
                                    safetyMode++
                                    // Do Nothing
                                }

                                else -> {
                                    // 상태 이상
                                    // Log.e("---> Device To App", "RealtimeData Receive State Error : ${it.bluetoothState}")
                                }
                            }
//                            Log.e(TAG, "readData: isRealDataRemoved = ${innerData.value.realData.value}")
//                            Log.e(TAG, "id1 = ${(innerData.value.realData.value?.dataId ?: -1)}")
//                            Log.e(TAG, "id2 = ${innerData.value.dataId}")
//                            Log.e(TAG, "상태 state = ${innerData.value.bluetoothState}")
                            val check = (innerData.value.bluetoothState == BluetoothState.Connected.ReceivingRealtime) &&
                                    (innerData.value.realData.value == null ||
                                            (innerData.value.realData.value?.dataId?.toInt() ?: -1) == innerData.value.dataId)
                            if (check) {
                                if (value.verifyCheckSum()) {
                                    coroutine.launch {
                                        val index1 = String.format("%02X%02X%02X", value[6], value[7], value[8]).toUInt(16).toInt()
                                        val capacitance1 = String.format("%02X%02X%02X", value[9], value[10], value[11]).toUInt(16).toInt()

                                        val accelerationX1 = String.format("%02X", value[12]).toUInt(16).toInt()
                                        val accelerationY1 = String.format("%02X", value[13]).toUInt(16).toInt()
                                        val accelerationZ1 = String.format("%02X", value[14]).toUInt(16).toInt()

                                        val calcAccX1 = accFormatter.format((accelerationX1.toByte() * 0.0156F))
                                        val calcAccY1 = accFormatter.format((accelerationY1.toByte() * 0.0156F))
                                        val calcAccZ1 = accFormatter.format((accelerationZ1.toByte() * 0.0156F))

                                        val time1 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format((startTime + (200 * index1)))
                                        info.currentData.emit(capacitance1)
                                        //                                info.currentData?.postValue(capacitance1)

                                        val index2 = String.format("%02X%02X%02X", value[15], value[16], value[17]).toUInt(16).toInt()
                                        val capacitance2 = String.format("%02X%02X%02X", value[18], value[19], value[20]).toUInt(16).toInt()

                                        val accelerationX2 = String.format("%02X", value[21]).toUInt(16).toInt()
                                        val accelerationY2 = String.format("%02X", value[22]).toUInt(16).toInt()
                                        val accelerationZ2 = String.format("%02X", value[23]).toUInt(16).toInt()

                                        val calcAccX2 = accFormatter.format((accelerationX2.toByte() * 0.0156F))
                                        val calcAccY2 = accFormatter.format((accelerationY2.toByte() * 0.0156F))
                                        val calcAccZ2 = accFormatter.format((accelerationZ2.toByte() * 0.0156F))

                                        val time2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format((startTime + (200 * index2)))

                                        //                                info.currentData?.postValue(capacitance2)
                                        info.currentData.emit(capacitance2)
                                        val quotient = index1 / UPLOAD_COUNT_INTERVAL
                                        if (safetyMode >= SAFETY_STANDARD && uploadCallbackQuotient > -1 && quotient > uploadCallbackQuotient) {
                                            uploadCallback?.let { cb ->
                                                safetyMode = 0
                                                uploadCallbackQuotient = quotient
                                                cb.invoke()
                                            }
                                        }

                                        info.channel.apply {
                                            emit(SBSensorData(index1, time1, capacitance1, calcAccX1, calcAccY1, calcAccZ1, info.dataId ?: -1))
                                            emit(SBSensorData(index2, time2, capacitance2, calcAccX2, calcAccY2, calcAccZ2, info.dataId ?: -1))
                                        }
                                    }
                                    writeResponse(gatt, AppToModuleResponse.RealtimeDataResponseACK)
                                } else {
                                    writeResponse(gatt, AppToModuleResponse.RealtimeDataResponseNAK)
                                }
                            }
                        }
                    }

                    ModuleToApp.DelayedData -> {
                        innerData.value.let { info ->
                            when (info.bluetoothState) {
                                BluetoothState.Connected.WaitStart -> {
                                    val minusLength = String.format("%02X", value[5]).toUInt(16).toInt() / DATA_INTERVAL
                                    startTime = System.currentTimeMillis() - (minusLength * 200)
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingDelayed) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingDelayed
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> ReceivingDelayed")
                                }

                                BluetoothState.Connected.ReceivingRealtime -> {
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingDelayed) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingDelayed
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> ReceivingDelayed")
                                }

                                BluetoothState.Connected.ReceivingDelayed -> {
                                    // Do Nothing
                                }

                                else -> {
                                    // 상태 이상
                                    //Log.e("---> Device To App", "DelayedData Receive State Error : ${it.bluetoothState}")
                                }
                            }

                            if (value.verifyCheckSum()) {
                                coroutine.launch {
                                    val length = String.format("%02X", value[5]).toUInt(16).toInt() / DATA_INTERVAL
                                    for (i in 0 until length) {
                                        // Index O
                                        val index = String.format("%02X%02X%02X", value[i * DATA_INTERVAL + 6], value[i * DATA_INTERVAL + 7], value[i * DATA_INTERVAL + 8]).toUInt(16).toInt()
                                        val capacitance = String.format("%02X%02X%02X", value[i * DATA_INTERVAL + 9], value[i * DATA_INTERVAL + 10], value[i * DATA_INTERVAL + 11]).toUInt(16).toInt()

                                        val accelerationX = String.format("%02X", value[i * DATA_INTERVAL + 12]).toUInt(16).toInt()
                                        val accelerationY = String.format("%02X", value[i * DATA_INTERVAL + 13]).toUInt(16).toInt()
                                        val accelerationZ = String.format("%02X", value[i * DATA_INTERVAL + 14]).toUInt(16).toInt()

                                        val calcAccX = accFormatter.format((accelerationX.toByte() * 0.0156F))
                                        val calcAccY = accFormatter.format((accelerationY.toByte() * 0.0156F))
                                        val calcAccZ = accFormatter.format((accelerationZ.toByte() * 0.0156F))

                                        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format((startTime + (200 * index)))

                                        info.channel.emit(SBSensorData(index, time, capacitance, calcAccX, calcAccY, calcAccZ, info.dataId ?: -1))
                                    }
                                }
                                writeResponse(gatt, AppToModuleResponse.DelayedDataResponseACK)
                            } else {
                                writeResponse(gatt, AppToModuleResponse.DelayedDataResponseNAK)
                            }
                        }
                    }

                    ModuleToApp.MOTCtrlSetACK -> {
                        innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
                        logHelper.insertLog("코골이 동작 피드백")
                    }

                    ModuleToApp.MotorTestACK -> {
                        logHelper.insertLog("테스트 코골이 동작 피드백")
                    }

                    ModuleToApp.OperateACK -> {
                        if (value.verifyCheckSum()) {
                            innerData.value.let { info ->
                                when (info.bluetoothState) {
                                    BluetoothState.Connected.SendRealtime -> {
                                        innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                    it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                    innerData.tryEmit(it)
                                        logHelper.insertLog("${info.bluetoothState} -> ReceivingRealtime")
                                    }

                                    BluetoothState.Connected.SendDelayed -> {
                                        innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingDelayed) }
//                                    it.bluetoothState = BluetoothState.Connected.ReceivingDelayed
//                                    innerData.tryEmit(it)
                                        logHelper.insertLog("${info.bluetoothState} -> ReceivingDelayed")
                                    }

                                    else -> {
                                        //Log.e("---> Device To App", "OperateACK Receive State Error : ${it.bluetoothState}")
                                    }
                                }
                            }
                        }
                    }

                    ModuleToApp.MemoryData -> {
                        innerData.value.let {
                            when (it.bluetoothState) {
                                BluetoothState.Connected.SendDownload, BluetoothState.Connected.SendDownloadContinue -> {
                                    downloadContinueCount = 0
                                }

                                else -> {
                                    //Log.e("---> Device To App", "MemoryData Receive State Error : ${it.bluetoothState}")
                                }
                            }

                            if (value.verifyCheckSum()) {
                                coroutine.launch {
                                    val length = String.format("%02X", value[5]).toUInt(16).toInt() / DATA_INTERVAL
                                    for (i in 0 until length) {
                                        // Index O
                                        val index = String.format("%02X%02X%02X", value[i * DATA_INTERVAL + 6], value[i * DATA_INTERVAL + 7], value[i * DATA_INTERVAL + 8]).toUInt(16).toInt()
                                        val capacitance = String.format("%02X%02X%02X", value[i * DATA_INTERVAL + 9], value[i * DATA_INTERVAL + 10], value[i * DATA_INTERVAL + 11]).toUInt(16).toInt()

                                        val accelerationX = String.format("%02X", value[i * DATA_INTERVAL + 12]).toUInt(16).toInt()
                                        val accelerationY = String.format("%02X", value[i * DATA_INTERVAL + 13]).toUInt(16).toInt()
                                        val accelerationZ = String.format("%02X", value[i * DATA_INTERVAL + 14]).toUInt(16).toInt()

                                        val calcAccX = accFormatter.format((accelerationX.toByte() * 0.0156F))
                                        val calcAccY = accFormatter.format((accelerationY.toByte() * 0.0156F))
                                        val calcAccZ = accFormatter.format((accelerationZ.toByte() * 0.0156F))

                                        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format((startTime + (200 * index)))

//                                        Log.d(TAG, "onCreate: SEND ${it.dataId} ")
                                        it.channel.emit(SBSensorData(index, time, capacitance, calcAccX, calcAccY, calcAccZ, it.dataId ?: -1))
                                    }
                                }
                                //writeResponse(gatt, AppToModuleResponse.DelayedDataResponseACK)
                            } else {
                                //writeResponse(gatt, AppToModuleResponse.DelayedDataResponseNAK)
                            }
                        }
                    }

                    ModuleToApp.MemoryDataACK -> {
                        innerData.value.let { info ->
                            when (info.bluetoothState) {
                                BluetoothState.Connected.SendDownload -> {
                                    downloadContinueCount = 0
                                    lastDownloadCompleteCallback?.invoke(BLEService.FinishState.FinishNormal)
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.FinishDownload) }
                                    Log.d(TAG, "readData: finish 먼저!!")
//                                it.bluetoothState = BluetoothState.Connected.FinishDownload
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> FinishDownload")
                                }

                                BluetoothState.Connected.SendDownloadContinue -> {
                                    downloadContinueCount = 0
                                    //                                downloadCompleteCallback?.invoke()
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                innerData.tryEmit(it)
                                    logHelper.insertLog("${info.bluetoothState} -> ReceivingRealtime")
                                }

                                BluetoothState.Connected.DataFlow -> {
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.DataFlowUploadFinish) }
                                }


                                else -> {
                                    // 상태 이상
                                    //Log.e("---> Device To App", "MemoryDataACK Receive State Error : ${it.bluetoothState}")
                                }
                            }
                            if (value.verifyCheckSum()) {
                                val memoryTotalIndex = String.format("%02X%02X", value[6], value[7]).toUInt(16).toInt()
                                logHelper.insertLog("총 받 갯수 ${memoryTotalIndex * 20}")
                                dataFlowMaxCount = memoryTotalIndex * 20
                                writeResponse(gatt, AppToModuleResponse.MemoryDataResponseACK)

                                delay(2000)
                                when (innerData.value.bluetoothState) {
                                    BluetoothState.Connected.DataFlowUploadFinish -> {
                                        dataFlowCallback?.invoke()
                                        setDataFlow(true, dataFlowCurrentCount, dataFlowMaxCount)
                                        coroutine.launch {
                                            launch {
                                                settingDataRepository.getSleepType().let {
                                                    when (it) {
                                                        SleepType.NoSering.name -> {
                                                            writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.NoSeringOperateStop, stateCallback = null)
                                                            Log.d(TAG, "DataFlow: 코골이 종료 ")
                                                        }

                                                        else -> {
                                                            writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.BreathingOperateStop, stateCallback = null)
                                                            Log.d(TAG, "DataFlow: 호흡 종료 ")
                                                        }
                                                    }
                                                }
                                                /*?: launch {
                                                // FIXME: 하드웨어와 DataFlow 상황에서 강제종료에 대해 논의해야함.!! 중요!!
                                                writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.BreathingOperateStop, stateCallback = null)
                                                delay(1000)
                                                writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.NoSeringOperateStop, stateCallback = null)
                                            }*/

                                            }
                                        }
                                        innerData.update { it.copy(bluetoothState = BluetoothState.Connected.Ready) }
//                                it.bluetoothState = BluetoothState.Connected.DataFlow
//                                innerData.tryEmit(it)
                                        logHelper.insertLog("${info.bluetoothState} -> BluetoothState.Connected.DataFlow")
                                    }

                                    else -> {}
                                }
                            } else {
                                writeResponse(gatt, AppToModuleResponse.MemoryDataResponseNAK)
                            }
                        }
                    }

                    ModuleToApp.MemoryDataDeleteACK -> {
                        val result = innerData.updateAndGet { it.copy(bluetoothState = BluetoothState.Connected.Ready) }
                        logHelper.insertLog(result.bluetoothState)
//                    innerData.value?.let {
//                        it.bluetoothState = BluetoothState.Connected.Ready
//                        innerData.tryEmit(it)
//                        insertLog(it.bluetoothState)
//                    }
                    }

                    ModuleToApp.PowerOff -> {
                        val data = String.format("%02X", value[6])
                        lastDownloadCompleteCallback?.invoke(
                            if (data == "01") BLEService.FinishState.FinishPowerOff
                            else BLEService.FinishState.FinishBatteryLow
                        )
                        logHelper.insertLog(
                            if (data == "01") "FinishPowerOff"
                            else "FinishBatteryLow"
                        )
                        downloadContinueCount = 0
                        writeResponse(gatt, AppToModuleResponse.PowerOffACK)
                        innerData.update { it.copy(canMeasurement = false) }
                    }

                    ModuleToApp.Error -> {
                        // Do Nothing ???
                    }

                    ModuleToApp.BatteryState -> {
                        val data = String.format("%02X", value[6])
                        val result = Integer.parseInt(data, 16)
                        Log.e(TAG, "BatteryState: ${result}")
                        innerData.value.let { info ->
                            when (info.bluetoothState) {
                                BluetoothState.Connecting -> {
                                    innerData.update {
                                        it.copy(
                                            batteryInfo = result.toString(), canMeasurement = result > 20,
                                            bluetoothState = BluetoothState.Connected.Reconnected
                                        )
                                    }
                                }

                                else -> {
                                    innerData.update {
                                        it.copy(
                                            batteryInfo = result.toString(), canMeasurement = result > 20,
                                            bluetoothState = if (it.bluetoothState == BluetoothState.Connected.Init
                                                || it.bluetoothState == BluetoothState.DisconnectedByUser
                                            ) BluetoothState.Connected.Ready
                                            else it.bluetoothState
                                        )
                                    }

                                }
                            }
                        }
                    }

                    ModuleToApp.MOTCData -> {
                        snoreCountIncreaseCallBack?.invoke()
                        writeResponse(gatt, AppToModuleResponse.MOTCDataSetACK)
                    }

                    ModuleToApp.FirmwareVersion -> {
                        if (value.verifyCheckSum()) {
                            coroutine.launch {
                                val major = String.format("%02X", value[6]).toUInt(16).toInt()
                                val minor = String.format("%02X", value[7]).toUInt(16).toInt()
                                val patch = String.format("%02X", value[8]).toUInt(16).toInt()
                                val firmware = major.toString().plus(".").plus(minor.toString().plus(".").plus(patch.toString()))
                                _sbSensorFirmwareInfo.emit(FirmwareData(firmware, innerData.value.bluetoothName ?: "", innerData.value.bluetoothAddress ?: ""))
                                Log.e(TAG, "version: major =$major minor = $minor patch = $patch")
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}


