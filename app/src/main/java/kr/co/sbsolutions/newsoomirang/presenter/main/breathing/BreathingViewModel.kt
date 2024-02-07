package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataResultModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _showMeasurementCancelAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementCancelAlert: SharedFlow<Boolean> = _showMeasurementCancelAlert
    private val _measuringState: MutableStateFlow<MeasuringState> = MutableStateFlow(MeasuringState.InIt)
    val measuringState: SharedFlow<MeasuringState> = _measuringState.asSharedFlow()
    val _capacitanceFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    val capacitanceFlow: SharedFlow<Int> = _capacitanceFlow
    private val _sleepDataResultFlow: MutableSharedFlow<SleepDataResultModel> = MutableSharedFlow()
    val sleepDataResultFlow: SharedFlow<SleepDataResultModel> = _sleepDataResultFlow
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer

    private val _isResultProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isResultProgressBar: SharedFlow<Boolean> = _isResultProgressBar

    init {
        viewModelScope.launch {
            launch {
                ApplicationManager.getBluetoothInfoFlow().collect { info ->
                    if (info.bluetoothState == BluetoothState.Connected.SendRealtime ||
                        info.bluetoothState == BluetoothState.Connected.ReceivingRealtime &&
                        info.sleepType == SleepType.Breathing
                    ) {
                        info.currentData.collectLatest {
//                            Log.d(TAG, ": $it")
                            _capacitanceFlow.emit(it)
                        }
                    }

                }
            }
        }
    }

    fun startClick() {
        if (isRegistered()) {
            if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Init ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.End ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.ForceEnd
            ) {
                viewModelScope.launch {
                    _showMeasurementAlert.emit(true)
                }
            } else if (bluetoothInfo.bluetoothState == BluetoothState.Connected.ReceivingRealtime) {
                sendErrorMessage("코골이 측정중 입니다. 종료후 사용해 주세요")
            }
        }
    }

    fun cancelClick() {
        setMeasuringState(MeasuringState.InIt)
        sleepDataDelete()
        viewModelScope.launch {
            getService()?.stopSBSensor(true)
            setCommend(ServiceCommend.CANCEL)
        }
    }

    fun stopClick() {
        if (getService()?.timeHelper?.getTime() ?: 0 < 300) {
            viewModelScope.launch {
                _showMeasurementCancelAlert.emit(true)
            }
            return
        }
        setMeasuringState(MeasuringState.Analytics)
        viewModelScope.launch {
            getService()?.stopSBSensor()
        }
    }

    private fun sleepDataDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.postSleepDataRemove(SleepDataRemoveModel(bluetoothInfo.dataId ?: -1)) }
                .collectLatest {
                    bluetoothInfo.dataId = null
                }
        }
    }

    fun sleepDataCreate(): Flow<Boolean> = callbackFlow {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first()?.let {
                request { authAPIRepository.postSleepDataCreate(SleepCreateModel(it)) }
                    .catch {
                        trySend(false)
                        close()
                    }
                    .collectLatest {
                        it.result?.id?.let { id ->
                            getService()?.startSBSensor(id, SleepType.Breathing)
                            setMeasuringState(MeasuringState.FiveRecode)
                            trySend(true)
                            close()
                        }
                    }
            }
        }
        awaitClose()
    }

    fun sleepDataResult() {
        if (_measuringState.value == MeasuringState.Charging) {
            showCharging()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(true)
            request(showProgressBar = false) { authAPIRepository.getSleepDataResult() }
                .collectLatest {
                    it.result?.let { result ->
                        Log.d(TAG, "sleepDataResult: $result")
                        if (_measuringState.value == MeasuringState.Result) {
                            Log.d(TAG, "sleepDataResult: 11111")
                            _isResultProgressBar.emit(false)
                            return@let
                        }
                        _measuringState.emit(if (result.state == 1) MeasuringState.Analytics else MeasuringState.Result)
                        val startedAt = result.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
                        val endedAt = result.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
                        val endedAtString = endedAt?.toDayString("M월 d일 E요일") ?: ""
                        val durationString: String = (startedAt?.toDayString("HH:mm") + "~" + endedAt?.toDayString("HH:mm"))
                        val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
                        val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()
                        val sleepTime = (result.sleepTime * 60).toHourMinute()
                        val resultAsleep = (result.asleepTime * 60).toHourMinute()
                        val deepSleepTime = (result.deepSleepTime * 60).toHourMinute()
                        val moveCount = (result.moveCount).toString()
                        _sleepDataResultFlow.emit(
                            SleepDataResultModel(
                                endDate = endedAtString,
                                duration = "$durationString 수면",
                                resultTotal = min,
                                resultReal = sleepTime,
                                resultAsleep = resultAsleep,
                                apneaState = result.apneaState,
                                moveCount = moveCount,
                                deepSleepTime = deepSleepTime
                            )
                        )
                        _isResultProgressBar.emit(result.state == 1)
                        Log.d(TAG, "sleepDataResult: 22222 ${result.state == 1}")
                        viewModelScope.launch(Dispatchers.IO) {
                            delay(4000)
                            if (_measuringState.value == MeasuringState.Analytics) {
                                sleepDataResult()
                            }
                        }

                    } ?: _measuringState.emit(MeasuringState.InIt).run {
                        Log.d(TAG, "sleepDataResult: 33333")
                        _isResultProgressBar.emit(false)
                    }
        }
    }


}

fun setMeasuringState(state: MeasuringState) {
    viewModelScope.launch {
        _measuringState.emit(state)
    }
}

override fun whereTag(): String {
    return SleepType.Breathing.name
}

override fun serviceSettingCall() {
    viewModelScope.launch(Dispatchers.IO) {
        getService()?.timeHelper?.measuringTimer?.collectLatest {
            if (bluetoothInfo.sleepType == SleepType.Breathing) {
                _measuringTimer.emit(it)
            }
        }
    }
}
}

enum class MeasuringState {
    InIt, FiveRecode, Record, Analytics, Result, Charging
}