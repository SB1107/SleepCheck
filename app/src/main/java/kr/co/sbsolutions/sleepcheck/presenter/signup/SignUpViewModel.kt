package kr.co.sbsolutions.sleepcheck.presenter.signup

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.entity.RentalCompanyItemData
import kr.co.sbsolutions.sleepcheck.data.entity.UpdateUserResultData
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
    private val _companyList: MutableStateFlow<List<RentalCompanyItemData>> = MutableStateFlow(emptyList())
    val companyList: StateFlow<List<RentalCompanyItemData>> = _companyList.asStateFlow()

    private val _signUpResult: MutableSharedFlow<UpdateUserResultData> = MutableSharedFlow()
    val signUpResult: SharedFlow<UpdateUserResultData> = _signUpResult.asSharedFlow()

    fun getCompanyList() {
        viewModelScope.launch(Dispatchers.IO) {
            request {
                authRepository.getRentalCompany()
            }.collectLatest {
                it.result?.let { items ->
                    _companyList.emit(items.data)
                }
            }
        }
    }

    fun signUp(accessToken: String?,company: String, name: String, birthday: String) {
        runBlocking {
            accessToken?.let {
                tokenManager.saveToken(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            request {
                authRepository.postSignUp(company, name, birthday)
            }.collectLatest { entity ->
                entity.result?.let {
                    _signUpResult.emit(it)
                    viewModelScope.launch(Dispatchers.IO) {
                        dataManager.saveUserName(name)
                    }
                }
            }
        }
    }
}