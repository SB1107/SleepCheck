package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
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
import kotlinx.coroutines.yield
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice

class RequestHelper(
    private val scope: CoroutineScope,
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val errorMessage: MutableSharedFlow<String>? = null,
    private val isProgressBar: MutableSharedFlow<Boolean>? = null,
) {
    companion object {
        var reAuthorizeCallBack: ReAuthorizeCallBack? = null
        var logWorkerHelper: LogWorkerHelper? = null
    }

    private val requestMap: MutableMap<String, Job> = mutableMapOf()

    fun setReAuthorizeCallBack(reAuthorizeCallBack: ReAuthorizeCallBack) {
        RequestHelper.reAuthorizeCallBack = reAuthorizeCallBack
    }

    fun setLogWorkerHelper(logWorkerHelper: LogWorkerHelper) {
        RequestHelper.logWorkerHelper = logWorkerHelper
    }

    suspend fun <T : BaseEntity> request(request: () -> Flow<ApiResponse<T>>, errorHandler: CoroutinesErrorHandler? = null, showProgressBar: Boolean = true) = callbackFlow {
        val name = getClazzName(request)
        Log.e(TAG, "request: $name")
        if (!ApplicationManager.getNetworkCheck()) {
            scope.launch {
                val errorMSG = "네트워크 연결이 되어 있지 않습니다. \n확인후 다시 실행해주세요"
                errorMessage?.emit(errorMSG)
                logWorkerHelper?.insertLog("$name = 네트워크 연결 오류")
                errorHandler?.onError("네트워크 연결 오류")
                cancel("네트워크 오류")
            }
        } else {
            if (requestMap.containsKey(request.toString())) {
//                Log.e("Aaa","중복호출")
                requestMap[request.toString()]?.cancel("중복호출")
            }

            val job = scope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
                scope.launch(Dispatchers.Main) {
                    val errorMSG = error.localizedMessage ?: "Error occured! Please try again."
                    errorMessage?.emit(errorMSG)
                    errorHandler?.onError(errorMSG)
                    logWorkerHelper?.insertLog(errorMSG)
                }
            }) {
                yield()
                request().collect {
                    when (it) {
                        is ApiResponse.Failure -> {
                            if (showProgressBar) {
                                isProgressBar?.emit(false)
                            }
                            errorMessage?.emit(it.errorCode.msg)
                            errorHandler?.onError(it.errorCode.msg)
                        }

                        ApiResponse.Loading -> {
                            if (showProgressBar) {
                                isProgressBar?.emit(true)
                            }
                        }

                        ApiResponse.ReAuthorize -> {
                            if (showProgressBar) {
                                isProgressBar?.emit(false)
                            }
                            reAuthorizeCallBack?.reLogin()
                            scope.launch(Dispatchers.IO) {
                                tokenManager.deleteToken()
                                dataManager.deleteUserName()
                                dataManager.deleteBluetoothDevice(SBBluetoothDevice.SB_SOOM_SENSOR.type.name)
                            }
                        }

                        is ApiResponse.Success -> {
                            if (showProgressBar) {
                                isProgressBar?.emit(false)
                            }
                            trySend(it.data)
                            cancel()
                        }
                    }
                }
            }
            requestMap[request.toString()] = job
            job.invokeOnCompletion {
                if (it?.localizedMessage?.isEmpty() == true) {
                    requestMap.remove(request.toString())
                }
            }
        }
        awaitClose()
    }

    private fun <T : BaseEntity> getClazzName(request: () -> Flow<ApiResponse<T>>): String {
        val regex = Regex("[\\d\\p{Punct}$]")
        val clazzName = regex.replace(request.javaClass.name.split(".").last(), "")

         return if (clazzName.contains("ViewModel")) {
            val index = clazzName.indexOf("ViewModel")
            clazzName.substring(index + "ViewModel".length)
        } else {
            clazzName
        }
    }

    fun netWorkCancel() {
        requestMap.forEach { (_, job) ->
            job.cancel()
        }
        requestMap.clear()
    }

    fun interface CoroutinesErrorHandler {
        fun onError(message: String)
    }

    interface ReAuthorizeCallBack {
        fun reLogin()
    }
}
