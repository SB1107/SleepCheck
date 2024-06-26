package kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity

import android.bluetooth.BluetoothGatt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kr.co.sbsolutions.sleepcheck.data.firebasedb.RealData
import kr.co.sbsolutions.sleepcheck.domain.model.SleepType
import kr.co.sbsolutions.soomirang.db.SBSensorData

data class BluetoothInfo(
    val sbBluetoothDevice: SBBluetoothDevice,
    var bluetoothName: String? = null,
    var bluetoothAddress: String? = null,
    var bluetoothState: BluetoothState = BluetoothState.Unregistered,
    var bluetoothGatt: BluetoothGatt? = null,
    var dataId: Int? = null,
    var batteryInfo: String? = null,
    var canMeasurement: Boolean = true,
    var cancelCheck: Boolean = false,
    val isResetGatt : MutableSharedFlow<Boolean> = MutableSharedFlow(),
//    var removedRealData : MutableStateFlow<RealData?> = MutableStateFlow(null),
    var realData : MutableStateFlow<RealData?> = MutableStateFlow(null),
    var currentData: MutableSharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 1),
    var sleepType: SleepType = SleepType.Breathing,
    val channel: MutableSharedFlow<SBSensorData> = MutableSharedFlow(),
    var snoreTime : Long = 0,
    var isDataFlow : MutableStateFlow<DataFlowInfo> = MutableStateFlow(DataFlowInfo()),
) {
    companion object {
        var isOn = true
    }
    override fun toString() = "$bluetoothState / $bluetoothName / $bluetoothAddress / ${if (isOn) "ON" else "OFF"} / $dataId / $batteryInfo / $canMeasurement"
}

data class  DataFlowInfo(var isDataFlow: Boolean  = false,  var totalCount : Int = 0 ,   var currentCount : Int = 0)