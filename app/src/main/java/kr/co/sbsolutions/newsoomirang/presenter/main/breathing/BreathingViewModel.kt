package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.util.Log
import androidx.lifecycle.viewModelScope
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
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataResultModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.utils.TokenManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager,tokenManager) {
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _measuringState: MutableSharedFlow<MeasuringState> = MutableSharedFlow()
    val measuringState: SharedFlow<MeasuringState> = _measuringState

    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer
    val  _capacitanceFlow : MutableSharedFlow<Int> = MutableSharedFlow()
     val capacitanceFlow: SharedFlow<Int> = _capacitanceFlow


    private val  _sleepDataResultFlow : MutableSharedFlow<SleepDataResultModel> = MutableSharedFlow()
    val sleepDataResultFlow: SharedFlow<SleepDataResultModel> = _sleepDataResultFlow


    lateinit var timerJob: Job
    private var time: Int = 0


     fun startClick() {
         if (isRegistered()) {
             if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Init ||
                 bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready ||
                 bluetoothInfo.bluetoothState == BluetoothState.Connected.End ||
                 bluetoothInfo.bluetoothState == BluetoothState.Connected.ForceEnd) {
                 viewModelScope.launch {
                     _showMeasurementAlert.emit(true)
                 }
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
    fun sleepDataResult(){
        viewModelScope.launch(Dispatchers.IO) {
            request{authAPIRepository.getSleepDataResult()}
            .collectLatest {
                it.result?.let {result ->
                        _measuringState.emit(if(result.state == 1) MeasuringState.Analytics else MeasuringState.Result)
                        val startedAt = result.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
                        val endedAt = result.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
                        val endedAtString = endedAt?.toDayString("M월 d일 E요일") ?: ""
                        val durationString: String = (startedAt?.toDayString("HH:mm") + "~" + endedAt?.toDayString("HH:mm"))
                        val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
                        val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()
                        val sleepTime = (result.sleepTime * 60).toHourMinute()
                        val resultAsleep = (result.asleepTime * 60).toHourMinute()
                        _sleepDataResultFlow.emit(SleepDataResultModel(endDate = endedAtString, duration = "$durationString 수면"
                            , resultTotal = min, resultReal =  sleepTime, resultAsleep = resultAsleep, apneaState = result.apneaState ))
                }?: _measuringState.emit(MeasuringState.InIt)
            }
        }


    }

    fun setMeasuringState(state: MeasuringState) {
        viewModelScope.launch {
            _measuringState.emit(state)
        }
    }

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        super.onChangeSBSensorInfo(info)
        bluetoothInfo = info
        viewModelScope.launch {

            launch {
                if (info.bluetoothState == BluetoothState.Connected.SendRealtime || info.bluetoothState == BluetoothState.Connected.ReceivingRealtime && info.sleepType == SleepType.Breathing) {
                    info.currentData.collectLatest {
                        _capacitanceFlow.emit(it)
                    }
                }else if(info.bluetoothState == BluetoothState.Connected.End) {
                    stopTimer()
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
    private  fun stopTimer(){
        time = 0
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
    }
}

enum class MeasuringState {
    InIt, FiveRecode, Record, Analytics, Result
}