package kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCallback
import kotlinx.coroutines.flow.StateFlow
import kr.co.sbsolutions.newsoomirang.data.firebasedb.RealData
import kr.co.sbsolutions.newsoomirang.service.BLEService
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice

interface IBluetoothNetworkRepository {
    var downloadCompleteCallback: (() -> Unit)?

    @Deprecated("사용안함")
    fun setOnDownloadCompleteCallback(callback: (() -> Unit)?)

    var lastDownloadCompleteCallback: ((state: BLEService.FinishState) -> Unit)?
    fun setOnLastDownloadCompleteCallback(callback: ((state: BLEService.FinishState) -> Unit)?)

    fun setDataFlowForceFinish(callBack: (() -> Unit)?)
    fun setLastIndexCk(data: Boolean)
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
    fun setRealData(realData: RealData?)
    fun setSBSensorCancel(isCancel: Boolean)
    fun setDataFlow(isDataFlow: Boolean, currentCount : Int = 0 ,totalCount : Int = 0)
    fun setDataId(dataId: Int)
    fun isSBSensorConnect() : Pair<Boolean , String>
    fun getDataFlowMaxCount() : Int
    fun sendDownloadContinueCancel()
    fun snoreCountIncrease(callBack: (() -> Unit)?)
    val sbSensorInfo : StateFlow<BluetoothInfo>
    val spo2SensorInfo : StateFlow<BluetoothInfo>
    val eegSensorInfo : StateFlow<BluetoothInfo>
}