package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.DataRemove
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.usecase.BluetoothManageUseCase
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
) : BaseServiceViewModel(dataManager, tokenManager) , DataRemove {

    private val _logoutResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val logoutResult: SharedFlow<Boolean> = _logoutResult

    private val _deviceName: MutableStateFlow<String?> = MutableStateFlow("")
    val deviceName: StateFlow<String?> = _deviceName

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                bluetoothManagerUseCase.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR).collectLatest {
                    it?.let {
                        Log.d(TAG, "getDeviceName:11r $it")
                        _deviceName.emit(it)
                    } ?: _deviceName.emit("")
                }
            }
        }
    }
    //로그아웃
    fun logout() {
        if (bluetoothInfo.bluetoothState == BluetoothState.Connected.ReceivingRealtime ||
            bluetoothInfo.bluetoothState == BluetoothState.Connected.SendDownloadContinue
        ) {
            sendErrorMessage("호흡 측정중 입니다.\n종료후 로그아웃을 시도해주세요")
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                launch {
                    request { remoteAuthDataSource.postLogout() }.collectLatest {
                        Log.d(TAG, "logout 결과: ${it.success}")
                        if (it.success) {
                            dataRemove()
                        }
                    }
                }
            }
        }
    }

    override fun dataRemove() {
        viewModelScope.launch {
            tokenManager.deleteToken()
            dataManager.deleteUserName()
            Firebase.auth.signOut()
            FirebaseAuth.getInstance().signOut();
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
        return "Setting"
    }


}