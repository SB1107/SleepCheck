package kr.co.sbsolutions.newsoomirang.presenter.main.nosering

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.domain.model.NoSeringDataResultModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataResultModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.MeasuringState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.withsoom.utils.TokenManager
import org.tensorflow.lite.support.label.Category
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class NoSeringViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _showMeasurementCancelAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementCancelAlert: SharedFlow<Boolean> = _showMeasurementCancelAlert
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _measuringState: MutableSharedFlow<MeasuringState> = MutableSharedFlow()
    val measuringState: SharedFlow<MeasuringState> = _measuringState
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer

    private var motorCheckBok: Boolean = true
    private var type: Int = 2
    private val _noSeringDataResultFlow: MutableSharedFlow<NoSeringDataResultModel> = MutableSharedFlow()
    val noSeringDataResult: SharedFlow<NoSeringDataResultModel> = _noSeringDataResultFlow


    fun startClick() {
        if (isRegistered()) {
            if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Init ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.End ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.ForceEnd
            )
                viewModelScope.launch {
                    _showMeasurementAlert.emit(true)
                }
        }
    }

    fun cancelClick() {
        setMeasuringState(MeasuringState.InIt)
        sleepDataDelete()
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

    fun sleepDataCreate() {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first()?.let {
                request { authAPIRepository.postSleepDataCreate(SleepCreateModel(it, type = SleepType.NoSering.ordinal.toString())) }
                    .collectLatest {
                        it.result?.id?.let { id ->
                            getService()?.startSBSensor(id, SleepType.NoSering)
                            setMeasuringState(MeasuringState.Record)
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

    fun noSeringResult() {
        Log.d(TAG, "noSeringResult: $ 측정 완료 결과 보기")
        viewModelScope.launch {
            request { authAPIRepository.getNoSeringDataResult() }
                .collectLatest {
                    it.result?.let { result ->
                        _measuringState.emit(if (result.state == 0) MeasuringState.Analytics else MeasuringState.Result)
                        val startedAt = result.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
                        val endedAt = result.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
                        val endedAtString = endedAt?.toDayString("M월 d일 E요일") ?: ""
                        val durationString: String = (startedAt?.toDayString("HH:mm") + "~" + endedAt?.toDayString("HH:mm"))
                        val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
                        val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()
                        val snoreTime = (result.noSeringTime * 60).toHourMinute()
                        _noSeringDataResultFlow.emit(
                            NoSeringDataResultModel(
                                endDate = endedAtString,
                                duration = "$durationString 수면",
                                resultTotal = min,
                                resultReal = snoreTime,
                                apneaState = result.apneaState
                            )
                        )
                    } ?: _measuringState.emit(MeasuringState.InIt)
                }
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

    fun setType(type: Int) {
        this.type = type
    }

    fun callVibrationNotifications() {
        if (!this.motorCheckBok) {
            return
        }
        getService()?.callVibrationNotifications(type)
    }

    fun setMotorCheckBox(isChecked: Boolean) {
        this.motorCheckBok = isChecked
    }

    fun noSeringResultData() {
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.getSleepDataResult() }
                .collectLatest {
                    Log.e("aa", it.toString())
                }
        }

    }



    override fun whereTag(): String {
        return  SleepType.NoSering.name
    }

    override fun serviceSettingCall() {
        viewModelScope.launch {
                getService()?.timeHelper?.measuringTimer?.collectLatest {
                    if (bluetoothInfo.sleepType == SleepType.NoSering) {
                    _measuringTimer.emit(it)
                }
            }
        }
    }
}