package kr.co.sbsolutions.newsoomirang.presenter.main

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : BaseServiceViewModel() {
    private val _changeSBSensorInfo: MutableSharedFlow<BluetoothInfo> = MutableSharedFlow()
    val changeSBSensorInfo: SharedFlow<BluetoothInfo> = _changeSBSensorInfo

    private val _breathingResults: MutableSharedFlow<Int> = MutableSharedFlow()
    private val _noSeringResults: MutableSharedFlow<Int> = MutableSharedFlow()
    val breathingResults: SharedFlow<Int> = _breathingResults
    val noSeringResults: SharedFlow<Int> = _noSeringResults
    private lateinit var  btInfo : BluetoothInfo

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        viewModelScope.launch {
            _changeSBSensorInfo.emit(info)
            btInfo = info
        }
    }
    fun  sendMeasurementResults(){
        viewModelScope.launch {
            if (::btInfo.isInitialized) {
                if (btInfo.sleepType == SleepType.Breathing) {
                    _breathingResults.emit(0)
                }else{
                    _noSeringResults.emit(0)
                }
            }
        }
    }

}

enum class ServiceCommend {
    START,STOP
}