package kr.co.sbsolutions.sleepcheck.presenter.signup

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import okhttp3.Dispatcher
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authRepository: RemoteAuthDataSource
) : BaseViewModel(dataManager, tokenManager) {

    private val _signUpResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val policyResult: SharedFlow<Boolean> = _signUpResult

    fun signUp(accessToken: String?, company: String, name: String, birthday: String) {
        runBlocking {
            accessToken?.let {
                tokenManager.saveToken(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
//            request {
//            authRepository.postSignUp()
//            }
        }
    }
}