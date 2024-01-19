package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LimitedQueue
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val authAPIRepository: RemoteAuthDataSource
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

    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer
    val  _capacitanceFlow : MutableSharedFlow<Int> = MutableSharedFlow()
     val capacitanceFlow: SharedFlow<Int> = _capacitanceFlow

    lateinit var timerJob: Job
    private var time: Int = 0

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getUserName().first()?.let {
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

        if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Init || bluetoothInfo.bluetoothState == BluetoothState.Connected.Init) {
            viewModelScope.launch {
                _showMeasurementAlert.emit(true)
            }
        }
    }

    fun stopClick() {
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
        setMeasuringState(MeasuringState.Analytics)
        viewModelScope.launch {
            getService()?.stopSBSensor()
        }
    }

    fun sleepDataCreate() {
        startTimer()
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first()?.let {
                request { authAPIRepository.postSleepDataCreate(SleepCreateModel(it)) }
                    .collectLatest {
                        it.result?.id?.let { id ->
                            getService()?.startSBSensor(id, SleepType.Breathing )
                            setMeasuringState(MeasuringState.FiveRecode)
                        }
                    }
            }
        }
    }

    fun setMeasuringState(state: MeasuringState) {
        viewModelScope.launch {
            _measuringState.emit(state)
        }
    }

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        bluetoothInfo = info
        viewModelScope.launch {
            launch {
                info.batteryInfo?.let { _batteryState.emit(it) }
                _canMeasurement.emit(info.canMeasurement)
            }
            launch {
                if (info.bluetoothState == BluetoothState.Connected.SendRealtime || info.bluetoothState == BluetoothState.Connected.ReceivingRealtime && info.sleepType == SleepType.Breathing) {
                    info.currentData.collectLatest {
                        _capacitanceFlow.emit(it)
                    }
                }
            }
        }

        Log.e(TAG, "[BVM]  ${info.bluetoothState}")
    }

    private fun startTimer() {
        time = 0
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                time += 1
                val hour = time / 3600
                val minute = time % 3600 / 60
                val second = time % 60
                _measuringTimer.emit(Triple(hour, minute, second))
            }

        }
    }
}

enum class MeasuringState {
    InIt, FiveRecode, Record, Analytics, Result
}