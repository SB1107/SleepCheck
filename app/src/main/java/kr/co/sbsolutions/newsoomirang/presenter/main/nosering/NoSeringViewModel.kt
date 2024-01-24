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
import kr.co.sbsolutions.newsoomirang.common.DataManager
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
import javax.inject.Inject

@HiltViewModel
class NoSeringViewModel  @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager,tokenManager) {
    private val _showMeasurementCancelAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementCancelAlert: SharedFlow<Boolean> = _showMeasurementCancelAlert
    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _measuringState: MutableSharedFlow<MeasuringState> = MutableSharedFlow()
    val measuringState: SharedFlow<MeasuringState> = _measuringState
    private val _sleepDataResultFlow: MutableSharedFlow<SleepDataResultModel> = MutableSharedFlow()
    val sleepDataResultFlow: SharedFlow<SleepDataResultModel> = _sleepDataResultFlow
    private var mSnoreTime : Long = 0
    private var mLastEventTime : Long  = 0
    private var  mContSnoringTime : Long = 0
    private  var motorCheckBok : Boolean = true
    private  var type : Int = 2

    init {
        viewModelScope.launch {
            launch {
                ApplicationManager.getBluetoothInfoFlow().collectLatest {info ->
                    if (info.bluetoothState == BluetoothState.Connected.End) {
                        stopTimer()
                    }
                }

            }
        }

    }
    fun startClick() {
        if(isRegistered()){
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
        timerJobCancel()
        setMeasuringState(MeasuringState.InIt)
        sleepDataDelete()
        viewModelScope.launch {
            getService()?.stopSBSensor(snoreTime = mSnoreTime / 1000 / 60)
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
    fun sleepDataCreate(){
        startTimer()
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first()?.let {
                request { authAPIRepository.postSleepDataCreate(SleepCreateModel(it, type = SleepType.NoSering.ordinal.toString())) }
                    .collectLatest {
                        it.result?.id?.let { id ->
                            getService()?.startSBSensor(id, SleepType.NoSering,)
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
    fun noSeringResult(){

    }

    fun stopClick() {
        if (getTime() < 300) {
            viewModelScope.launch {
                _showMeasurementCancelAlert.emit(true)
            }
            return
        }
        timerJobCancel()

        setMeasuringState(MeasuringState.Analytics)
        viewModelScope.launch {
            getService()?.stopSBSensor(mSnoreTime)
        }
    }
    fun setType(type : Int){
        this.type = type
    }
    fun callVibrationNotifications(){
        Log.e("Aa","callVibrationNotifications")
        if (!this.motorCheckBok) {
            return
        }
        getService()?.callVibrationNotifications(type)
        Log.e("Aa","callVibrationNotifications2")
    }
    fun setMotorCheckBox(isChecked : Boolean){
        this.motorCheckBok = isChecked
    }
    fun noSeringResultData(){
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.getSleepDataResult() }
                .collectLatest {
                    Log.e("aa",it.toString())
                }
        }

    }
    fun noSeringResult(results: List<Category?>?, inferenceTime: Long?){
        results?.forEach {value ->
            if (value?.index == 38) { // 코골이만 측정
                val currentTime = System.currentTimeMillis()
                if (currentTime - mLastEventTime < 10000) {
                    val timeDelta: Long = currentTime - mLastEventTime
                    mSnoreTime += timeDelta
                    mContSnoringTime += timeDelta
                    if (mContSnoringTime > 10000) {
                        Log.e("Aa","mContSnoringTime")
                        callVibrationNotifications()
                    }
                } else {
                    mContSnoringTime = 0
                }
                mLastEventTime = currentTime
                mSnoreTime += inferenceTime ?: 0
            }
        }

    }

}