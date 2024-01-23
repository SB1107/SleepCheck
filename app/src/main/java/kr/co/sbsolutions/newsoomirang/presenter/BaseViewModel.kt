package kr.co.sbsolutions.newsoomirang.presenter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.utils.TokenManager


open class BaseViewModel(private  val dataManager: DataManager,private  val tokenManager: TokenManager) : ViewModel() {
    var mJob: Job? = null
    private val _errorMessage: MutableSharedFlow<String> = MutableSharedFlow()
    val errorMessage: SharedFlow<String> = _errorMessage

    private val _isProgressBar: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val isProgressBar: SharedFlow<Boolean> = _isProgressBar

    private  lateinit var reAuthorizeCallBack : BaseActivity.ReAuthorizeCallBack

    fun cancelJob() {
        mJob?.let {
            if (it.isActive) {
                it.cancel()
            }
        }
    }
fun setReAuthorizeCallBack(reAuthorizeCallBack : BaseActivity.ReAuthorizeCallBack){
    this.reAuthorizeCallBack = reAuthorizeCallBack
}
    override fun onCleared() {
        super.onCleared()
        cancelJob()
    }

    protected suspend fun <T> request(request: () -> Flow<ApiResponse<T>>) = callbackFlow {
        mJob = viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            viewModelScope.launch(Dispatchers.Main) {
                _errorMessage.emit(error.localizedMessage ?: "Error occured! Please try again.")
            }
        }) {
            request().collect {
                when (it) {
                    is ApiResponse.Failure -> {
                        _isProgressBar.emit(false)
                        _errorMessage.emit(it.errorCode.msg)

                    }
                    ApiResponse.Loading -> {
                        _isProgressBar.emit(true)
                    }

                    ApiResponse.ReAuthorize -> {
                        _isProgressBar.emit(false)
                        if (::reAuthorizeCallBack.isInitialized) {
                            reAuthorizeCallBack.reLogin()
                            viewModelScope.launch(Dispatchers.IO) {
                                tokenManager.deleteToken()
                                dataManager.deleteUserName()
                                dataManager.deleteBluetoothDevice(SBBluetoothDevice.SB_SOOM_SENSOR.type.name)
                            }

                        }
                    }

                    is ApiResponse.Success -> {
                        _isProgressBar.emit(false)
                        trySend(it.data)
                        cancel()
                    }
                }
            }
        }
        awaitClose()
    }

    interface CoroutinesErrorHandler {
        fun onError(message: String)
    }
}