package kr.co.sbsolutions.newsoomirang.presenter

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
open class BaseServiceViewModel : BaseViewModel() {

    init {
        viewModelScope.launch {
            BLEService.sbSensorInfo.collect {
                onChangeSBSensorInfo(it)
            }
        }

    }

    open fun onChangeSBSensorInfo(info: BluetoothInfo) {

    }
    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}
}