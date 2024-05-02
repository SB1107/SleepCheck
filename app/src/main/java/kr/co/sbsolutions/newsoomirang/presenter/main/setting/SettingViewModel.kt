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
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.DataRemove
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.hasUpdate
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
        var deviceFirmVer: String? = null
        viewModelScope.launch {
            ApplicationManager.getService().value.get()?.getFirmwareVersion()?.collectLatest {
                Log.e(TAG, "getFirmwareVersion: ${it}", )
                deviceFirmVer = it.toString()
                if (it == null){
                    _updateCheckResult.emit(true)
                    return@collectLatest
                }
            }
            
            request { remoteAuthDataSource.getNewFirmVersion() }.collectLatest {
                Log.d(TAG, "getFirmwareVersion: $it")
                if (it.success) {
                    it.result?.newFirmVer?.let { newFirmVer ->
                        deviceFirmVer?.let { deviceFirmVer ->
                            _updateCheckResult.emit(hasUpdate(currentVer = deviceFirmVer, compareVer = newFirmVer))
                            return@collectLatest
                        }
                    }
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
    
    
    private fun isVersionCheck(version1: String? = null, version2: String): Boolean {
        val v1Components = version1?.split(".")
        val v2Components = version2.split(".")
        
        if (version1.isNullOrEmpty()){
            return true
        }
        
        if (v1Components != null) {
            for (i in 0 until maxOf(v1Components.size, v2Components.size)) {
                val v1Component = v1Components.getOrElse(i) { "0" }.toInt()
                val v2Component = v2Components.getOrElse(i) { "0" }.toInt()
                
                if (v1Component < v2Component) {
                    return true
                } else if (v1Component > v2Component) {
                    return false
                }
            }
        }
        return false
    }
}