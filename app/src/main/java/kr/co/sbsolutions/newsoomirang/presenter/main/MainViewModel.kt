package kr.co.sbsolutions.newsoomirang.presenter.main

import android.os.Build
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.service.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    private val logHelper: LogHelper
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _isResultProgressBar: MutableStateFlow<ResultData> = MutableStateFlow(ResultData(dataId = -1, state = 1, isShow = false))
    val isResultProgressBar: StateFlow<ResultData> = _isResultProgressBar

    private val _dataIDSet = mutableSetOf<Int>()
    private lateinit var job: Job
    private lateinit var resultJob: Job

    init {
        userInfoLog()
    }

    private fun userInfoLog() {
        viewModelScope.launch {
            logHelper.insertLog("[M] Model Name: ${Build.MODEL} OS API:${Build.VERSION.SDK_INT} ${dataManager.getUserName().first().toString()}")
        }
    }
    /*private fun getAppGuide() {
        Log.d(TAG, "getAppGuide11111111: ")
        viewModelScope.launch(Dispatchers.IO) {
        Log.d(TAG, "getAppGuide22222222 ${dataManager.isFirstExecute().first()} ")
            _isAppGuideFirst.emit(dataManager.isFirstExecute().first())
        }
    }*/

    /*fun setAppGuide(check: Boolean) {
        if (check){
            Log.d(TAG, "setAppGuide: $check")
            viewModelScope.launch {
                dataManager.setFirstExecuted()
            }
        }
    }*/
    fun stopResultProgressBar() {
        registerJob("stopResultProgressBar()",
            viewModelScope.launch {
                _isResultProgressBar.emit(ResultData(dataId = -1, state = 1, isShow = false))
            }
        )
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

    fun forceDataFlowUpdate() {
        getService()?.forceDataFlowDataUpload()
    }

    fun forceDataFlowCancel() {
        getService()?.forceDataFlowDataUploadCancel()
    }


    private fun sleepDataResult() {
        if (::resultJob.isInitialized) {
            resultJob.cancel()
        }
        resultJob = viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(ResultData(dataId = -1, state = 1, isShow = true))
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
        if (::resultJob.isInitialized) {
            resultJob.cancel()
        }
        resultJob = viewModelScope.launch(Dispatchers.IO) {
            _isResultProgressBar.emit(ResultData(dataId = -1, state = 1, isShow = true))
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
                    if (_dataIDSet.contains(result.id.toInt()).not() && result.state == 3) {
                        _dataIDSet.add(result.id.toInt())
                        _isResultProgressBar.emit(ResultData(dataId = -1, state = result.state, isShow = false))
                        sendErrorMessage(ApplicationManager.instance.getString(R.string.data_error_message))
                        jobCancel()
                        delay(100)
                        return@collectLatest
                    }

                    if (_dataIDSet.contains(result.id.toInt()).not() && result.state == 2) {
                        _dataIDSet.add(result.id.toInt())
                        _isResultProgressBar.emit(ResultData(dataId = result.id.toInt(), state = result.state, isShow = false))
                    } else {
                        ResultData(dataId = -1, state = result.state, isShow = result.state == 1)
                        _isResultProgressBar.emit(ResultData(dataId = -1, state = result.state, isShow = result.state == 1))
                    }
                    if (result.state == 2) {
                        jobCancel()
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        if (result.state == 1) {
                            sleepDataResult()
                        }
                    }
                } ?: _isResultProgressBar.emit(ResultData(dataId = -1, state = 1, isShow = true))
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
                    if (_dataIDSet.contains(result.id.toInt()).not() && result.state == 3) {
                        _dataIDSet.add(result.id.toInt())
                        _isResultProgressBar.emit(ResultData(dataId = -1, state = result.state, isShow = false))
                        sendErrorMessage(ApplicationManager.instance.getString(R.string.data_error_message))
                        jobCancel()
                        delay(100)
                        return@collectLatest
                    }
                    if (_dataIDSet.contains(result.id.toInt()).not() && result.state == 2) {
                        _dataIDSet.add(result.id.toInt())
                        _isResultProgressBar.emit(ResultData(dataId = result.id.toInt(), state = result.state, isShow = false))
                    } else {
                        _isResultProgressBar.emit(ResultData(dataId = -1, state = result.state, isShow = result.state == 1))
                    }
                    if (result.state == 2) {
                        jobCancel()
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        delay(4000)
                        if (result.state == 1) {
                            noSeringResult()
                        }
                    }
                } ?: _isResultProgressBar.emit(ResultData(dataId = -1, state = 1, isShow = true))
            }
    }

    override fun whereTag(): String {
        return "Main"
    }

}

enum class ServiceCommend {
    START, STOP, CANCEL
}

data class ResultData(val dataId: Int = -1, val state: Int, val isShow: Boolean = false)