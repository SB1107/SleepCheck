package kr.co.sbsolutions.newsoomirang.presenter.login

import androidx.lifecycle.ViewModel
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.repository.LoginRepository
import kr.co.sbsolutions.withsoom.utils.TokenManager
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val loginRepository: LoginRepository
) : ViewModel() {
}