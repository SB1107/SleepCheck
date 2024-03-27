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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
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
    private val bluetoothAdapter: BluetoothAdapter,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
) : BaseServiceViewModel(dataManager, tokenManager) {
    companion object {
        private const val DELAY_TIMEOUT = 5000L
    }

    private val _isScanning: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isScanning: SharedFlow<Boolean?> = _isScanning.asSharedFlow()


    private val _scanList: MutableStateFlow<List<BluetoothDevice>> = MutableStateFlow(emptyList())
    val scanList: SharedFlow<List<BluetoothDevice>> = _scanList.asSharedFlow()

    private val _bleName: MutableStateFlow<String?> = MutableStateFlow(null)
    val bleName: SharedFlow<String?> = _bleName.asSharedFlow()


    private val _checkSensorResult: MutableStateFlow<String?> = MutableStateFlow(null)
    val checkSensorResult: SharedFlow<String?> = _checkSensorResult.asSharedFlow()

    private val _bleStateResultText: MutableStateFlow<String?> = MutableStateFlow(null)
    val bleStateResultText: SharedFlow<String?> = _bleStateResultText.asSharedFlow()

    private val _isBleProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isBleProgressBar: SharedFlow<Boolean> = _isBleProgressBar


//    private val _disconnected: MutableSharedFlow<Boolean> = MutableSharedFlow(extraBufferCapacity = 1)

    private val _scanSet = mutableSetOf<BluetoothDevice>()
    private var timer: Timer? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            //디바이스 네임 상태 이벤트
            launch {
                bluetoothManagerUseCase.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR).collectLatest {
                    getDeviceName(it)
                }
            }
        }
    }

    private suspend fun getDeviceName(it: String?) {
        _bleName.emit(it)
        Log.d(TAG, "디바이스 이름: $it")
    }

    fun connectState() {
        viewModelScope.launch {
            Log.d(TAG, "connectState: ${getService()?.sbSensorInfo}")
            launch {
                getService()?.sbSensorInfo?.let { it ->
                    canMeasurement.zip(it)
                    { canMeasurement, info -> canMeasurement to info }
                        .filter { it.first }
                        .collectLatest {
                        Log.e(TAG, "배터리1: ${getService()?.sbSensorInfo?.value?.batteryInfo}")
                        Log.e(TAG, "배터리2: ${getService()?.sbSensorInfo?.value?.batteryInfo.isNullOrEmpty().not()}")
                        _isBleProgressBar.emit(getService()?.sbSensorInfo?.value?.batteryInfo.isNullOrEmpty().not() and it.first)
                    }
                }
            }
        }
    }

    fun bleDisconnect() {
        disconnectDevice()
    }

    fun bleConnect() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "bleConnectOrDisconnect: 나 불림 ${bluetoothInfo.bluetoothState}")
            when (getService()?.sbSensorInfo?.value?.bluetoothState) {
                //연결
                BluetoothState.Unregistered,
                BluetoothState.DisconnectedByUser -> {
                    scanBLEDevices()
                }

                //측정중
                BluetoothState.Connected.ReceivingRealtime,
                BluetoothState.Connected.SendDownloadContinue -> {
                    sendErrorMessage(("측정중 입니다.\n측정을 종료후 시도해주세요"))
                }

                BluetoothState.Connected.Init,
                BluetoothState.Connected.DataFlow,
                BluetoothState.Connected.Finish,
                BluetoothState.Connected.End,
                BluetoothState.Connected.ForceEnd -> {
                    disconnectDevice()
                }

                BluetoothState.DisconnectedNotIntent -> {
                    sendErrorMessage(("측정 종료후 시도해주세요."))
                }

                BluetoothState.Connecting -> {
                }

                BluetoothState.Registered -> {
                }

                //연결 해제
                else -> {
                    disconnectDevice()
                }
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
//        Log.d(TAG, "현재 상태 : ${bluetoothInfo.bluetoothState} ")
        viewModelScope.launch(Dispatchers.IO) {
            Log.e(TAG, "sensor disconnectDevice: ")
            getService()?.disconnectDevice()
            bluetoothManagerUseCase.unregisterSBSensor(SBBluetoothDevice.SB_SOOM_SENSOR)
        }


    }

