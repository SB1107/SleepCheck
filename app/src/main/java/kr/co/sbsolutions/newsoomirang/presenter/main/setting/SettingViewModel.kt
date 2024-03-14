package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.bluetooth.BluetoothAdapter
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
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.DataRemove
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.usecase.BluetoothManageUseCase
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val remoteAuthDataSource: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) , DataRemove {

    private val _logoutResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val logoutResult: SharedFlow<Boolean> = _logoutResult

    private val _deviceName: MutableStateFlow<String?> = MutableStateFlow("")
    val deviceName: SharedFlow<String?> = _deviceName.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                ApplicationManager.getBluetoothInfoFlow().collectLatest { info ->
                    Log.d(TAG, "[STM]: $info")
                    getDeviceName()
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

    fun getDeviceName(){
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.getBluetoothDeviceName(bluetoothInfo.sbBluetoothDevice.type.name).first()?.let {
                Log.d(TAG, "getDeviceName: $it")
                _deviceName.emit(it)
            } ?: _deviceName.emit("")
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