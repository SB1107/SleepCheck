package kr.co.sbsolutions.newsoomirang.presenter.splash

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.withsoom.utils.TokenManager
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _nextProcess: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val nextProcess: SharedFlow<Boolean> = _nextProcess
    private val _whereActivity: MutableSharedFlow<WHERE> = MutableStateFlow(WHERE.None)
    val whereActivity: SharedFlow<WHERE> = _whereActivity

    init {
        gotoLogin()
    }

    fun gotoLogin() {
        viewModelScope.launch {
            delay(2000)
            _nextProcess.emit(true)
        }
    }

    fun whereLocation() {
        viewModelScope.launch {
            val token = tokenManager.getToken().first()
            _whereActivity.emit(if (token.isNullOrEmpty()) WHERE.Login else WHERE.Main)
        }

    }
}

enum class WHERE {
    None, Login, Main
}