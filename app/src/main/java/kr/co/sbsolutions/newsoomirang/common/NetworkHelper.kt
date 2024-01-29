package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.repository.AuthAPIRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import javax.inject.Inject


class NetworkHelper @Inject constructor (tokenManager: TokenManager, dataManager: DataManager, authAPIRepository: RemoteAuthDataSource) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenManager.getFcmToken().collect {
                Log.d(TAG, "토큰 생성 발생!!: ")
                CoroutineScope(Dispatchers.IO).launch {
                    RequestHelper(this, tokenManager = tokenManager, dataManager = dataManager)
                        .request({
                            authAPIRepository.postNewFcmToken(it!!)
                        }).collectLatest {
                            Log.d(TAG, "$it")
                        }

                }

            }
        }
    }



}