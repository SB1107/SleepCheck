package kr.co.sbsolutions.newsoomirang.presenter

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.DataFlowInfo
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import java.lang.ref.WeakReference

abstract class BaseServiceViewModel(
    private val dataManager: DataManager,
    tokenManager: TokenManager
) : BaseViewModel(dataManager, tokenManager) {
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

    private val _canMeasurement: MutableSharedFlow<Boolean> = MutableStateFlow(true)
//    val canMeasurement: SharedFlow<Boolean> = _canMeasurement
    private val _bluetoothButtonState: MutableStateFlow<String> = MutableStateFlow("시작")
//    val bluetoothButtonState: SharedFlow<String> = _bluetoothButtonState.asSharedFlow()
    val  canMeasurementAndBluetoothButtonState : StateFlow<Pair<Boolean, String>> = _bluetoothButtonState.combine(_canMeasurement){ bluetoothButtonState,canMeasurement  ->
    Pair(canMeasurement, bluetoothButtonState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Pair(true, "시작"))
    protected var bluetoothInfo = ApplicationManager.getBluetoothInfo()

    private val _isHomeBleProgressBar: MutableSharedFlow<Pair<Boolean, String>> =
        MutableSharedFlow()
    val isHomeBleProgressBar: SharedFlow<Pair<Boolean, String>> = _isHomeBleProgressBar
    private  val _guideAlert : MutableStateFlow<Boolean>  = MutableStateFlow(false)
    val guideAlert : StateFlow<Boolean> = _guideAlert

    private val _dataFlowInfoMessage: MutableStateFlow<DataFlowInfo> = MutableStateFlow(DataFlowInfo())
    val dataFlowInfoMessage : StateFlow<DataFlowInfo> = _dataFlowInfoMessage

    private val _dataFlowPopUp: MutableStateFlow<Boolean> = MutableStateFlow(false)
     val dataFlowPopUp: StateFlow<Boolean> = _dataFlowPopUp

    abstract fun whereTag(): String

    init {

        viewModelScope.launch {
            launch(Dispatchers.IO) {
                ApplicationManager.getBluetoothInfoFlow().collect {
                    Log.d(TAG, "${whereTag()} 상태: ${it.bluetoothState}")
                    bluetoothInfo = it

                    setBatteryInfo()
                    launch {
                        bluetoothInfo.isDataFlow.collectLatest { isDataFlow ->
                            _dataFlowInfoMessage.emit(isDataFlow.copy())
                            return@collectLatest
                        }
                    }
                        when (it.bluetoothState) {
                            BluetoothState.Unregistered -> {
                                _bluetoothButtonState.emit("연결")
                            }

                            BluetoothState.DisconnectedByUser -> {
                                Log.e(TAG, "BluetoothState.DisconnectedByUser ")
                                bluetoothInfo.batteryInfo = null
                                        _isHomeBleProgressBar.emit(Pair(false, ""))
                                    _batteryState.emit("")
                                    _bluetoothButtonState.emit("연결")
                            }

                            BluetoothState.Connected.Reconnected -> {
                                    _bluetoothButtonState.emit("재 연결중")
                                setBatteryInfo()
                            }

                            BluetoothState.DisconnectedNotIntent -> {
                                bluetoothInfo.batteryInfo = null
                                _batteryState.emit("")
                                _bluetoothButtonState.emit("연결 끊김")
                            }

                            BluetoothState.Connected.Ready,
                            BluetoothState.Connected.ReceivingRealtime,
                            BluetoothState.Connected.SendDownloadContinue,
                            BluetoothState.Connected.End -> {
                                _bluetoothButtonState.emit("시작")
                            }
                            BluetoothState.Connected.WaitStart ->{
                                launch {
                                    _bluetoothButtonState.emit("시작")
                                    _isHomeBleProgressBar.emit(Pair(true, "센서정보를\n 받아오는 중입니다."))
                                    getService()?.waitStart()
                                }
                            }
                            BluetoothState.Connected.Finish ->{
                                launch {
                                    _bluetoothButtonState.emit("시작")
                                    _isHomeBleProgressBar.emit(Pair(true, "센서정보를\n 받아오는 중입니다."))
                                    getService()?.finishSenor()
                                }
                            }

                            BluetoothState.Connecting -> {
                                _isHomeBleProgressBar.emit(Pair(true, "기기와 연결중 입니다."))
                                _bluetoothButtonState.emit("재 연결중")
//                                getService()?.timerOfDisconnection()
                            }

                            BluetoothState.Connected.DataFlow,
                            BluetoothState.Connected.DataFlowUploadFinish-> {
                                _guideAlert.emit(false)
                            }

                            else -> {
                                _bluetoothButtonState.emit("시작")
                                _isHomeBleProgressBar.emit(Pair(true, "센서정보를\n 받아오는 중입니다."))
                            }
                        }
                    }
            }
            /*launch {
                val name = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
                _bluetoothButtonState.emit(if (name.isNullOrEmpty()) "연결" else "시작")
            }*/
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

    open fun serviceSettingCall() {
        viewModelScope.launch {
            getService()?.dataFlowPopUp?.collectLatest {
                _dataFlowPopUp.emit(it)
            }
        }
    }
    fun setBatteryInfo() {
        viewModelScope.launch {
            bluetoothInfo = ApplicationManager.getBluetoothInfo()
            bluetoothInfo.batteryInfo?.let {
                _batteryState.emit(it)
                _bluetoothButtonState.emit("시작")
                _isHomeBleProgressBar.emit(Pair(false, ""))
            }
            when (bluetoothInfo.bluetoothState) {
                //충전 상태를 알아야하는 상태
                BluetoothState.Connected.Ready,
                BluetoothState.Connected.Reconnected -> {
                    _canMeasurement.emit(bluetoothInfo.canMeasurement)
                }

                else -> {}
            }
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

    fun getResultMessage(): String? {
        return getService()?.getResultMessage()
    }

    fun isRegistered(isConnectAlertShow: Boolean): Boolean {
        if (bluetoothInfo.bluetoothState == BluetoothState.Unregistered || bluetoothInfo.bluetoothState == BluetoothState.DisconnectedByUser || bluetoothInfo.bluetoothGatt == null) {
            Log.d(TAG, "isRegistered: 여기도 콜 baseService")
            /*if (bluetoothInfo.bluetoothState == BluetoothState.DisconnectedByUser) {
                viewModelScope.launch {
                    dataManager.deleteBluetoothDevice(bluetoothInfo.sbBluetoothDevice.type.name)
                }
            }*/
            viewModelScope.launch {
                if (isConnectAlertShow) {
                    _connectAlert.emit(true)
                }
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

    fun showConnectAlert() {
        viewModelScope.launch {
            _connectAlert.emit(true)
        }
    }

    fun getService(): BLEService? {
        return if (::service.isInitialized) {
            this.service.get()
        } else {
            null
        }
    }
    fun dismissGuideAlert(){
        viewModelScope.launch {
            _guideAlert.emit(false)
        }
    }

}