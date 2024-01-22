package kr.co.sbsolutions.newsoomirang.presenter

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import java.lang.ref.WeakReference

abstract class BaseServiceViewModel(private val dataManager: DataManager) : BaseViewModel() {
    private lateinit var service: WeakReference<BLEService>
    private val _serviceCommend: MutableSharedFlow<ServiceCommend> = MutableSharedFlow()
    val serviceCommend: SharedFlow<ServiceCommend> = _serviceCommend
    private val _userName: MutableSharedFlow<String> = MutableSharedFlow()
    val userName: SharedFlow<String> = _userName
    private val _gotoScan: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val gotoScan: SharedFlow<Boolean> = _gotoScan
    private val _batteryState: MutableSharedFlow<String> = MutableSharedFlow()
    val batteryState: SharedFlow<String> = _batteryState
    protected var bluetoothInfo: BluetoothInfo = BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR)
    private val _canMeasurement: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val canMeasurement: SharedFlow<Boolean> = _canMeasurement

    open fun onChangeSBSensorInfo(info: BluetoothInfo){
        viewModelScope.launch {
            launch {
                info.batteryInfo?.let { _batteryState.emit(it) }
                _canMeasurement.emit(info.canMeasurement)
            }
        }
    }
    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}
    fun setCommend(serviceCommend: ServiceCommend) {
        viewModelScope.launch {
            _serviceCommend.emit(serviceCommend)
        }
    }
    fun  isRegistered() : Boolean{
        if (bluetoothInfo.bluetoothState == BluetoothState.Unregistered) {
            viewModelScope.launch {
                _gotoScan.emit(true)
            }
            return false
        }
        return  true
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getUserName().first()?.let {
                _userName.emit(it)
            }
        }
    }

    fun setService(service: BLEService) {
        this.service = WeakReference(service)
    }

    fun getService(): BLEService? {
        return if (::service.isInitialized) {
            this.service.get()
        } else {
            null
        }

    }
}