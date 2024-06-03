package kr.co.sbsolutions.sleepcheck.presenter.main.nosering

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
import kr.co.sbsolutions.sleepcheck.presenter.main.breathing.MeasuringState
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
    private lateinit var dataRemovedJob: Job

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
                sendErrorMessage(ApplicationManager.instance.baseContext.getString(R.string.nosering_exit_error_message))
            }
        } else {
            viewModelScope.launch {
                _isRegisteredMessage.emit(ApplicationManager.instance.baseContext.getString(R.string.nosering_connected_and_start_message))
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

    fun ralDataRemovedObservers() {
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
//
//            Log.d(TAG, "realDataChange: ${hasSensor} ")
//            Log.d(TAG, "realDataChange: ${realData.sleepType } ")
//            Log.d(TAG, "realDataChange: ${realData.sensorName } ")
//            Log.d(TAG, "realDataChange: ${dataId } ")
//            Log.d(TAG, "realDataChange: ${realData.dataId}} ")

            if (hasSensor &&
                realData.sensorName == sensorName &&
                realData.dataId == dataId.toString()
            ) {
                sendErrorMessage("다른 사용자가 센서 사용을 하여 종료 합니다.")
                cancelClick()
            }
        }

    }
    /*
 종료시 연결이 끊어져 있어 검색후 다시 연결시 강제 업로드 콜백
 으로 오기 때문에 UI 초기화 및 타이머 초기화
 */
    fun forceUploadResetUIAndTimer() {
        getService()?.forceUploadStop {
            setMeasuringState(MeasuringState.InIt)
        } ?: insertLog("코골이 측정 중 서비스가 없습니다.")
    }

    fun cancelClick(isForce: Boolean = false) {
        sleepDataDelete()
        viewModelScope.launch {
            Log.d(TAG, "cancelClick1212: ${dataManager.getHasSensor().first()}")
        }
        registerJob("cancelClick",
            viewModelScope.launch {
                if (isForce.not()) {
                    if (dataManager.getHasSensor().first()) {
                        getService()?.stopSBSensor(true, callback = {
                            setMeasuringState(MeasuringState.InIt)
                        })
                        setCommend(ServiceCommend.CANCEL)
                        cancel()
                        return@launch
                    }
                }
                getService()?.noSensorSeringMeasurement(true, callback = {
                    setMeasuringState(MeasuringState.InIt)
                }) ?: insertLog("코골이 측정 중 서비스가 없습니다.")
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

    fun sleepDataCreate(): Flow<Boolean> = callbackFlow {
        insertLog {
            sleepDataCreate()
        }
        viewModelScope.launch(Dispatchers.IO) {
            val hasSensor = dataManager.getHasSensor().first()
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first().let {
                request { authAPIRepository.postSleepDataCreate(SleepCreateModel(if (hasSensor) it else null, type = SleepType.NoSering.ordinal.toString())) }
                    .catch {
                        trySend(false)
                        close()
                    }
                    .collectLatest {
                        it.result?.id?.let { id ->
                            Log.d(TAG, "hasSensor: $hasSensor")
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

    fun getIntensity() = callbackFlow {
        viewModelScope.launch(Dispatchers.IO) {
            val intensity = settingDataRepository.getSnoringVibrationIntensity()
            val on = settingDataRepository.getSnoringOnOff()
            send(Pair(on, intensity))
            close()
        }
        awaitClose()
    }


    fun stopClick() {
        if (::dataRemovedJob.isInitialized) {
            dataRemovedJob.cancel()
        }

        viewModelScope.launch {
            val hasSensor = dataManager.getHasSensor().first()
            insertLog("stopClick 코골이: $hasSensor")
            if (hasSensor) {
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
                    }) ?: insertLog("코골이 측정 중 서비스가 없습니다.")

                }
            } else {
                if ((getService()?.getTime() ?: 0) < 300) {
                    _showMeasurementCancelAlert.emit(true)
                    cancel()
                    return@launch
                }
                getService()?.noSensorSeringMeasurement(callback = {
                    setMeasuringState(MeasuringState.InIt)
                }) ?: insertLog("코골이 측정 중 서비스가 없습니다.")

                setMeasuringState(MeasuringState.InIt)
            }
        }

    }

    fun setType(type: Int) {
        this.type = type
        viewModelScope.launch(Dispatchers.IO) {
            settingDataRepository.setSnoringVibrationIntensity(type)
            getService()?.motorTest(type)
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
            getService()?.getTimeHelper()?.collectLatest {
                if (bluetoothInfo.sleepType == SleepType.NoSering) {
                    _measuringTimer.emit(it)
                }
            }
        }
    }
}