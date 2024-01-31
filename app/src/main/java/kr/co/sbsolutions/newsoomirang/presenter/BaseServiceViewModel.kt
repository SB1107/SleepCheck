package kr.co.sbsolutions.newsoomirang.presenter

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import java.lang.ref.WeakReference

abstract class BaseServiceViewModel(private val dataManager: DataManager, private val tokenManager: TokenManager) : BaseViewModel(dataManager, tokenManager) {
    private lateinit var service: WeakReference<BLEService>
    private val _serviceCommend: MutableSharedFlow<ServiceCommend> = MutableSharedFlow()
    val serviceCommend: SharedFlow<ServiceCommend> = _serviceCommend
    private val _userName: MutableStateFlow<String> = MutableStateFlow("")
    val userName: StateFlow<String> = _userName
    private val _gotoScan: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val gotoScan: SharedFlow<Boolean> = _gotoScan

    private val _connectAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val connectAlert: SharedFlow<Boolean> = _connectAlert

    private val _batteryState: MutableStateFlow<String> = MutableStateFlow("")
    val batteryState: StateFlow<String> = _batteryState

    private val _canMeasurement: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val canMeasurement: SharedFlow<Boolean> = _canMeasurement
    private val _bluetoothButtonState: MutableStateFlow<String> = MutableStateFlow("시작")
    val bluetoothButtonState: StateFlow<String> = _bluetoothButtonState
    protected var bluetoothInfo = ApplicationManager.getBluetoothInfo()
    abstract fun whereTag(): String

    init {

        viewModelScope.launch {
            launch {
                ApplicationManager.getBluetoothInfoFlow().collect {
                    Log.d(TAG, "${whereTag()} 상태: ${it.bluetoothState}")
                    bluetoothInfo = it

                    setBatteryInfo()
                    if (it.bluetoothState == BluetoothState.Unregistered) {
                        _bluetoothButtonState.emit("연결")
                    } else if (it.bluetoothState == BluetoothState.DisconnectedByUser || it.bluetoothGatt == null) {
                        _bluetoothButtonState.emit("연결")
                        _batteryState.emit("")
                    } else {
                        _bluetoothButtonState.emit("시작")
                    }
                }
            }
            launch {
                val name = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
                _bluetoothButtonState.emit(if (name.isNullOrEmpty()) "연결" else "시작")
            }
            launch {
                service = ApplicationManager.getService().value
                serviceSettingCall()
                ApplicationManager.getService().collect {
                    service = it
                    serviceSettingCall()
                }
            }
        }
    }

    open fun serviceSettingCall() {}
    fun setBatteryInfo() {
        viewModelScope.launch {
            bluetoothInfo = ApplicationManager.getBluetoothInfo()
            bluetoothInfo.batteryInfo?.let { _batteryState.emit(it) }
            when (bluetoothInfo.bluetoothState) {
                //충전 상태를 알아야하는 상태
                BluetoothState.Connected.Ready ->
                    { _canMeasurement.emit(bluetoothInfo.canMeasurement) }
                else -> {}
            }
        }
    }

    fun showCharging() {
        viewModelScope.launch {
            _canMeasurement.emit(true)
        }
    }

    //    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
//    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}
    fun setCommend(serviceCommend: ServiceCommend) {
        viewModelScope.launch {
            _serviceCommend.emit(serviceCommend)
        }
    }

    fun connectClick() {
        viewModelScope.launch {
            _gotoScan.emit(true)
        }
    }

    fun isRegistered(): Boolean {
        if (bluetoothInfo.bluetoothState == BluetoothState.Unregistered || bluetoothInfo.bluetoothState == BluetoothState.DisconnectedByUser || bluetoothInfo.bluetoothGatt == null) {
            Log.d(TAG, "isRegistered: 여기도 콜 baseService")
            if (bluetoothInfo.bluetoothState == BluetoothState.DisconnectedByUser) {
                viewModelScope.launch {
                    dataManager.deleteBluetoothDevice(bluetoothInfo.sbBluetoothDevice.type.name)
                }
            }
            viewModelScope.launch {
                _connectAlert.emit(true)
            }
            return false
        }
        return true
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getUserName().first()?.let {
                _userName.emit(it)
            }
        }
    }

    fun getService(): BLEService? {
        return if (::service.isInitialized) {
            this.service.get()
        } else {
            null
        }
    }

}