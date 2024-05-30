package kr.co.sbsolutions.newsoomirang.presenter.sensor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.BlueToothScanHelper
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.usecase.BluetoothManageUseCase
import kr.co.sbsolutions.newsoomirang.domain.model.CheckSensor
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val blueToothScanHelper: BlueToothScanHelper,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    private val logHelper: LogHelper

) : BaseServiceViewModel(dataManager, tokenManager) {

    val isScanning: StateFlow<Boolean?> = blueToothScanHelper.isScanning

    val scanList: StateFlow<List<BluetoothDevice>> = blueToothScanHelper.scanList

    private val _bleName: MutableStateFlow<String?> = MutableStateFlow(null)
    val bleName: StateFlow<String?> = _bleName

    private val _checkSensorResult: MutableStateFlow<String?> = MutableStateFlow(null)
    val checkSensorResult: StateFlow<String?> = _checkSensorResult

    private val _bleStateResultText: MutableStateFlow<String?> = MutableStateFlow(null)
    val bleStateResultText: StateFlow<String?> = _bleStateResultText

    private val _isBleProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isBleProgressBar: SharedFlow<Boolean> = _isBleProgressBar

    //    private val _disconnected: MutableSharedFlow<Boolean> = MutableSharedFlow(extraBufferCapacity = 1)
    init {
        viewModelScope.launch(Dispatchers.IO) {
            //디바이스 네임 상태 이벤트
            launch {
                bluetoothManagerUseCase.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR)
                    .collectLatest {
                        getDeviceName(it)
                    }
            }
        }
    }

    private suspend fun getDeviceName(it: String?) {
        _bleName.emit(it)
        Log.d(TAG, "디바이스 이름: $it")
    }

    private fun connectState() {
        viewModelScope.launch {
            logHelper.insertLog("connectState: ${getService()?.getSbSensorInfo()?.value.toString()}")
            launch {
                getService()?.getSbSensorInfo()?.onEach {
                    logHelper.insertLog("onEach batteryInfo -> ${it.batteryInfo}  ")
                }?.filter { it.batteryInfo != null }?.collectLatest {
                    Log.e(TAG, "배터리1: ${it.batteryInfo}")
                    Log.e(TAG, "배터리2: ${it.batteryInfo.isNullOrEmpty().not()}")
                    _isBleProgressBar.emit(it.batteryInfo.isNullOrEmpty().not())
                }
            }
        }
    }

    fun bleDisconnect() {
        when (getService()?.getSbSensorInfo()?.value?.bluetoothState) {
            //측정중
            BluetoothState.Connected.ReceivingRealtime,
            BluetoothState.Connected.SendDownloadContinue -> {
                sendErrorMessage((ApplicationManager.instance.baseContext.getString(R.string.sensor_error_message_measurement)))
            }
            //연결 해제
            else -> {
                disconnectDevice()
            }
        }
    }

    fun bleConnect() {
        insertLog("스캔 사용자가 직접 bleConnect() ${bluetoothInfo.bluetoothState}")
        viewModelScope.launch(Dispatchers.IO) {
            when (getService()?.getSbSensorInfo()?.value?.bluetoothState) {
                //연결
                BluetoothState.Unregistered,
                BluetoothState.DisconnectedByUser -> {
                    scanBLEDevices()
                }
                //연결 해제
                else -> {}
            }
        }
    }

    fun checkDeviceScan() {
        viewModelScope.launch {
            val name = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
            if (name.isNullOrEmpty()) {
                scanBLEDevices()
            }
        }
    }


    //연결끊기
    private fun disconnectDevice() {
        getService()?.disconnectDevice()
        insertLog("사용자가 직접 연결 끊음 현재 상태 : ${bluetoothInfo.bluetoothState}")
        viewModelScope.launch(Dispatchers.IO) {
            bluetoothManagerUseCase.unregisterSBSensor(SBBluetoothDevice.SB_SOOM_SENSOR)
        }
    }


    @SuppressLint("MissingPermission")
    fun scanBLEDevices() {
        blueToothScanHelper.scanBLEDevices(viewModelScope)
    }

    @SuppressLint("MissingPermission")
    suspend fun registerDevice(bluetoothDevice: BluetoothDevice) {
        insertLog("registerDevice 사용자가 시도: ${bluetoothDevice.name}")
        request { authAPIRepository.postCheckSensor(CheckSensor(sensorName = bluetoothDevice.name)) }
            .collectLatest {
                insertLog("registerDevice 서버: ${it.success}  ${it.message}")
                if (it.success.not()) {
                    _checkSensorResult.emit(it.message)
                } else {
                    connectState()
                    blueToothScanHelper.stopTimer()
                    registerBluetoothDevice(bluetoothDevice)
                }
            }
    }


    override fun whereTag(): String {
        return "Sensor"
    }

    override fun onCleared() {
        super.onCleared()
        blueToothScanHelper.stopTimer()
        Log.e(TAG, "onCleared: ")
    }

    @SuppressLint("MissingPermission")
    private fun registerBluetoothDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBleProgressBar.emit(false)
            _bleStateResultText.emit(ApplicationManager.instance.baseContext.getString(R.string.sensor_ask_to_connect_message, device.name))

            bluetoothManagerUseCase.registerSBSensor(SBBluetoothDevice.SB_SOOM_SENSOR, device.name, device.address)
        }
    }

}