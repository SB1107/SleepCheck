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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.service.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.firebasedb.RealData
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.DataFlowInfo
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import okhttp3.internal.notify
import java.lang.ref.WeakReference
import java.util.Timer
import kotlin.concurrent.timerTask

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
    val canMeasurementAndBluetoothButtonState: StateFlow<Pair<Boolean, String>> = _bluetoothButtonState.combine(_canMeasurement) { bluetoothButtonState, canMeasurement ->
        Pair(canMeasurement, bluetoothButtonState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), Pair(true, "시작"))
    protected var bluetoothInfo = ApplicationManager.getBluetoothInfo()

    private val _isHomeBleProgressBar: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(Pair(false, ""))
    val isHomeBleProgressBar: StateFlow<Pair<Boolean, String>> = _isHomeBleProgressBar.asStateFlow()
    private val _guideAlert: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val guideAlert: StateFlow<Boolean> = _guideAlert

    private val _dataFlowInfoMessage: MutableStateFlow<DataFlowInfo> = MutableStateFlow(DataFlowInfo())
    val dataFlowInfoMessage: StateFlow<DataFlowInfo> = _dataFlowInfoMessage

    private val _dataFlowPopUp: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val dataFlowPopUp: StateFlow<Boolean> = _dataFlowPopUp

    private val _blueToothErrorMessage: MutableSharedFlow<String> = MutableSharedFlow()
    val blueToothErrorMessage: SharedFlow<String> = _blueToothErrorMessage.asSharedFlow()

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
                            launch {
                                _bluetoothButtonState.emit("연결")
                                setIsHomeBleProgressBar()
                            }
                        }

                        BluetoothState.DisconnectedByUser -> {
                            launch {
                                Log.e(TAG, "BluetoothState.DisconnectedByUser ")
                                bluetoothInfo.batteryInfo = null
                                setIsHomeBleProgressBar()
                                _batteryState.emit("")
                                _bluetoothButtonState.emit("연결")
                            }
                        }

                        BluetoothState.Connected.Reconnected -> {
                            _bluetoothButtonState.emit("재 연결중")
                            setBatteryInfo()
                        }

                        BluetoothState.DisconnectedNotIntent -> {
                            launch {
                                bluetoothInfo.batteryInfo = null
                                _batteryState.emit("")
                                _bluetoothButtonState.emit("연결 끊김")
                            }
                        }

                        BluetoothState.Connected.Ready,
                        BluetoothState.Connected.ReceivingRealtime,
                        BluetoothState.Connected.SendDownloadContinue,
                        BluetoothState.Connected.End -> {
                            launch {
                                _bluetoothButtonState.emit("시작")
                                setIsHomeBleProgressBar()
                            }
                        }

                        BluetoothState.Connected.WaitStart -> {
                            launch {
                                _bluetoothButtonState.emit("시작")
                                setIsHomeBleProgressBar(true, ApplicationManager.instance.getString(R.string.sensor_info_wait))
                            }
                        }

                        BluetoothState.Connected.Finish -> {
                            launch {
                                _bluetoothButtonState.emit("시작")
                                setIsHomeBleProgressBar(true, ApplicationManager.instance.getString(R.string.sensor_info_wait))
                            }
                        }

                        BluetoothState.Connecting -> {
                            setIsHomeBleProgressBar(true, ApplicationManager.instance.getString(R.string.sensor_conneting))
                            _bluetoothButtonState.emit("재 연결중")
//                                getService()?.timerOfDisconnection()
                        }

                        BluetoothState.Connected.DataFlow,
                        BluetoothState.Connected.DataFlowUploadFinish -> {
                            _guideAlert.emit(false)
                        }

                        else -> {
                            _bluetoothButtonState.emit("시작")
                            setIsHomeBleProgressBar(true, ApplicationManager.instance.getString(R.string.sensor_info_wait))
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

    private suspend fun setIsHomeBleProgressBar(onOff: Boolean = false, massage: String = "") {
        _isHomeBleProgressBar.emit(Pair(onOff, massage))
    }

    fun reConnectBluetooth() {
        getService()?.forceConnectDevice {
            if (it != "success") {
                sendErrorMessage(it)
            }
        }
    }

    open fun serviceSettingCall() {
        viewModelScope.launch {
            getService()?.getDataFlowPopUp()?.collectLatest {
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
                setIsHomeBleProgressBar()
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


    fun sendBlueToothErrorMessage(msg: String) {
        viewModelScope.launch {
            _blueToothErrorMessage.emit(msg)
        }
    }

    fun isRegistered(isConnectAlertShow: Boolean): Boolean {
        if (bluetoothInfo.bluetoothState == BluetoothState.Unregistered
            || bluetoothInfo.bluetoothState == BluetoothState.DisconnectedByUser
            || bluetoothInfo.bluetoothGatt == null
            || (bluetoothInfo.bluetoothState == BluetoothState.DisconnectedNotIntent && getService()?.getTime() == 0)
        ) {
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

    fun dismissGuideAlert() {
        viewModelScope.launch {
            _guideAlert.emit(false)
        }
    }

}