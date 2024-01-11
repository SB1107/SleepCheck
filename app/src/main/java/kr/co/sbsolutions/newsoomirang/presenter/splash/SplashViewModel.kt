package kr.co.sbsolutions.newsoomirang.presenter.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {
    private val _nextProcess: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val nextProcess: SharedFlow<Boolean> = _nextProcess


init {
    gotoLogin()
}
    fun gotoLogin() {
        viewModelScope.launch {
            delay(2000)
            _nextProcess.emit(true)
        }
    }
}