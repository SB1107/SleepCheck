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
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val dataManager: DataManager,
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val bluetoothManagerUseCase: BluetoothManageUseCase,
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
            getService()?.getSbSensorInfo()?.value?.bluetoothState?.let { infoState ->
                if (infoState == BluetoothState.Connected.Ready ||
                    infoState == BluetoothState.Connected.Init ||
                    infoState == BluetoothState.Connected.End
                )
                    ApplicationManager.getService().value.get()?.getFirmwareVersion()?.collectLatest { deviceInfo ->
                        Log.e(TAG, "getFirmwareVersion11: ${deviceInfo?.firmwareVersion}")
                        if (deviceInfo?.firmwareVersion.isNullOrEmpty()) {
                            _updateCheckResult.emit(true)
                            cancel()
                            delay(100)
                            return@collectLatest
                        }
                        deviceInfo?.let { getNewFirmVersion(it) }
                    }
            }
        }
    }

    private fun getNewFirmVersion(deviceInfo: FirmwareData?) {
        viewModelScope.launch {
            deviceInfo?.let { info ->
                request { remoteAuthDataSource.getNewFirmVersion(info.deviceName, ApplicationManager.instance.baseContext.getLanguage()) }.collectLatest { result ->
                    Log.d(TAG, "getFirmwareVersion: $result")
                    result.result?.newFirmVer?.let { newFirmVer ->
                        val currentVersion = if (result.result.sensorVer.isNullOrEmpty()) {
                            deviceInfo.firmwareVersion
                        } else {
                            result.result.sensorVer
                        }
                        _updateCheckResult.emit(hasUpdate(currentVer = currentVersion, compareVer = newFirmVer))
                        Log.d(TAG, "getFirmwareVersion000: ${hasUpdate(currentVer = currentVersion, compareVer = newFirmVer)}")
                        cancel()
                        delay(100)
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


    private fun isVersionCheck(version1: String? = null, version2: String): Boolean {
        val v1Components = version1?.split(".")
        val v2Components = version2.split(".")

        if (version1.isNullOrEmpty()) {
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