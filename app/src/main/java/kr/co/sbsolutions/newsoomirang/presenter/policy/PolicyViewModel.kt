package kr.co.sbsolutions.newsoomirang.presenter.policy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.booleanToInt
import javax.inject.Inject

@HiltViewModel
class PolicyViewModel @Inject constructor(private  val dataManager: DataManager) : ViewModel() {
    private  val _userName : MutableSharedFlow<String> = MutableSharedFlow()
    val userName : SharedFlow<String> = _userName
    private  val _checkServerDataFlow : MutableStateFlow<Int> = MutableStateFlow(0)
    val checkServerDataFlow : StateFlow<Int> = _checkServerDataFlow
    private  val _checkAppDataFlow : MutableStateFlow<Int> = MutableStateFlow(0)
    val checkAppDataFlow : StateFlow<Int> = _checkAppDataFlow



    init {
        viewModelScope.launch {
            dataManager.getUserName().first()?.let {
                _userName.emit(it)
            }
        }
    }
    fun setCheckServerData(isChecked : Boolean){
        _checkServerDataFlow.tryEmit(isChecked.booleanToInt())
    }
    fun setCheckAppData(isChecked : Boolean){
        _checkAppDataFlow.tryEmit(isChecked.booleanToInt())
    }
}