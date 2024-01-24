package kr.co.sbsolutions.newsoomirang.presenter

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.utils.TokenManager
import java.lang.ref.WeakReference

abstract class BaseServiceViewModel(private val dataManager: DataManager , private  val tokenManager: TokenManager) : BaseViewModel(dataManager,tokenManager) {
    private lateinit var service: WeakReference<BLEService>
    private val _serviceCommend: MutableSharedFlow<ServiceCommend> = MutableSharedFlow()
    val serviceCommend: SharedFlow<ServiceCommend> = _serviceCommend
    private val _userName: MutableSharedFlow<String> = MutableSharedFlow()
    val userName: SharedFlow<String> = _userName
    private val _gotoScan: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val gotoScan: SharedFlow<Boolean> = _gotoScan
    private val _batteryState: MutableSharedFlow<String> = MutableSharedFlow(replay = 1)
    val batteryState: SharedFlow<String> = _batteryState

    private val _canMeasurement: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val canMeasurement: SharedFlow<Boolean> = _canMeasurement
    protected  var bluetoothInfo = ApplicationManager.getBluetoothInfo()
    abstract fun whereTag() : String

    init {
        viewModelScope.launch {
            launch {
                ApplicationManager.getBluetoothInfoFlow().collect {
                    Log.d(TAG, "${whereTag()} 상태: ${it.bluetoothState}")
                    bluetoothInfo = it
                    setBatteryInfo()
                    serviceSettingCall()
                }
            }
            launch {
                service = ApplicationManager.getService().value
                ApplicationManager.getService().collect {
                    service = it
                }
            }
        }
    }
    open fun serviceSettingCall(){}
    fun setBatteryInfo(){
        viewModelScope.launch {
            bluetoothInfo = ApplicationManager.getBluetoothInfo()
            bluetoothInfo.batteryInfo?.let { _batteryState.emit(it) }
            _canMeasurement.emit(bluetoothInfo.canMeasurement)
        }

    }

//    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
//    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}
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

    fun getService(): BLEService? {
        return if (::service.isInitialized) {
            this.service.get()
        } else {
            null
        }
    }

}