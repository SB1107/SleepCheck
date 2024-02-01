package kr.co.sbsolutions.newsoomirang.presenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.RequestHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse


open class BaseViewModel(private val dataManager: DataManager, private val tokenManager: TokenManager) : ViewModel() {
    private val _errorMessage: MutableSharedFlow<String> = MutableSharedFlow()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _isProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isProgressBar: SharedFlow<Boolean> = _isProgressBar

    private lateinit var reAuthorizeCallBack: BaseActivity.ReAuthorizeCallBack
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

    fun setReAuthorizeCallBack(reAuthorizeCallBack: BaseActivity.ReAuthorizeCallBack) {
        this.reAuthorizeCallBack = reAuthorizeCallBack
        requestHelper.setReAuthorizeCallBack(this.reAuthorizeCallBack)
    }

    override fun onCleared() {
        super.onCleared()
        cancelJob()
    }

    protected suspend fun <T: BaseEntity> request(request: () -> Flow<ApiResponse<T>>): Flow<T> {
        return requestHelper.request(request)
    }
}
