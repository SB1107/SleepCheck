package kr.co.sbsolutions.sleepcheck.common

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.service.ILogHelper
import javax.inject.Inject


class FCMTokenUpdateHelper @Inject constructor (tokenManager: TokenManager, dataManager: DataManager, authAPIRepository: RemoteAuthDataSource, logHelper: ILogHelper) {
    init {
        CoroutineScope(Dispatchers.IO).launch {
            val state = tokenManager.getTokenState().first()
            val token = tokenManager.getFcmToken().first()
            if (state.not() && token?.isNotEmpty() == true) {
                    RequestHelper(this, tokenManager = tokenManager, dataManager = dataManager)
                        .apply { setLogWorkerHelper(logHelper)}
                        .request({
                            authAPIRepository.postNewFcmToken(token!!)
                        }).collectLatest { userEntity ->
                            Log.d(TAG, "$userEntity")
                            tokenManager.setUpdateFcmToken()
                        }
            }
        }
    }



}