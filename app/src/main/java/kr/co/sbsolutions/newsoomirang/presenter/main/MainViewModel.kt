package kr.co.sbsolutions.newsoomirang.presenter.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.model.NoSeringDataResultModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataResultModel
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.MeasuringState
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _breathingResults: MutableSharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 1)
    private val _noSeringResults: MutableSharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 1)
    @Deprecated("메인에서 처리 하게됨")
    val breathingResults: SharedFlow<Int> = _breathingResults
    @Deprecated("메인에서 처리 하게됨")
    val noSeringResults: SharedFlow<Int> = _noSeringResults
    private val _isResultProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isResultProgressBar: SharedFlow<Boolean> = _isResultProgressBar
    private  lateinit var  job: Job
    fun sendMeasurementResults() {
        viewModelScope.launch(Dispatchers.IO) {
            if (ApplicationManager.getBluetoothInfo().sleepType == SleepType.Breathing) {
                Log.d(TAG, "RESULT: ${ApplicationManager.getBluetoothInfo().sleepType} ")
//                    _breathingResults.emit(0)
                sleepDataResult()
            } else {
                Log.d(TAG, "RESULT: ${ApplicationManager.getBluetoothInfo().sleepType} ")
//                _noSeringResults.emit(0)
                noSeringResult()
            }
        }
    }

    fun canMove(): Pair<Boolean, SleepType> {
        return Pair(bluetoothInfo.bluetoothState != BluetoothState.Connected.ReceivingRealtime, bluetoothInfo.sleepType)
    }

    private fun sleepDataResult() {
        viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(true)
            getResultMessage()?.let {
                if (it != BLEService.FINISH) {
                    delay(2000)
                    Log.d(TAG, "sleepDataResult1: ${getResultMessage()}")
                    sleepDataResult()
                } else {
                    Log.d(TAG, "sleepDataResult2: ${getResultMessage()}")
                    sleepDataResultRequest()
                }
            } ?: sleepDataResultRequest()
        }
    }

    private fun noSeringResult() {
        if (::job.isInitialized) {
            job.cancel()
        }
        job = viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(true)
            getResultMessage()?.let {
                if (it != BLEService.FINISH) {
                    delay(2000)
                    noSeringResult()
                } else {
                    snoSeringResultRequest()
                }
            } ?: snoSeringResultRequest()
        }
    }

    private suspend fun sleepDataResultRequest() {
        request(showProgressBar = false) { authAPIRepository.getSleepDataResult() }
            .collectLatest {
                it.result?.let { result ->
                    _isResultProgressBar.emit(result.state == 1)
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        if (result.state == 1) {
                            sleepDataResult()
                        }
                    }
                } ?: _isResultProgressBar.emit(false)
            }
    }

    private suspend fun snoSeringResultRequest() {
        request(showProgressBar = false) { authAPIRepository.getNoSeringDataResult() }
            .collectLatest {
                it.result?.let { result ->
                    _isResultProgressBar.emit(result.state == 1)
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        if (result.state == 1) {
                            noSeringResult()
                        }
                    }
                } ?: _isResultProgressBar.emit(false)
            }
    }

    override fun whereTag(): String {
        return "Main"
    }

}

enum class ServiceCommend {
    START, STOP, CANCEL
}