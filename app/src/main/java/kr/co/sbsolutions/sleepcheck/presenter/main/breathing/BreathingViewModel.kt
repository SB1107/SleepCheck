package kr.co.sbsolutions.sleepcheck.presenter.main.breathing

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.ApplicationManager
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.firebasedb.RealData
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.model.SleepCreateModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepType
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceViewModel
import kr.co.sbsolutions.sleepcheck.presenter.main.ServiceCommend
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    private val settingDataRepository: SettingDataRepository
) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _showMeasurementCancelAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementCancelAlert: SharedFlow<Boolean> = _showMeasurementCancelAlert
    private val _measuringState: MutableStateFlow<MeasuringState> = MutableStateFlow(MeasuringState.InIt)
    val measuringState: StateFlow<MeasuringState> = _measuringState
    private val _capacitanceFlow: MutableSharedFlow<Int> = MutableSharedFlow()
    val capacitanceFlow: SharedFlow<Int> = _capacitanceFlow
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer
    private lateinit var dataRemovedJob: Job

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
                sendErrorMessage(ApplicationManager.instance.baseContext.getString(R.string.snoring_measurable_finish))
            }
        }
    }

    fun cancelClick(isForce: Boolean = false) {
        sleepDataDelete()
        registerJob("cancelClick",
            viewModelScope.launch {
                if (isForce.not()) {
                    getService()?.stopSBSensor(true, callback = {
                        setMeasuringState(MeasuringState.InIt)
                    })
                    setCommend(ServiceCommend.CANCEL)
                } else {
                    getService()?.forceStopBreathing()
                }
            })
    }

    fun stopClick() {
        if (::dataRemovedJob.isInitialized) {
            dataRemovedJob.cancel()
        }
        registerJob("stopClick()",
            viewModelScope.launch(Dispatchers.Main) {
                if (getService()?.isBleDeviceConnect()?.first?.not() == true) {
                    sendBlueToothErrorMessage(ApplicationManager.instance.baseContext.getString(R.string.sensor_disconnect_error))
                    cancel()
                    return@launch
                }
                getService()?.checkDataSize()?.collectLatest {
                    if (it) {
                        _showMeasurementCancelAlert.emit(true)
                        cancel()
                        return@collectLatest
                    }

                    getService()?.stopSBSensor(callback = {
                        setMeasuringState(MeasuringState.InIt)
                    }) ?: insertLog("호흡 측중중 서비스가 없습니다.")
                }
            }
        )
    }


    private fun sleepDataDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            val dataId = settingDataRepository.getDataId() ?: -1
            request { authAPIRepository.postSleepDataRemove(SleepDataRemoveModel(dataId)) }
                .collectLatest {
                    getService()?.removeDataId()
                }
        }
    }

    fun dataRemovedObservers() {
        if (::dataRemovedJob.isInitialized) {
            dataRemovedJob.cancel()
        }
        dataRemovedJob = viewModelScope.launch(Dispatchers.IO) {
            getService()?.getRealDataRemoved()?.collectLatest {
                it?.let {
                    realDataChange(it)
                }
            }
        }
    }


    //파이어 베이스 데이터 지워짐
    private fun realDataChange(realData: RealData) {
        //리무브 데이터 내가  액션을 취하지 않았을때 초기화
        viewModelScope.launch(Dispatchers.IO) {
            val hasSensor = dataManager.getHasSensor().first()
            val sensorName = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first() ?: ""
            val dataId = settingDataRepository.getDataId() ?: -1

            Log.d(TAG, "realDataChange: ${hasSensor} ")
//            Log.d(TAG, "realDataChange: ${realData.sleepType } ")
            Log.d(TAG, "realDataChange: ${realData.sensorName} ${sensorName}")
            Log.d(TAG, "realDataChange: ${dataId} ")
            Log.d(TAG, "realDataChange: ${realData.dataId}} ")

            if (hasSensor &&
                realData.sensorName == sensorName &&
                realData.dataId == dataId.toString()
            ) {
                sendErrorMessage(ApplicationManager.instance.baseContext.getString(R.string.other_user_sensor))
                cancelClick(true)
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
                                getService()?.startSBSensor(id, SleepType.Breathing){
                                    setMeasuringState(MeasuringState.FiveRecode)

                                    trySend(true)
                                    close()
                                }
                            }
                        }
                }
            }
        )
        awaitClose()
    }
    /*
    종료시 연결이 끊어져 있어 검색후 다시 연결시 강제 업로드 콜백
    으로 오기 때문에 UI 초기화 및 타이머 초기화
    */
    fun forceUploadResetUIAndTimer(){
        getService()?.forceUploadStop{
            setMeasuringState(MeasuringState.InIt)
        } ?: insertLog("호흡 측중중 서비스가 없습니다.")
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