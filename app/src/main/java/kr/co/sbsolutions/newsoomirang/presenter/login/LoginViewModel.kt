package kr.co.sbsolutions.newsoomirang.presenter.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteDataSource
import kr.co.sbsolutions.newsoomirang.presenter.splash.WHERE
import kr.co.sbsolutions.withsoom.utils.TokenManager
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val loginRepository: RemoteDataSource
) : ViewModel() {
    private val _whereActivity: MutableSharedFlow<WHERE> = MutableStateFlow(WHERE.None)
    val whereActivity: SharedFlow<WHERE> = _whereActivity
    lateinit var accessToken: String

    fun login(snsType: String, token: String, name: String) {

        viewModelScope.launch {
            tokenManager.getFcmToken().collectLatest {
                it?.let { fcmToken ->
                    loginRepository.postLogin(SnsLoginModel(snsType, token, fcm_key = fcmToken, name = name)).collectLatest {user ->
                        user.result?.let { result ->
                            val isMember = result.member == "Y"
                            dataManager.saveSNSType(snsType)
                            dataManager.saveUserName(name)
                            _whereActivity.tryEmit(whereActivity(isMember, result.access_token ?: ""))
                        }
                    }
                }
            }
        }
    }

    private fun whereActivity(isMember: Boolean, accessToken: String): WHERE {
        if (isMember) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenManager.saveFcmToken(accessToken)
            }
        }
        this.accessToken = accessToken
        return if (isMember) WHERE.Main else WHERE.Policy
    }
}