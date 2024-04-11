package kr.co.sbsolutions.newsoomirang.presenter.main.nosering

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
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.MeasuringState
import javax.inject.Inject

@HiltViewModel
class NoSeringViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    private val settingDataRepository: SettingDataRepository
) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _showMeasurementCancelAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementCancelAlert: SharedFlow<Boolean> = _showMeasurementCancelAlert
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _measuringState: MutableStateFlow<MeasuringState> = MutableStateFlow(MeasuringState.InIt)
    val measuringState: StateFlow<MeasuringState> = _measuringState
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer

    private var motorCheckBok: Boolean = true
    private var type: Int = 2

    private val _motorCheckBox: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val motorCheckBox: SharedFlow<Boolean> = _motorCheckBox

    private val _intensity: MutableSharedFlow<Int> = MutableSharedFlow()
    val intensity: SharedFlow<Int> = _intensity

    private val _isRegisteredMessage: MutableSharedFlow<String> = MutableSharedFlow()
    val isRegisteredMessage: SharedFlow<String> = _isRegisteredMessage


    init {
        registerJob("NoSeringViewModelInit",
            viewModelScope.launch(Dispatchers.IO) {
                launch {
                    settingDataRepository.getSnoringOnOff().let {
                        _motorCheckBox.emit(it)
                    }
                }

                launch {
                    settingDataRepository.getSnoringVibrationIntensity().let {
                        _intensity.emit(it)
                    }
                }
            }
            )
    }

    fun startClick() {
//        Log.d(TAG, "startClick: ${isRegistered()}")
        if (isRegistered(isConnectAlertShow = false)) {
            if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.End ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.ForceEnd ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.Reconnected
            )
                viewModelScope.launch {
                    dataManager.setHasSensor(true)
                    getService()?.let {
                        _showMeasurementAlert.emit(true)
                    } ?: run {
                        insertLog("서비스가 없습니다.")
                        reLoginCallBack()
                    }
                } else if (bluetoothInfo.bluetoothState == BluetoothState.Connected.ReceivingRealtime) {
                sendErrorMessage("호흡 측정중 입니다. 종료후 사용해 주세요")
            }
        } else {
            viewModelScope.launch {
                _isRegisteredMessage.emit("숨이랑 기기와 연결이 되지 않았습니다.\n코골이 만 측정하겠습니까?")
            }
        }
    }

    fun forceStartClick() {
        registerJob("forceStartClick()",
            viewModelScope.launch {
                _showMeasurementAlert.emit(true)
                dataManager.setHasSensor(false)
            }
        )
    }

    fun bluetoothConnect() {
        showConnectAlert()
    }

    fun cancelClick() {
        setMeasuringState(MeasuringState.InIt)
        sleepDataDelete()
        viewModelScope.launch {
            if (dataManager.getHasSensor().first()) {
                getService()?.stopSBSensor(true)
                return@launch
            }
            getService()?.noSensorSeringMeasurement(true) ?: insertLog("코골이 측정 중 서비스가 없습니다.")
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
        insertLog {
            sleepDataCreate()
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first().let {
                request { authAPIRepository.postSleepDataCreate(SleepCreateModel(it, type = SleepType.NoSering.ordinal.toString())) }
                    .catch {
                        trySend(false)
                        close()
                    }
                    .collectLatest {
                        it.result?.id?.let { id ->
                            getService()?.startSBSensor(dataId = id, sleepType = SleepType.NoSering, hasSensor = dataManager.getHasSensor().first())
                            setMeasuringState(MeasuringState.Record)
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


    fun stopClick() {
        insertLog { stopClick() }
        setMeasuringState(MeasuringState.InIt)
        viewModelScope.launch {
            val hasSensor = dataManager.getHasSensor().first()
            Log.d(TAG, "stopClick 코골이: $hasSensor")
            if (hasSensor) {
                getService()?.checkDataSize()?.collectLatest {
                    if (it) {
                        _showMeasurementCancelAlert.emit(true)
                        return@collectLatest
                    }
                    getService()?.stopSBSensor() ?: insertLog("코골이 측정 중 서비스가 없습니다.")
                }
            } else {
                if ((getService()?.timeHelper?.getTime() ?: 0) < 300) {
                    _showMeasurementCancelAlert.emit(true)
                    return@launch
                }
                getService()?.noSensorSeringMeasurement() ?: insertLog("코골이 측정 중 서비스가 없습니다.")
            }
        }
    }

    fun setType(type: Int) {
        this.type = type
        viewModelScope.launch(Dispatchers.IO) {
            settingDataRepository.setSnoringVibrationIntensity(type)
        }
    }


    fun setMotorCheckBox(isChecked: Boolean) {
        this.motorCheckBok = isChecked
        registerJob("setMotorCheckBox",
            viewModelScope.launch(Dispatchers.IO) {
                settingDataRepository.setSnoringOnOff(isChecked)
                _motorCheckBox.emit(isChecked)
            }
        )
    }

    /*    fun noSeringResultData() {
            viewModelScope.launch(Dispatchers.IO) {
                request { authAPIRepository.getSleepDataResult() }
                    .collectLatest {
                        Log.e("aa", it.toString())
                    }
            }

        }*/


    override fun whereTag(): String {
        return SleepType.NoSering.name
    }

    override fun serviceSettingCall() {
        viewModelScope.launch(Dispatchers.IO) {
            getService()?.timeHelper?.measuringTimer?.collectLatest {
                if (bluetoothInfo.sleepType == SleepType.NoSering) {
                    _measuringTimer.emit(it)
                }
            }
        }
    }
}