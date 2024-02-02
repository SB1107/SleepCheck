package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.bluetooth.BluetoothAdapter
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.usecase.BluetoothManageUseCase
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val remoteAuthDataSource: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _logoutResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val logoutResult: SharedFlow<Boolean> = _logoutResult


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
                            launch {
                                _logoutResult.emit(true)
                                tokenManager.deleteToken()
                                dataManager.deleteUserName()
                                Firebase.auth.signOut()
                                deleteDeviceName()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun deleteDeviceName() {
        viewModelScope.launch(Dispatchers.IO) {
            dataManager.deleteBluetoothDevice(bluetoothInfo.sbBluetoothDevice.type.name)
        }

    }

    override fun whereTag(): String {
        return "Setting"
    }


}