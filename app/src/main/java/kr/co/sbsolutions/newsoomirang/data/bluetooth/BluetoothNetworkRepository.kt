package kr.co.sbsolutions.newsoomirang.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.BluetoothUtils
import kr.co.sbsolutions.newsoomirang.common.Cons.CLIENT_CHARACTERISTIC_CONFIG
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogWorkerHelper
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.LogDBDataRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.soomirang.db.LogData
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BluetoothNetworkRepository @Inject constructor(
    private val dataManager: DataManager,
    private val settingDataRepository: SettingDataRepository,
    private val logWorkerHelper: LogWorkerHelper,
) : IBluetoothNetworkRepository {
    private val strBuilder = StringBuilder()

    private val logCoroutine = CoroutineScope(Dispatchers.IO)

    private val _sbSensorInfo = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR))
    override val sbSensorInfo: StateFlow<BluetoothInfo> = _sbSensorInfo.asStateFlow()
    private val _spo2SensorInfo = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SPO2_SENSOR))
    override val spo2SensorInfo: StateFlow<BluetoothInfo> = _spo2SensorInfo.asStateFlow()
    private val _eegSensorInfo = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_EEG_SENSOR))
    override val eegSensorInfo: StateFlow<BluetoothInfo> = _eegSensorInfo.asStateFlow()
    override suspend fun listenRegisterSBSensor() {

        dataManager.getBluetoothDeviceName(_sbSensorInfo.value.sbBluetoothDevice.type.toString())
            .zip(dataManager.getBluetoothDeviceAddress(_sbSensorInfo.value.sbBluetoothDevice.type.toString()))
            { name, address ->
                _sbSensorInfo.update { it.copy(bluetoothName = name, bluetoothAddress = address) }
                !name.isNullOrEmpty() && !address.isNullOrEmpty()
            }.collect { registered ->
                val result = _sbSensorInfo.updateAndGet { it.copy(bluetoothState = if (registered) BluetoothState.Registered else BluetoothState.Unregistered) }
                insertLog(result.bluetoothState)
//                _sbSensorInfo.value.let {
//                    it.bluetoothState = if (registered) BluetoothState.Registered else BluetoothState.Unregistered
//                    _sbSensorInfo.tryEmit(it)
//                    insertLog(it.bluetoothState)
//                }
            }


    }

    override fun setSBSensorCancel(isCancel: Boolean) {
        _sbSensorInfo.update {
            it.copy(cancelCheck = isCancel)
        }
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
            _sbSensorInfo.value.bluetoothAddress -> {
                _sbSensorInfo
            }

            _spo2SensorInfo.value.bluetoothAddress -> {
                _spo2SensorInfo
            }

            _eegSensorInfo.value.bluetoothAddress -> {
                _eegSensorInfo
            }

            else -> {
                return
            }
        }.apply {
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
            insertLog(result.bluetoothState)
        }
    }

    private fun disconnectedDevice(gatt: BluetoothGatt) {
        when (gatt.device.address) {
            _sbSensorInfo.value.bluetoothAddress -> {
                _sbSensorInfo
            }

            _spo2SensorInfo.value.bluetoothAddress -> {
                _spo2SensorInfo
            }

            _eegSensorInfo.value.bluetoothAddress -> {
                _eegSensorInfo
            }

            else -> {
                Log.d(TAG, "disconnectedDevice = 없음")
                return
            }
        }.apply {
            value.let { bi ->
                when (bi.bluetoothState) {
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
                    BluetoothState.Connected.MotCtrlSet,
                    BluetoothState.Connected.WaitStart -> {
                        update { it.copy(bluetoothState = BluetoothState.DisconnectedNotIntent) }
                        insertLog(BluetoothState.DisconnectedNotIntent)
                    }

                    else -> {
                        gatt.disconnect()
                        gatt.close()
                        Log.d(TAG, "disconnect = disconnect")
                        update { it.copy(bluetoothGatt = null, bluetoothState = BluetoothState.DisconnectedByUser) }
                        insertLog(BluetoothState.DisconnectedByUser)
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

        releaseResource()
    }

    override fun disconnectedDevice(sbBluetoothDevice: SBBluetoothDevice) {
        when (sbBluetoothDevice) {
            SBBluetoothDevice.SB_SOOM_SENSOR -> {
                _sbSensorInfo
            }

            SBBluetoothDevice.SB_SPO2_SENSOR -> {
                _spo2SensorInfo
            }

            SBBluetoothDevice.SB_EEG_SENSOR -> {
                _eegSensorInfo
            }
        }.apply {
            value.let { info ->
                if (info.bluetoothState != BluetoothState.Unregistered) {
                    val result = updateAndGet { it.copy(bluetoothState = BluetoothState.DisconnectedByUser) }
                    insertLog(result.bluetoothState)
                }
            }
        }
    }

    override fun releaseResource() {
        _sbSensorInfo.value.apply {
            if (bluetoothState != BluetoothState.Unregistered) {
                bluetoothState = BluetoothState.DisconnectedByUser
//                Log.d(TAG, "disconnectedDevice: 2")
            }
            bluetoothGatt?.let {
                it.setCharacteristicNotification(BluetoothUtils.findResponseCharacteristic(it), false)
                it.disconnect()
                it.close()
            }
            dataId = null
            bluetoothGatt = null
        }
        _spo2SensorInfo.value.apply {
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
        }
//
        _eegSensorInfo.value.apply {
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
        }
    }

    override fun startNetworkSBSensor(dataId: Int, sleepType: SleepType) {
        val module = if (sleepType == SleepType.Breathing) AppToModule.BreathingOperateStart else AppToModule.NoSeringOperateStart
        if (_sbSensorInfo.value.bluetoothState == BluetoothState.Unregistered) {
            _sbSensorInfo.update { it.copy(dataId = dataId, sleepType = sleepType, snoreTime = 0) }
        }else{
            writeData(_sbSensorInfo.value.bluetoothGatt, module) { state ->
                _sbSensorInfo.update { it.copy(dataId = dataId, bluetoothState = state, sleepType = sleepType, snoreTime = 0) }
                insertLog(state)
            }
        }

    }

    override fun stopNetworkSBSensor(snoreTime: Long) {
        val module = if (_sbSensorInfo.value.sleepType == SleepType.Breathing) AppToModule.BreathingOperateStop else AppToModule.NoSeringOperateStop
        writeData(_sbSensorInfo.value.bluetoothGatt, module) { state ->
            Log.d(TAG, "stopNetworkSBSensor snoreTime: $snoreTime")
            _sbSensorInfo.update { it.copy(bluetoothState = state, snoreTime = snoreTime) }
            insertLog(state)
        }
    }

    override fun endNetworkSBSensor(isForcedClose: Boolean) {
        val result = _sbSensorInfo.updateAndGet { it.copy(bluetoothState = if (isForcedClose) BluetoothState.Connected.ForceEnd else BluetoothState.Connected.End) }
        insertLog(result.bluetoothState)
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
            insertLog(state)
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
            insertLog(state)
//            _sbSensorInfo.value.let {
//                it.bluetoothState = state
//                _sbSensorInfo.tryEmit(it)
//                insertLog(it.bluetoothState)
//            }
        }
    }

    override fun operateDownloadSbSensor(isContinue: Boolean) {
        writeData(_sbSensorInfo.value.bluetoothGatt, if (isContinue) AppToModule.OperateDownloadContinue else AppToModule.OperateDownload) { state ->
            _sbSensorInfo.update { it.copy(bluetoothState = state) }
            insertLog(state)
//            _sbSensorInfo.value.let {
//                it.bluetoothState = state
//                _sbSensorInfo.tryEmit(it)
//                insertLog(it.bluetoothState)
//            }
        }
    }

    override var downloadCompleteCallback: (() -> Unit)? = null
    override fun setOnDownloadCompleteCallback(callback: (() -> Unit)?) {
        downloadCompleteCallback = callback
    }

    override var lastDownloadCompleteCallback: ((state: BLEService.FinishState) -> Unit)? = null

    override fun setOnLastDownloadCompleteCallback(callback: ((state: BLEService.FinishState) -> Unit)?) {
        lastDownloadCompleteCallback = callback
    }

    override var uploadCallback: (() -> Unit)? = null
    override fun setOnUploadCallback(callback: (() -> Unit)?) {
        uploadCallback = callback
    }

    override fun operateDeleteSbSensor(isAllDelete: Boolean) {
        writeData(_sbSensorInfo.value.bluetoothGatt, if (isAllDelete) AppToModule.OperateDeleteAll else AppToModule.OperateDeleteSector) { state ->
            _sbSensorInfo.update { it.copy(bluetoothState = state) }
            insertLog(state)
//            _sbSensorInfo.value.let {
//                it.bluetoothState = state
//                _sbSensorInfo.tryEmit(it)
//                insertLog(it.bluetoothState)
//            }
        }
    }


    override fun startNetworkSpO2Sensor() {
        // TODO write command
    }

    override fun startNetworkEEGSensor() {
        // TODO write command
    }

    private fun writeResponse(gatt: BluetoothGatt, command: AppToModuleResponse) {
        val cmd = BluetoothUtils.findCommandCharacteristic(gatt) ?: return
        logCoroutine.launch {
            val byteArr = command.getCommandByteArr()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(cmd, byteArr, WRITE_TYPE_DEFAULT)
            } else {
                cmd.value = byteArr
                gatt.writeCharacteristic(cmd)
            }

            strBuilder.clear()
            strBuilder.append("[ ")
            for (v in byteArr) {
                strBuilder.append(String.format("%02X ", v))
            }
            strBuilder.append("]\n")
            Log.d("<--- App To Device", strBuilder.toString())
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
                val byteArr = command.getCommandByteArr()
//            cmd.value = byteArr

                var result: Boolean
                do {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result = gatt.writeCharacteristic(cmd, byteArr, WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
                    } else {
                        cmd.value = byteArr
                        result = gatt.writeCharacteristic(cmd)
                    }


                    if (result) {
                        strBuilder.clear()
                        strBuilder.append("[ ")
                        for (v in byteArr) {
                            strBuilder.append(String.format("%02X ", v))
                        }
                        strBuilder.append("]\n")
                        Log.d("<--- App To Device", strBuilder.toString())
                    }

                } while (!result)
            }
        } ?: stateCallback?.invoke(BluetoothState.DisconnectedNotIntent)

    }

    override fun stopNetworkSpO2Sensor() {}

    override fun stopNetworkEEGSensor() {}
    override fun callVibrationNotifications(intensity: Int) {
        val module = when (intensity) {
            3 -> {
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
                    insertLog(state)
                }
            }

        }

    }

    override fun getGattCallback(sbBluetoothDevice: SBBluetoothDevice): BluetoothGattCallback = getCallback(sbBluetoothDevice)

    //////////////////////////////////////////////////////
    /////                                            /////
    /////           BluetoothGattCallback            /////
    /////                                            /////
    //////////////////////////////////////////////////////
    private fun getCallback(sbBluetoothDevice: SBBluetoothDevice) = object : BluetoothGattCallback() {
        private val UPLOAD_COUNT_INTERVAL = 300 * 10
        private val DATA_INTERVAL = 9

        private var uploadCallbackQuotient = -1

        private var safetyMode = 0
        private val SAFETY_STANDARD = 50

        private val DOWNLOAD_RETRY_INTERVAL = 5
        private val DOWNLOAD_RETRY_COUNT = 3
        private var downloadContinueCount = 0

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
//            Log.d(TAG, "getCallback: Connecting ")
            val result = innerData.updateAndGet {
                it.copy(bluetoothState = BluetoothState.Connecting)
            }
            insertLog(result.bluetoothState)
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d(TAG, "[NR] onConnectionStateChange: GATT_FAILURE ${gatt.device.name} / ${gatt.device.address}")
                disconnectedDevice(gatt)
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[NR] onConnectionStateChange: NOT GATT_SUCCESS ${gatt.device.name} / ${gatt.device.address}")
                disconnectedDevice(gatt)
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "[NR] onConnectionStateChange: CONNECTED ${gatt.device.name} / ${gatt.device.address}")
                gatt.discoverServices()
                innerData.update { it.copy(bluetoothGatt = gatt) }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "[NR] onConnectionStateChange: DISCONNECTED ${gatt.device.name} / ${gatt.device.address}")
                disconnectedDevice(gatt)
                return
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "[NR] onServicesDiscovered: NOT GATT_SUCCESS ${gatt.device.name} / ${gatt.device.address}")
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
            innerData.value?.bluetoothGatt?.writeDescriptor(descriptor)
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

        private fun readData(gatt: BluetoothGatt, value: ByteArray) {
            logCoroutine.launch {
            strBuilder.clear()
            strBuilder.append("[ ")
            for (v in value) {
                strBuilder.append(String.format("%02X ", v))
            }
            strBuilder.append("]\n")
            Log.d("---> Device To App", strBuilder.toString())

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
                                insertLog(info.bluetoothState)
                            }

                            BluetoothState.Connected.SendStop -> {
                                uploadCallbackQuotient = -1
                                innerData.update { it.copy(bluetoothState = BluetoothState.Connected.Finish) }
//                                it.bluetoothState = BluetoothState.Connected.Finish
//                                innerData.tryEmit(it)
                                insertLog(info.bluetoothState)
                            }

                            BluetoothState.Connected.DataFlow -> {
//                                writeData(gatt, AppToModule.OperateDeleteSector, null)
                                innerData.update { it.copy(bluetoothState = BluetoothState.Connected.Ready) }
//                                it.bluetoothState = BluetoothState.Connected.SendDelete
//                                innerData.tryEmit(it)
                                insertLog(info.bluetoothState)
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
                                insertLog(info.bluetoothState)

                                startTime = System.currentTimeMillis()
                            }

                            BluetoothState.Connected.ReceivingDelayed, BluetoothState.Connected.Reconnected -> {
                                safetyMode = 0
                                innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                innerData.tryEmit(it)
                                insertLog(info.bluetoothState)
                            }

                            BluetoothState.Connected.Init -> {
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
                                        } ?: launch {
                                            // FIXME: 하드웨어와 DataFlow 상황에서 강제종료에 대해 논의해야함.!! 중요!!
                                            writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.BreathingOperateStop, stateCallback = null)
                                            delay(1000)
                                            writeData(gatt = _sbSensorInfo.value.bluetoothGatt, command = AppToModule.NoSeringOperateStop, stateCallback = null)
                                        }

                                    }

                                }
                                innerData.update { it.copy(bluetoothState = BluetoothState.Connected.DataFlow) }
