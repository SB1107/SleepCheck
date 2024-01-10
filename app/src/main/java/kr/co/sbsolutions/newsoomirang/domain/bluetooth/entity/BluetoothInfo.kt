package kr.co.sbsolutions.withsoom.domain.bluetooth.entity

import android.bluetooth.BluetoothGatt
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.channels.Channel
import kr.co.sbsolutions.soomirang.db.SBSensorData

data class BluetoothInfo(
    val sbBluetoothDevice: SBBluetoothDevice,
    var bluetoothName: String? = null,
    var bluetoothAddress: String? = null,
    var bluetoothState: BluetoothState = BluetoothState.Unregistered,
    var bluetoothGatt: BluetoothGatt? = null,
    var dataId: Int? = null,

    var currentData: MutableLiveData<Int>? = null,
    val channel: Channel<SBSensorData> = Channel(Channel.UNLIMITED)
) {
    companion object {
        var isOn = true
    }
    override fun toString() = "$bluetoothState / $bluetoothName / $bluetoothAddress / ${if(isOn) "ON" else "OFF"} / $dataId"
}
