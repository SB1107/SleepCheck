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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.domain.bluetooth.usecase.BluetoothManageUseCase
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SensorViewModel @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
) : BaseServiceViewModel() {
    companion object {
        private const val DELAY_TIMEOUT = 5000L
    }
    private val _isScanning = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val isScanning: SharedFlow<Boolean> = _isScanning

    private val _isRegistered = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val isRegistered: SharedFlow<Boolean> = _isRegistered

    private val _scanSet = mutableSetOf<BluetoothDevice>()
    private val _scanList = MutableSharedFlow<List<BluetoothDevice>>(extraBufferCapacity = 1)
    val scanList: SharedFlow<List<BluetoothDevice>> = _scanList
    private var timer: Timer? = null

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        changeStatus(info)
    }

    private fun changeStatus(info: BluetoothInfo) {
        /*if (info.bluetoothState.) {
            deviceNameTextView.text = info.bluetoothName
            btSearch.visibility = View.INVISIBLE
        }*/
        if (info.bluetoothState == BluetoothState.Registered) {
            Log.e("safsf","Registered")
            deviceConnect(info)
        }

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

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    fun deviceConnect(info: BluetoothInfo) {
        getService()?.connectDevice(info)

    }

    @SuppressLint("MissingPermission")
    private fun registerBluetoothDevice(device: BluetoothDevice) {
        viewModelScope.launch(Dispatchers.Main) {
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