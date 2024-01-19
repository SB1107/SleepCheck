package kr.co.sbsolutions.newsoomirang.presenter.main

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : BaseServiceViewModel() {
    private val _changeSBSensorInfo: MutableSharedFlow<BluetoothInfo> = MutableSharedFlow()
    val changeSBSensorInfo: SharedFlow<BluetoothInfo> = _changeSBSensorInfo
    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        viewModelScope.launch {
            _changeSBSensorInfo.emit(info)
        }
    }
}

enum class ServiceCommend {
    START,STOP
}