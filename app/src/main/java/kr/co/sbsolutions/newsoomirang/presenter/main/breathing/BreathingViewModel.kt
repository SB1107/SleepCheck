package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import javax.inject.Inject

@HiltViewModel
class BreathingViewModel @Inject constructor(
    private val dataManager: DataManager,
) : BaseServiceViewModel() {
    private val _userName: MutableSharedFlow<String> = MutableSharedFlow()
    val userName: SharedFlow<String> = _userName

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getUserName().first()?.let {
                _userName.emit(it)
            }
        }
    }
    fun start(){
//        bleRepository.getBleService().
//        service.sbBreathingInfo.
    }

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
    }
}