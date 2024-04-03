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
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject
import kotlin.math.log

@HiltViewModel
class MainViewModel @Inject constructor(
    val dataManager: DataManager,
    tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _isResultProgressBar: MutableStateFlow<Pair<Int, Boolean>> = MutableStateFlow(Pair(-1 , false))
    val isResultProgressBar: SharedFlow<Pair<Int, Boolean>> = _isResultProgressBar.asSharedFlow()

    private val _isAppGuideFirst: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isAppGuideFirst: SharedFlow<Boolean> = _isAppGuideFirst.asSharedFlow()

    private val _dataIDSet = mutableSetOf<Int>()
    private lateinit var job: Job
    private lateinit var resultJob: Job
    
    init {
        getAppGuide()
    }

    private fun getAppGuide() {
        Log.d(TAG, "getAppGuide11111111: ")
        viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, "getAppGuide22222222 ${dataManager.isFirstExecute().first()} ")
            _isAppGuideFirst.emit(dataManager.isFirstExecute().first())
        }
    }

    fun setAppGuide(check: Boolean) {
        if (check){
            Log.d(TAG, "setAppGuide: $check")
            viewModelScope.launch {
                dataManager.setFirstExecuted()
            }
        }
    }
    fun stopResultProgressBar() {
        viewModelScope.launch {
            _isResultProgressBar.emit(Pair(-1, false))
        }
    }

    fun sendMeasurementResults() {
        if (::job.isInitialized) {
            job.cancel()
        }
        job = viewModelScope.launch(Dispatchers.IO) {


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


    private fun sleepDataResult() {
        if(::resultJob.isInitialized){
            resultJob.cancel()
        }
        resultJob = viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(Pair(-1, true))
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
        if(::resultJob.isInitialized){
            resultJob.cancel()
        }
        resultJob = viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(Pair(-1, true))
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
                    if (_dataIDSet.contains(result.id.toInt()).not() && result.state != 1) {
                        _dataIDSet.add(result.id.toInt())
                        _isResultProgressBar.emit(Pair(result.id.toInt(), false))
                    }else{
                        _isResultProgressBar.emit(Pair(-1,  result.state == 1))
                    }
                        if((result.state != 1)){
                            jobCancel()
                        }
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        if (result.state == 1) {
                            sleepDataResult()
                        }
                    }
                } ?: _isResultProgressBar.emit(Pair(-1, true))
            }
    }

    private fun jobCancel() {
        if (::job.isInitialized) {
            job.cancel()
        }
        if (::resultJob.isInitialized) {
            resultJob.cancel()
        }
    }

    private suspend fun snoSeringResultRequest() {
        request(showProgressBar = false) { authAPIRepository.getNoSeringDataResult() }
            .collectLatest {
                it.result?.let { result ->
                    if (_dataIDSet.contains(result.id.toInt()).not() && result.state != 1) {
                        _dataIDSet.add(result.id.toInt())
                        _isResultProgressBar.emit(Pair(result.id.toInt(), false))
                    }else{
                        _isResultProgressBar.emit(Pair(-1, result.state == 1))
                    }
                        if((result.state != 1).not()){
                            jobCancel()
                        }
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        if (result.state == 1) {
                            noSeringResult()
                        }
                    }
                } ?: _isResultProgressBar.emit(Pair(-1, true))
            }
    }

    override fun whereTag(): String {
        return "Main"
    }

}

enum class ServiceCommend {
    START, STOP, CANCEL
}