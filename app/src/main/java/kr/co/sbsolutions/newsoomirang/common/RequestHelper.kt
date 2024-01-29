package kr.co.sbsolutions.newsoomirang.common

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity

class RequestHelper(
    private val scope: CoroutineScope,
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val errorMessage: MutableSharedFlow<String>? = null,
    private val isProgressBar: MutableSharedFlow<Boolean>? = null,
) {
    private var mJob: Job? = null
    private var reAuthorizeCallBack: BaseActivity.ReAuthorizeCallBack? = null

    fun setReAuthorizeCallBack(reAuthorizeCallBack: BaseActivity.ReAuthorizeCallBack) {
        this.reAuthorizeCallBack = reAuthorizeCallBack
    }

    suspend fun <T> request(request: () -> Flow<ApiResponse<T>>,  errorHandler: CoroutinesErrorHandler? = null) = callbackFlow {
        if (!ApplicationManager.getNetworkCheck()) {
            scope.launch {
                errorMessage?.emit("네트워크 연결이 되어 있지 않습니다. \n확인후 다시 실행해주세요")
                cancel("네트워크 오류")
            }
        } else {
            mJob = scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
                scope.launch(Dispatchers.Main) {
                    if (error.message != "Unable to create instance of class okhttp3.RequestBody. Registering an InstanceCreator or a TypeAdapter for this type, or adding a no-args constructor may fix this problem.") {
                        errorMessage?.emit(error.localizedMessage ?: "Error occured! Please try again.")
                        errorHandler?.onError(error.localizedMessage ?: "Error occured! Please try again.")
                    }
                }
            }) {
                request().collect {
                    when (it) {
                        is ApiResponse.Failure -> {
                            isProgressBar?.emit(false)
                            errorMessage?.emit(it.errorCode.msg)
                        }
                        ApiResponse.Loading -> {
                            isProgressBar?.emit(true)
                        }
                        ApiResponse.ReAuthorize -> {
                            isProgressBar?.emit(false)
                            reAuthorizeCallBack?.reLogin()
                            scope.launch(Dispatchers.IO) {
                                tokenManager.deleteToken()
                                dataManager.deleteUserName()
                                dataManager.deleteBluetoothDevice(SBBluetoothDevice.SB_SOOM_SENSOR.type.name)
                            }
                        }

                        is ApiResponse.Success -> {
                            isProgressBar?.emit(false)
                            trySend(it.data)
                            cancel()
                        }
                    }
                }
            }
        }
        awaitClose()
    }

    fun getNetWorkJob(): Job? {
        return mJob
    }

    fun interface CoroutinesErrorHandler {
        fun onError(message: String)
    }
}