@SuppressLint("MissingPermission")
fun scanBLEDevices() {
    if (!bluetoothAdapter.isEnabled) {
        _isScanning.tryEmit(false)
        return
    }

    /*Log.d(TAG, "scanBLEDevices:${bluetoothAdapter.getProfileConnectionState(1)} ")
    Log.d(TAG, "scanBLEDevices:${bluetoothAdapter.getProfileConnectionState(2)} ")*/

    viewModelScope.launch(Dispatchers.IO) {
        startTimer()
        delay(1000)
        _scanList.tryEmit(emptyList())
        _scanSet.clear()
        bluetoothAdapter.bluetoothLeScanner.startScan(scanFilter, scanSettings, bleScanCallback)
    }
}

@SuppressLint("MissingPermission")
suspend fun registerDevice(bluetoothDevice: BluetoothDevice) {
    Log.d(TAG, "registerDevice: ${bluetoothDevice.name}")
    request { authAPIRepository.postCheckSensor(CheckSensor(sensorName = bluetoothDevice.name)) }
        .collectLatest {
            Log.d(TAG, "registerDevice: ${it.success}  ${it.message}")
            if (it.success.not()) {
                _checkSensorResult.emit(it.message)
            } else {
                stopTimer()
                registerBluetoothDevice(bluetoothDevice)
            }
        }
}

private fun startTimer() {
    stopTimer()

    _isScanning.tryEmit(true)
    timer = Timer()
    timer?.schedule(object : TimerTask() {
        override fun run() {
            stopTimer()
        }
    }, DELAY_TIMEOUT)
}

private val scanFilter: MutableList<ScanFilter> by lazy {
    arrayListOf(
        ScanFilter
            .Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(Cons.SERVICE_STRING)))
            .build()
    )
}

@SuppressLint("MissingPermission")
private fun stopTimer() {
    _isScanning.tryEmit(false)
    timer?.let {
        bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
        it.cancel()
    }
    timer = null
}

override fun whereTag(): String {
    return "Sensor"
}

override fun onCleared() {
    super.onCleared()
    stopTimer()
}

private fun deviceConnect(info: BluetoothInfo) {
    getService()?.connectDevice(info)
}

@SuppressLint("MissingPermission")
private fun registerBluetoothDevice(device: BluetoothDevice) {
    viewModelScope.launch(Dispatchers.IO) {
        _isBleProgressBar.emit(false)
        _bleStateResultText.emit("숨이랑 ${device.name}\n 기기와 연결중입니다.")

        bluetoothManagerUseCase.registerSBSensor(SBBluetoothDevice.SB_SOOM_SENSOR, device.name, device.address)
        getService()?.sbSensorInfo?.collectLatest {
            if (it.bluetoothState == BluetoothState.DisconnectedByUser) {
                deviceConnect(it)
            }
        }
//            canMeasurement.collectLatest {
//                Log.d(TAG, "registerBluetoothDevice: $it")
//                if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready) {
//                    _isBleProgressBar.emit(it.not())
//                }
//            }

    }
}


private val scanSettings: ScanSettings by lazy {
    ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
}
private val bleScanCallback = object : ScanCallback() {
    override fun onScanResult(callbackType: Int, result: ScanResult) {
        super.onScanResult(callbackType, result)
        addScanResult(result)
    }

    override fun onBatchScanResults(results: List<ScanResult>) {
        for (result in results) {
            addScanResult(result)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addScanResult(result: ScanResult) {
        val device = result.device
        if (_scanSet.add(device)) {
            _scanList.tryEmit(_scanSet.toList())
        }
    }
}
}