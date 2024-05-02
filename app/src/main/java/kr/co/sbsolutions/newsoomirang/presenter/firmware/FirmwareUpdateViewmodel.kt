package kr.co.sbsolutions.newsoomirang.presenter.firmware

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FirmwareUpdateViewmodel
@Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
): BaseViewModel(dataManager,tokenManager) {
    
    private val _checkFirmWaveVersion: MutableSharedFlow<String?> = MutableSharedFlow()
    val checkFirmWaveVersion: SharedFlow<String?> = _checkFirmWaveVersion
    
    private val _checkDownloadInsertBtn: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val checkDownloadInsertBtn: StateFlow<Boolean?> = _checkDownloadInsertBtn
    
    init {
    
    }
    
    fun checkDownloadInsertBtn() {
        }
    
    fun deviceInfo():Pair<String, String> {
        var deviceName :String = ""
        var deviceAddress : String = ""
        viewModelScope.launch {
            deviceName = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first().toString()
            deviceAddress = dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first().toString()
        }
        return Pair(second = deviceAddress, first = deviceName)
    }
}
    