//                                it.bluetoothState = BluetoothState.Connected.DataFlow
//                                innerData.tryEmit(it)
                                insertLog(info.bluetoothState)
                            }

                            BluetoothState.Connected.SendDownloadContinue -> {
                                downloadContinueCount++
                                if (downloadContinueCount % DOWNLOAD_RETRY_INTERVAL == 0) {
                                    if (downloadContinueCount >= DOWNLOAD_RETRY_COUNT * DOWNLOAD_RETRY_INTERVAL) {
                                        innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                        it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                        innerData.tryEmit(it)
                                        insertLog(info.bluetoothState)
                                        downloadContinueCount = 0
                                    } else {
                                        writeData(gatt, AppToModule.OperateDownloadContinue) { state ->
                                            _sbSensorInfo.value.let { info ->
                                                innerData.update { it.copy(bluetoothState = state) }
//                                                info.bluetoothState = state
//                                                _sbSensorInfo.tryEmit(info)
                                                insertLog(state)
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
                                info.currentData.tryEmit(capacitance1)
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
                                info.currentData.tryEmit(capacitance2)
                                val quotient = index1 / UPLOAD_COUNT_INTERVAL
                                if (safetyMode >= SAFETY_STANDARD && uploadCallbackQuotient > -1 && quotient > uploadCallbackQuotient) {
                                    uploadCallback?.let { cb ->
                                        safetyMode = 0
                                        uploadCallbackQuotient = quotient
                                        cb.invoke()
                                    }
                                }

                                info.channel.apply {
                                    send(SBSensorData(index1, time1, capacitance1, calcAccX1, calcAccY1, calcAccZ1, info.dataId ?: -1))
                                    send(SBSensorData(index2, time2, capacitance2, calcAccX2, calcAccY2, calcAccZ2, info.dataId ?: -1))
                                }
                            }
                            writeResponse(gatt, AppToModuleResponse.RealtimeDataResponseACK)
                        } else {
                            writeResponse(gatt, AppToModuleResponse.RealtimeDataResponseNAK)
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
                                insertLog(info.bluetoothState)
                            }

                            BluetoothState.Connected.ReceivingRealtime -> {
                                innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingDelayed) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingDelayed
//                                innerData.tryEmit(it)
                                insertLog(info.bluetoothState)
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

                                    info.channel.send(SBSensorData(index, time, capacitance, calcAccX, calcAccY, calcAccZ, info.dataId ?: -1))
                                }
                            }
                            writeResponse(gatt, AppToModuleResponse.DelayedDataResponseACK)
                        } else {
                            writeResponse(gatt, AppToModuleResponse.DelayedDataResponseNAK)
                        }
                    }
                }

                ModuleToApp.MOTCtrlSetACK -> {
                    innerData.value.let { info ->
                        insertLog("코골이 동작 피드백")
                    }
                }

                ModuleToApp.OperateACK -> {
                    if (value.verifyCheckSum()) {
                        innerData.value.let { info ->
                            when (info.bluetoothState) {
                                BluetoothState.Connected.SendRealtime -> {
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                    it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                    innerData.tryEmit(it)
                                    insertLog(info.bluetoothState)
                                }

                                BluetoothState.Connected.SendDelayed -> {
                                    innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingDelayed) }
//                                    it.bluetoothState = BluetoothState.Connected.ReceivingDelayed
//                                    innerData.tryEmit(it)
                                    insertLog(info.bluetoothState)
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

                                    it.channel.send(SBSensorData(index, time, capacitance, calcAccX, calcAccY, calcAccZ, it.dataId ?: -1))
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
                                insertLog(info.bluetoothState)
                            }

                            BluetoothState.Connected.SendDownloadContinue -> {
                                downloadContinueCount = 0
                                //                                downloadCompleteCallback?.invoke()
                                innerData.update { it.copy(bluetoothState = BluetoothState.Connected.ReceivingRealtime) }
//                                it.bluetoothState = BluetoothState.Connected.ReceivingRealtime
//                                innerData.tryEmit(it)
                                insertLog(info.bluetoothState)
                            }

                            else -> {
                                // 상태 이상
                                //Log.e("---> Device To App", "MemoryDataACK Receive State Error : ${it.bluetoothState}")
                            }
                        }
                        if (value.verifyCheckSum()) {
                            writeResponse(gatt, AppToModuleResponse.MemoryDataResponseACK)
                        } else {
                            writeResponse(gatt, AppToModuleResponse.MemoryDataResponseNAK)
                        }
                    }
                }

                ModuleToApp.MemoryDataDeleteACK -> {
                    val result = innerData.updateAndGet { it.copy(bluetoothState = BluetoothState.Connected.Ready) }
                    insertLog(result.bluetoothState)
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
                    insertLog(
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
                    innerData.update { it.copy(batteryInfo = result.toString(), canMeasurement = result > 20) }
                }

                else -> {}
            }
            }
        }
    }

     private  fun insertLog(state: BluetoothState) {
         logWorkerHelper.insertLog(state.toString())
    }
      fun insertLog(msg: String) {
          logWorkerHelper.insertLog(msg)
    }
}