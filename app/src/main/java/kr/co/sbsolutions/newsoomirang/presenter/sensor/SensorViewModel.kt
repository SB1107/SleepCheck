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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.usecase.BluetoothManageUseCase
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
    private val dataManager: DataManager,
    private val tokenManager: TokenManager
) : BaseServiceViewModel(dataManager, tokenManager) {
    companion object {
        private const val DELAY_TIMEOUT = 5000L
    }

    private val _isScanning = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val isScanning: SharedFlow<Boolean> = _isScanning

    private val _isRegistered = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val isRegistered: SharedFlow<Boolean> = _isRegistered

    private val _scanList = MutableSharedFlow<List<BluetoothDevice>>(extraBufferCapacity = 1)
    val scanList: SharedFlow<List<BluetoothDevice>> = _scanList

    private val _bleName: MutableStateFlow<String?> = MutableStateFlow(null)
    val bleName: SharedFlow<String?> = _bleName.asSharedFlow()

    private val _disconnected: MutableSharedFlow<Boolean> = MutableSharedFlow(extraBufferCapacity = 1)

    private val _scanSet = mutableSetOf<BluetoothDevice>()
    private var timer: Timer? = null

    init {
        viewModelScope.launch {
            launch {
                getName()
                if (bluetoothInfo.bluetoothState == BluetoothState.Registered) {
                    changeStatus(bluetoothInfo)
                }
            }
            launch {
                ApplicationManager.getBluetoothInfoFlow().collectLatest { info ->
                    Log.d(TAG, "[SVM]: $info")
                    getName()

                    if (info.bluetoothState == BluetoothState.Registered) {
                        changeStatus(info)
                    }
                }
            }


        }
    }
    fun checkDeviceScan(){
        viewModelScope.launch {
                val name = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
                if (name.isNullOrEmpty()) {
                    scanBLEDevices()
                }
        }
    }

    private fun getName() {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()?.let {
                _bleName.emit(it)
                Log.d(TAG, "디바이스 이름: $it")
            }
        }
    }

    private fun changeStatus(info: BluetoothInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(info.sbBluetoothDevice.type.name).first()?.let {
                _bleName.emit(it)
                Log.d(TAG, "changeStatus: $it")
            }
        }

        if (info.bluetoothState == BluetoothState.Registered) {
            deviceConnect(info)
        }
    }


    fun disconnectDevice() {
//        Log.d(TAG, "현재 상태 : ${bluetoothInfo.bluetoothState} ")

        viewModelScope.launch(Dispatchers.IO) {
            _disconnected.emit(dataManager.deleteBluetoothDevice(bluetoothInfo.sbBluetoothDevice.type.name))
            getService()?.disconnectDevice(bluetoothInfo)
        }

        /*// 상태 정리 필요함
        when (bluetoothInfo.bluetoothState) {
            BluetoothState.Connected.ReceivingRealtime,
            BluetoothState.Connected.SendDownloadContinue,
            -> {
                Log.d(TAG, "disconnectDevice: 연결해제 불가능")
            }
            else -> {
                Log.d(TAG, "disconnectDevice: 연결해제 완료")
                getService()?.disconnectDevice(bluetoothInfo)
                unRegister()
            }
        }*/

    }

    @SuppressLint("MissingPermission")
    fun scanBLEDevices() {
        if (!bluetoothAdapter.isEnabled) {
            _isScanning.tryEmit(false)
            return
        }

        _scanList.tryEmit(emptyList())
        _scanSet.clear()
        startTimer()
        bluetoothAdapter.bluetoothLeScanner.startScan(scanFilter, scanSettings, bleScanCallback)
    }

    fun registerDevice(bluetoothDevice: BluetoothDevice) {
        stopTimer()
        registerBluetoothDevice(bluetoothDevice)

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
            _isRegistered.tryEmit(bluetoothManagerUseCase.registerSBSensor(SBBluetoothDevice.SB_SOOM_SENSOR, device.name, device.address))
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

        private fun addScanResult(result: ScanResult) {
            val device = result.device

            if (_scanSet.add(device)) {
                _scanList.tryEmit(_scanSet.toList())
            }
        }
    }
}