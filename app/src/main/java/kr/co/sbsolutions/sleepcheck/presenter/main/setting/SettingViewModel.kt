package kr.co.sbsolutions.sleepcheck.presenter.main.setting

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.ApplicationManager
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.DataRemove
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.common.getLanguage
import kr.co.sbsolutions.sleepcheck.common.hasUpdate
import kr.co.sbsolutions.sleepcheck.data.bluetooth.FirmwareData
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.usecase.BluetoothManageUseCase
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceViewModel
import kr.co.sbsolutions.sleepcheck.presenter.firmware.FirmwareHelper
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
    private  val firmwareHelper: FirmwareHelper
) : BaseServiceViewModel(dataManager, tokenManager), DataRemove {

    private val _logoutResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val logoutResult: SharedFlow<Boolean> = _logoutResult

    private val _deviceName: MutableStateFlow<String?> = MutableStateFlow("")
    val deviceName: StateFlow<String?> = _deviceName

    private val _updateCheckResult: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val updateCheckResult: StateFlow<Boolean> = _updateCheckResult

    init {
        registerJob("SettingViewModel",
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
        )

    }

        fun getFirmwareVersion() {
            viewModelScope.launch {
                viewModelScope.launch {
                    firmwareHelper.getFirmwareVersion(
                        viewModelScope, getService()?.getSbSensorInfo()?.value?.bluetoothState,
                        ApplicationManager.getService().value.get()?.getFirmwareVersion()
                    ).collectLatest {
                        _updateCheckResult.emit(it)
                    }
                }
            }
        }

    fun sendRental(isChecked: Boolean){
        insertLog("렌탈 회수 설정  = $isChecked")
        viewModelScope.launch(Dispatchers.IO) {
            request { remoteAuthDataSource.postRentalAlarm(isChecked) }.collectLatest {
                Log.d(TAG, "렌탈 회수 설정 결과: ${it.success}")
            }
        }
    }


    //로그아웃
    fun logout() {
        if (bluetoothInfo.bluetoothState == BluetoothState.Connected.ReceivingRealtime ||
            bluetoothInfo.bluetoothState == BluetoothState.Connected.SendDownloadContinue
        ) {
            sendErrorMessage(ApplicationManager.instance.baseContext.getString(R.string.breathing_measurable_finish))
        } else {
            registerJob("logout()",
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
            )
        }
    }

    override fun dataRemove() {
        registerJob("dataRemove()",
            viewModelScope.launch {
                tokenManager.deleteToken()
                dataManager.deleteUserName()
                Firebase.auth.signOut()
                FirebaseAuth.getInstance().signOut();
                deleteDeviceName()
                _logoutResult.emit(true)
            }
        )
    }

    override fun deleteDeviceName() {
        registerJob("deleteDeviceName()",
            viewModelScope.launch(Dispatchers.IO) {
                dataManager.deleteBluetoothDevice(bluetoothInfo.sbBluetoothDevice.type.name)
            }
        )
    }

    override fun whereTag(): String {
        return "Setting"
    }

}