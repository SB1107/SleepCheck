package kr.co.sbsolutions.newsoomirang.presenter.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteDataSource
import kr.co.sbsolutions.withsoom.utils.TokenManager
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val loginRepository: RemoteDataSource
) : ViewModel() {

    fun login(snsType: String, token: String, name: String) {

        viewModelScope.launch {
            launch {
                tokenManager.getFcmToken().collectLatest {
                    it?.let {fcmToken ->
                        loginRepository.postLogin(SnsLoginModel(snsType, token, fcm_key = fcmToken, name = name)).collectLatest {
                            Log.d(TAG, "login: ${it.toString()}")
                        }
                    }
                }
            }
        }
    }
}