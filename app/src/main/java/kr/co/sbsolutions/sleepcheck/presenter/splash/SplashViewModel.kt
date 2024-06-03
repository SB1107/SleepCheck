package kr.co.sbsolutions.sleepcheck.presenter.splash

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    dataManager: DataManager
) : BaseViewModel(dataManager, tokenManager) {
    private val _nextProcess: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val nextProcess: StateFlow<Boolean> = _nextProcess
    private val _whereActivity: MutableStateFlow<WHERE> = MutableStateFlow(WHERE.None)
    val whereActivity: StateFlow<WHERE> = _whereActivity


    init {
        gotoLogin()
        getFcmToken()
    }
    private fun getFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {

            if (!it.isSuccessful) {
                // 토큰 요청 task가 실패 한 경우 처리
                Log.d(Cons.TAG, "initFirebase: failed", it.exception)
                return@addOnCompleteListener
            }
            // 토큰 요청 task가 성공한 경우 task의 result에 token 값이 내려온다.
            val token = it.result
            viewModelScope.launch {
                tokenManager.saveFcmToken(token)
            }

            Log.d(Cons.TAG, "initFirebase: $token")
        }
    }

    private fun gotoLogin() {
        viewModelScope.launch {
            delay(2000)
            _nextProcess.emit(true)
        }
    }

    fun whereLocation() {
        registerJob("whereLocation()",
            viewModelScope.launch {
                val token = tokenManager.getToken().first()
                _whereActivity.emit(if (token.isNullOrEmpty()) WHERE.Login else WHERE.Main)
            }
        )
    }
}

enum class WHERE {
    None, Login, Main ,Policy
}