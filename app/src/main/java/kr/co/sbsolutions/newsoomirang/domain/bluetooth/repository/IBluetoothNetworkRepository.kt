package kr.co.sbsolutions.withsoom.domain.bluetooth.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice

interface IBluetoothNetworkRepository {
    var downloadCompleteCallback: (() -> Unit)?
    fun setOnDownloadCompleteCallback(callback: (() -> Unit)?)

    var lastDownloadCompleteCallback: ((state: BLEService.FinishState) -> Unit)?
    fun setOnLastDownloadCompleteCallback(callback: ((state: BLEService.FinishState) -> Unit)?)

    var uploadCallback: (() -> Unit)?
    fun setOnUploadCallback(callback: (() -> Unit)?)
    fun getGattCallback(sbBluetoothDevice: SBBluetoothDevice) : BluetoothGattCallback

    suspend fun listenRegisterSBSensor()
    suspend fun listenRegisterSpO2Sensor()
    suspend fun listenRegisterEEGSensor()

    fun getDeviceAddress(sbBluetoothDevice: SBBluetoothDevice) : String?
    fun connectedDevice(device: BluetoothDevice?)
    fun changeBluetoothState(isOn: Boolean)
    fun disconnectedDevice(sbBluetoothDevice: SBBluetoothDevice)
    fun releaseResource()

    fun startNetworkSBSensor(dataId: Int)
    fun stopNetworkSBSensor()
    fun endNetworkSBSensor(isForcedClose: Boolean)
    fun operateRealtimeSBSensor()
    fun operateDelayedSBSensor()
    fun operateDownloadSbSensor(isContinue: Boolean)
    fun operateDeleteSbSensor(isAllDelete: Boolean)
    fun startNetworkSpO2Sensor()
    fun stopNetworkSpO2Sensor()
    fun startNetworkEEGSensor()
    fun stopNetworkEEGSensor()

    val sbSensorInfo : StateFlow<BluetoothInfo>
    val spo2SensorInfo : StateFlow<BluetoothInfo>
    val eegSensorInfo : StateFlow<BluetoothInfo>
}