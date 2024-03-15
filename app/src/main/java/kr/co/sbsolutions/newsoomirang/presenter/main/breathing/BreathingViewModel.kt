package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _showMeasurementCancelAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementCancelAlert: SharedFlow<Boolean> = _showMeasurementCancelAlert
    private val _measuringState: MutableStateFlow<MeasuringState> = MutableStateFlow(MeasuringState.InIt)
    val measuringState: SharedFlow<MeasuringState> = _measuringState.asSharedFlow()
    private val _capacitanceFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    val capacitanceFlow: SharedFlow<Int> = _capacitanceFlow
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer


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
        if (isRegistered(isConnectAlertShow = true)) {
            if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready ||
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
            BLEService.instance?.stopSBSensor(true)
            setCommend(ServiceCommend.CANCEL)
        }
    }

    fun stopClick() {
        if ((BLEService.instance?.timeHelper?.getTime() ?: 0) < 300) {
            viewModelScope.launch {
                _showMeasurementCancelAlert.emit(true)
            }
            return
        }
        setMeasuringState(MeasuringState.Analytics)
        viewModelScope.launch(Dispatchers.IO) {
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
                            BLEService.instance?.startSBSensor(id, SleepType.Breathing)
                            setMeasuringState(MeasuringState.FiveRecode)
                            trySend(true)
                            close()
                        }
                    }
            }
        }
        awaitClose()
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
            BLEService.instance?.timeHelper?.measuringTimer?.collectLatest {
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