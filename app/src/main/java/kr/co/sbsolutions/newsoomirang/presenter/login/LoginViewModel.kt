package kr.co.sbsolutions.newsoomirang.presenter.login

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteLoginDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.presenter.splash.WHERE
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val loginRepository: RemoteLoginDataSource
) : BaseViewModel(dataManager, tokenManager) {
    private val _whereActivity: MutableSharedFlow<WHERE> = MutableStateFlow(WHERE.None)
    val whereActivity: SharedFlow<WHERE> = _whereActivity
    lateinit var accessToken: String

    fun login(snsType: String, token: String, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                tokenManager.getFcmToken().first()?.let { fcmToken ->
                    request { loginRepository.postLogin(SnsLoginModel(snsType, token, fcm_key = fcmToken, name = name)) }.collectLatest { user ->
                        user.result?.let { result ->
                            val isMember = result.member == "Y"
                            dataManager.saveSNSType(snsType)
                            dataManager.saveUserName(name)
                            _whereActivity.emit(whereActivity(isMember, result.access_token ?: ""))
                        }
                    }
                }
            }
        }
    }

    private fun whereActivity(isMember: Boolean, accessToken: String): WHERE {
        if (isMember) {
            viewModelScope.launch(Dispatchers.IO) {
                tokenManager.saveToken(accessToken)
            }
        }
        this.accessToken = accessToken
        return if (isMember) WHERE.Main else WHERE.Policy
    }
}