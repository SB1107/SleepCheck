package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.util.Log
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
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.firebasedb.RealData
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
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
    private val authAPIRepository: RemoteAuthDataSource,
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
        registerJob("Breathing init",
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
            })
    }

    fun startClick() {

        if (isRegistered(isConnectAlertShow = true)) {
            if (bluetoothInfo.bluetoothState == BluetoothState.Connected.Ready ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.End ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.ForceEnd ||
                bluetoothInfo.bluetoothState == BluetoothState.Connected.Reconnected
            ) {
                registerJob(viewModelScope.launch {
                    getService()?.let {
                        _showMeasurementAlert.emit(true)
                    } ?: run {
                        insertLog("서비스가 없습니다.")
                        reLoginCallBack()
                    }
                }) {
                    startClick()
                }

            } else if (bluetoothInfo.bluetoothState == BluetoothState.Connected.ReceivingRealtime) {
                sendErrorMessage("코골이 측정중 입니다. 종료후 사용해 주세요")
            }
        }
    }

    fun cancelClick() {
        setMeasuringState(MeasuringState.InIt)
        sleepDataDelete()
        registerJob("cancelClick",
            viewModelScope.launch {
                getService()?.stopSBSensor(true)
                setCommend(ServiceCommend.CANCEL)
            })

    }

    fun stopClick() {
        registerJob("stopClick()",
            viewModelScope.launch(Dispatchers.Main) {
                if (getService()?.isBleDeviceConnect()?.first?.not() == true) {
                    sendErrorMessage("숨이랑 센서와 연결이 끊겼습니다.\n\n상단의 연결상태를 확인후 다시 시도해 주세요.")
                    return@launch
                }
                getService()?.checkDataSize()?.collectLatest {
                    if (it) {
                        _showMeasurementCancelAlert.emit(true)
                        return@collectLatest
                    }

                    getService()?.stopSBSensor() ?: insertLog("호흡 측중중 서비스가 없습니다.")
                    setMeasuringState(MeasuringState.InIt)
                }
            }
        )
    }

    private fun sleepDataDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.postSleepDataRemove(SleepDataRemoveModel(bluetoothInfo.dataId ?: -1)) }
                .collectLatest {
                    getService()?.removeDataId()
                }
        }
    }
    fun ralDataRemovedObservers(){
        viewModelScope.launch(Dispatchers.IO) {
            getService()?.getRealDataRemoved()?.collectLatest {
                realDataChange(it, bluetoothInfo)
            }
        }
    }


    //파이어 베이스 데이터 지워짐
    private fun realDataChange(realData: RealData, info: BluetoothInfo) {
        //리무브 데이터 내가  액션을 취하지 않았을때 초기화
        Log.d(TAG, "realDataChange: ${info.dataId}")
        viewModelScope.launch(Dispatchers.IO) {
            val hasSensor = dataManager.getHasSensor().first()
            val sensorName = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first() ?: ""
            if (hasSensor && realData.sleepType == SleepType.Breathing.name && realData.sensorName ==   sensorName && realData.dataId != info.dataId.toString() ) {
                sendErrorMessage("다른 사용자가 센서 사용을 하여 종료 합니다.")
                cancelClick()
            }
        }
    }

    fun sleepDataCreate(): Flow<Boolean> = callbackFlow {
        registerJob("sleepDataCreate()",
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
        )
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
            getService()?.getTimeHelper()?.collectLatest {
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