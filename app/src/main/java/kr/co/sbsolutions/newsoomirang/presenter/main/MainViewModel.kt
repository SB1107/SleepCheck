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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _isResultProgressBar: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isResultProgressBar: SharedFlow<Boolean> = _isResultProgressBar.asSharedFlow()
    private val _moveHistory: MutableSharedFlow<Boolean> = MutableSharedFlow()
     val moveHistory: SharedFlow<Boolean> = _moveHistory

    private lateinit var job: Job

    fun stopResultProgressBar(){
        viewModelScope.launch {
            _isResultProgressBar.emit(false)
        }
    }

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

     fun isMoveHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            if (dataManager.getMoveView().first()) {
                _moveHistory.emit(true)
                dataManager.setMoveView(false)
            }
        }
    }


    private fun sleepDataResult() {
        if (::job.isInitialized) {
            job.cancel()
        }
        job = viewModelScope.launch(Dispatchers.IO) {
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
                    result.startedAt?.let { itStartedAt ->
                        val startedAt = itStartedAt.toDate("yyyy-MM-dd HH:mm:ss")
                        result.endedAt?.let { itEndedAt ->
                            val endedAt = itEndedAt.toDate("yyyy-MM-dd HH:mm:ss")
                            val durationString =
                                (startedAt?.toDayString("HH:mm") + "~" + (endedAt?.toDayString("HH:mm")))
                            ApplicationManager.setResultData(result.id, durationString.plus(if (bluetoothInfo.sleepType == SleepType.Breathing) "수면" else "코골이"))
                        }
                    }
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
                    result.startedAt?.let { itStartedAt ->
                        val startedAt = itStartedAt.toDate("yyyy-MM-dd HH:mm:ss")
                        result.endedAt?.let { itEndedAt ->
                            val endedAt = itEndedAt.toDate("yyyy-MM-dd HH:mm:ss")
                            val durationString =
                                (startedAt?.toDayString("HH:mm") + "~" + (endedAt?.toDayString("HH:mm")))
                            ApplicationManager.setResultData(result.id, durationString.plus(if (bluetoothInfo.sleepType == SleepType.Breathing) "수면" else "코골이"))
                        }
                    }
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