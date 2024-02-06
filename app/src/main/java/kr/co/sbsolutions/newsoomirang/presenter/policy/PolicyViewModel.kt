package kr.co.sbsolutions.newsoomirang.presenter.policy

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.booleanToInt
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val policyRepository: RemoteAuthDataSource

) : BaseViewModel(dataManager, tokenManager) {
    private val _userName: MutableSharedFlow<String> = MutableSharedFlow()
    val userName: SharedFlow<String> = _userName
    private val _checkServerDataFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val checkServerDataFlow: StateFlow<Int> = _checkServerDataFlow
    private val _checkAppDataFlow: MutableStateFlow<Int> = MutableStateFlow(0)
    val checkAppDataFlow: StateFlow<Int> = _checkAppDataFlow

    private val _policyResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val policyResult: SharedFlow<Boolean> = _policyResult

    init {
        viewModelScope.launch {
            launch {
                dataManager.getUserName().first()?.let {
                    _userName.emit(it)
                }
            }
        }
    }

    fun setCheckServerData(isChecked: Boolean) {
        _checkServerDataFlow.tryEmit(isChecked.booleanToInt())
    }

    fun setCheckAppData(isChecked: Boolean) {
        _checkAppDataFlow.tryEmit(isChecked.booleanToInt())
    }

    fun joinAgree(token: String?) {
        runBlocking {
            token?.let {
                tokenManager.saveToken(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            request { policyRepository.postPolicy(PolicyModel(_checkServerDataFlow.value, _checkAppDataFlow.value)) }.collectLatest {
                _policyResult.emit(true)
            }
        }
    }
}