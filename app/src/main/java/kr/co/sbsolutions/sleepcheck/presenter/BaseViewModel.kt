package kr.co.sbsolutions.sleepcheck.presenter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.RequestHelper
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.entity.BaseEntity
import kr.co.sbsolutions.sleepcheck.data.server.ApiResponse
import kr.co.sbsolutions.sleepcheck.service.ILogHelper


open class BaseViewModel(dataManager: DataManager, tokenManager: TokenManager) : ViewModel() {
    private val _errorMessage: MutableSharedFlow<String> = MutableSharedFlow()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _isProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isProgressBar: SharedFlow<Boolean> = _isProgressBar

    private lateinit var reAuthorizeCallBack: RequestHelper.ReAuthorizeCallBack
    private lateinit var logHelper: ILogHelper

    private val requestHelper: RequestHelper = RequestHelper(
        scope = viewModelScope,
        tokenManager = tokenManager, dataManager = dataManager, errorMessage = _errorMessage,
        _isProgressBar
    )

    fun cancelJob() {
        requestHelper.netWorkCancel()
    }

    fun sendErrorMessage(message: String) {
        viewModelScope.launch {
            _errorMessage.emit(message)
        }
    }

    fun showProgressBar() {
        viewModelScope.launch {
            _isProgressBar.emit(true)
        }
    }
    fun  dismissProgressBar() {
        viewModelScope.launch {
            _isProgressBar.emit(false)
        }
    }

    fun setReAuthorizeCallBack(reAuthorizeCallBack: RequestHelper.ReAuthorizeCallBack) {
        this.reAuthorizeCallBack = reAuthorizeCallBack
        requestHelper.setReAuthorizeCallBack(this.reAuthorizeCallBack)
    }

    fun reLoginCallBack() {
        reAuthorizeCallBack.reLogin()
    }

    fun insertLog(log: String) {
        if (::logHelper.isInitialized) {
            logHelper.insertLog(log)
            Log.d(TAG, "insertLog: $log")
        }
    }

    fun setLogHelper(logHelper: ILogHelper) {
        this.logHelper = logHelper
    }

    fun insertLog(method: () -> Unit) {
        if (::logHelper.isInitialized) {
            logHelper.insertLog(method)
        }
    }

    fun registerJob(job: Job, method: () -> Unit) {
        if (::logHelper.isInitialized) {
            return logHelper.registerJob(job, method)
        }
    }

    fun registerJob(tag : String ,job: Job ) {
        if (::logHelper.isInitialized) {
            return logHelper.registerJob(tag, job)
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelJob()
    }

    protected suspend fun <T : BaseEntity> request(showProgressBar: Boolean = true, request: () -> Flow<ApiResponse<T>>): Flow<T> {
        return requestHelper.request(request, showProgressBar = showProgressBar)
    }
}
