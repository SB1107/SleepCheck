package kr.co.sbsolutions.sleepcheck.common

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

class BlueToothScanHelper(private val context: Context) {
    companion object {
        private const val DELAY_TIMEOUT = 5000L
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        context.getSystemService(BluetoothManager::class.java)?.run {
            return@run adapter
        }
    }

    private val _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanList: MutableStateFlow<List<BluetoothDevice>> = MutableStateFlow(emptyList())
    val scanList: StateFlow<List<BluetoothDevice>> = _scanList
    private val _scanSet = mutableSetOf<BluetoothDevice>()

    private var timer: Timer? = null


    private val scanFilter: MutableList<ScanFilter> by lazy {
        arrayListOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(Cons.SERVICE_STRING))).build()
        )
    }

    private val scanSettings: ScanSettings by lazy {
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
    }


    @SuppressLint("MissingPermission")
    fun scanBLEDevices(lifecycleScope: CoroutineScope) {
        if (bluetoothAdapter?.isEnabled != true) {
            _isScanning.tryEmit(false)
            Log.e(TAG, "scanBLEDevices: false")
            return
        }

        /*Log.d(TAG, "scanBLEDevices:${bluetoothAdapter.getProfileConnectionState(1)} ")
        Log.d(TAG, "scanBLEDevices:${bluetoothAdapter.getProfileConnectionState(2)} ")*/

        lifecycleScope.launch(Dispatchers.IO) {
            startTimer()
            delay(1000)
            _scanList.tryEmit(emptyList())
            _scanSet.clear()
            bluetoothAdapter?.bluetoothLeScanner?.startScan(scanFilter,  scanSettings , bleScanCallback)
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

    @SuppressLint("MissingPermission")
    fun stopTimer() {
        _isScanning.tryEmit(false)
        timer?.let {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
            it.cancel()
        }
        timer = null
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