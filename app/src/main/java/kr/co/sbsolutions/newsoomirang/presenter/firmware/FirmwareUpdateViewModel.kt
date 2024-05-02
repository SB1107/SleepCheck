package kr.co.sbsolutions.newsoomirang.presenter.firmware

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.bluetooth.FirmwareData
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FirmwareUpdateViewModel
@Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _checkFirmWaveVersion: MutableStateFlow<Pair<Boolean, FirmwareData?>> = MutableStateFlow(Pair(false, null))
    val checkFirmWaveVersion: StateFlow<Pair<Boolean, FirmwareData?>> = _checkFirmWaveVersion


    fun getFirmwareVersion() {
        viewModelScope.launch {
            getService()?.getFirmwareVersion()?.collectLatest {
                if (it == null) {
                    _checkFirmWaveVersion.tryEmit(Pair(true, null))
                    cancel()
                    return@collectLatest
                }
                // FIXME: 서버 통신후 버전 비교 및 zip파일 다운로드 후 진행
                if (hasUpdate(it.firmwareVersion, "1.0.0")) {
                    deviceDisconnectConnect(it)
                    cancel()
                    return@collectLatest
                }
                _checkFirmWaveVersion.tryEmit(Pair(false, it))
                cancel()
                return@collectLatest
            }
        }
    }

    private fun deviceDisconnectConnect(firmwareData: FirmwareData) {
        viewModelScope.launch {
            getService()?.disconnectDevice()
            getService()?.getSbSensorInfo()?.collectLatest {
                if (it.bluetoothState == BluetoothState.DisconnectedByUser) {
                    _checkFirmWaveVersion.tryEmit(Pair(true, firmwareData))
                    cancel()
                    delay(100)
                    return@collectLatest
                }
            }
        }

    }

    fun deviceConnect() = callbackFlow {
        viewModelScope.launch {
            getService()?.connectDevice()
            getService()?.getSbSensorInfo()?.collectLatest {
                if (it.bluetoothState == BluetoothState.Connected.Ready) {
                    trySend(true)
                    close()
                    cancel()
                    delay(100)
                    return@collectLatest
                }
            }
        }
        awaitClose()
    }

    fun sendFirmwareUpdate() {

    }

    private fun hasUpdate(currentVer: String, compareVer: String): Boolean {
        val curVer = currentVer.split(".").map { it.toInt() }
        val comVer = compareVer.split(".").map { it.toInt() }

        // 메이저 버전 비교
        if (comVer[0] > curVer[0]) {
            return true
        }
        // 마이너 버전 비교
        else if (comVer[1] > curVer[1]) {
            return true
        }
        // 패치 버전 비교
        else if (comVer[2] > curVer[2]) {
            return true
        }
        // 버전이 같음
        return false
    }

    override fun whereTag(): String {
        return  "Firmware"
    }


}
    