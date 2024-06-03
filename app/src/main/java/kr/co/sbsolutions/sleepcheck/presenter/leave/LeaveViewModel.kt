package kr.co.sbsolutions.sleepcheck.presenter.leave

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.DataRemove
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class LeaveViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val authDataSource: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager), DataRemove {
    private val _logoutResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val logoutResult: SharedFlow<Boolean> = _logoutResult

    fun leaveButtonClick(reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.postLeave(reason) }.collectLatest {
                if (it.success) {
                    dataRemove()
                }
            }
        }
    }

    override fun dataRemove() {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.deleteToken()
            dataManager.deleteUserName()
            Firebase.auth.signOut()
            deleteDeviceName()
            _logoutResult.emit(true)
        }
    }

    override fun deleteDeviceName() {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.deleteBluetoothDevice(bluetoothInfo.sbBluetoothDevice.type.name)
        }
    }

    override fun whereTag(): String {
        return "leave"
    }
}