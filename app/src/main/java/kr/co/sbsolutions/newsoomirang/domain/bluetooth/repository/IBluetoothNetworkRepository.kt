package kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import kotlinx.coroutines.flow.StateFlow
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice

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

    fun startNetworkSBSensor(dataId: Int, sleepType: SleepType)
    fun stopNetworkSBSensor(snoreTime : Long = 0 )
    fun endNetworkSBSensor(isForcedClose: Boolean)
    fun operateRealtimeSBSensor()
    fun operateDelayedSBSensor()
    fun operateDownloadSbSensor(isContinue: Boolean)
    fun operateDeleteSbSensor(isAllDelete: Boolean)
    fun startNetworkSpO2Sensor()
    fun stopNetworkSpO2Sensor()
    fun startNetworkEEGSensor()
    fun stopNetworkEEGSensor()
    fun callVibrationNotifications(Intensity : Int)

    val sbSensorInfo : StateFlow<BluetoothInfo>
    val spo2SensorInfo : StateFlow<BluetoothInfo>
    val eegSensorInfo : StateFlow<BluetoothInfo>
}