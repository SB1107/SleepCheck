package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
    private  val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel() {
    private val _userName: MutableSharedFlow<String> = MutableSharedFlow()
    val userName: SharedFlow<String> = _userName
    private val _gotoScan: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val gotoScan: SharedFlow<Boolean> = _gotoScan
    private var bluetoothInfo: BluetoothInfo = BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR)
    private val _batteryState: MutableSharedFlow<String> = MutableSharedFlow()
    val batteryState: SharedFlow<String> = _batteryState
    private val _canMeasurement: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val canMeasurement: SharedFlow<Boolean> = _canMeasurement
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _measuringState: MutableSharedFlow<MeasuringState> = MutableSharedFlow()
    val measuringState: SharedFlow<MeasuringState> = _measuringState


    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getUserName().last()?.let {
                _userName.emit(it)
            }
        }
    }

    fun startClick() {
        if (bluetoothInfo.bluetoothState == BluetoothState.Unregistered) {
            viewModelScope.launch {
                _gotoScan.emit(true)
            }
            return
        }

        if(bluetoothInfo.bluetoothState == BluetoothState.Connected.Init || bluetoothInfo.bluetoothState == BluetoothState.Connected.Init){
            viewModelScope.launch {
                _showMeasurementAlert.emit(true)
            }
        }
    }
    fun sleepDataCreate(){
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first()?.let {
                request {  authAPIRepository.postSleepDataCreate(SleepCreateModel(it) )}
                    .collectLatest {
                        it.result?.id?.let {id ->
                            getService()?.startSBSensor(id, SleepType.Breathing )
                            _measuringState.emit(MeasuringState.FiveRecode)
                        }
                    }
            }
        }
    }

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        bluetoothInfo = info
        viewModelScope.launch {
            info.batteryInfo?.let { _batteryState.emit(it) }
            _canMeasurement.emit(info.canMeasurement)
        }

        Log.e("onChangeSBSensorInfo", "breating = ${info.bluetoothState}")
    }
}
enum class MeasuringState{
    InIt ,FiveRecode,Record,Analytics,Result
}