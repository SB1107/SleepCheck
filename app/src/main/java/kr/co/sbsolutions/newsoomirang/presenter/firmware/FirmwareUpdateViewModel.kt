package kr.co.sbsolutions.newsoomirang.presenter.firmware

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.hasUpdate
import kr.co.sbsolutions.newsoomirang.data.bluetooth.FirmwareData
import kr.co.sbsolutions.newsoomirang.data.bluetooth.FirmwareDataModel
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.repository.DownloadAPIRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import no.nordicsemi.android.dfu.DfuServiceInitiator
import java.io.File
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class FirmwareUpdateViewModel
@Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    private val downloadAPIRepository: DownloadAPIRepository,
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _checkFirmWaveVersion: MutableStateFlow<FirmwareDataModel> = MutableStateFlow(FirmwareDataModel())
    val checkFirmWaveVersion: StateFlow<FirmwareDataModel> = _checkFirmWaveVersion
    private var firmwareDataValue: FirmwareData? = null
    private var serverFirmwareVersion: String? = null
    private val _nextAPICall: MutableSharedFlow<Pair<String, FirmwareData>> = MutableSharedFlow()

    private fun downloadFirmware(url: String, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val urls = url.replace("http://sb-solutions1.net/", "").split("/")
            val urlPath = urls.dropLast(1).joinToString("/")
            val fileName = urls.last()
            showProgressBar()
            downloadAPIRepository.getDownloadZipFile(urlPath, fileName)
                .collect {
                    val tempFile = File(path, fileName)
                    it.byteStream().use { inputStream ->
                        tempFile.outputStream().use { outStream ->
                            inputStream.copyTo(outStream)
                        }
                    }
                    firmwareDataValue?.let { data ->
                        val starter = DfuServiceInitiator(data.deviceAddress)
                            .setDeviceName(data.deviceName)
                            .setKeepBond(true)
                        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                        starter.setPrepareDataObjectDelay(300L)
                        starter.setZip(Uri.fromFile(tempFile))
                        _checkFirmWaveVersion.tryEmit(
                            FirmwareDataModel(
                                isShow = true,
                                dfuServiceInitiator = starter,
                                firmwareVersion = data.firmwareVersion,
                                deviceName = data.deviceName,
                                deviceAddress = data.deviceAddress,
                                serverFirmwareVersion = serverFirmwareVersion ?: "1.0.0"
                            )
                        )
                    }
                }
        }
    }

    private fun getAPICall(path: String, firmwareData: FirmwareData) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.getNewFirmVersion(firmwareData.deviceName) }
                .collectLatest { firmware ->
                    firmware.result?.let { result ->
                        serverFirmwareVersion = result.newFirmVer ?: "1.0.0"
                        if (hasUpdate(firmwareData.firmwareVersion, serverFirmwareVersion!!)) {
                            result.url?.let { url ->
                                firmwareDataValue = firmwareData
                                deviceDisconnectConnect(url, path)
                            }
                        } else {
                            _checkFirmWaveVersion.tryEmit(FirmwareDataModel())
                        }
                    } ?: run {
                        _checkFirmWaveVersion.tryEmit(FirmwareDataModel())
                        cancel()
                        delay(100)
                    }
                }
        }


    }

    fun getFirmwareVersion(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            getService()?.getFirmwareVersion()?.collectLatest { firmwareData ->
                if (firmwareData == null) {
                    _checkFirmWaveVersion.tryEmit(FirmwareDataModel())
                    return@collectLatest
                }
                _nextAPICall.emit(Pair(path, firmwareData))
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _nextAPICall.collectLatest {
                val (path, data) = it
                getAPICall(path, data)
                cancel()
                delay(100)
                return@collectLatest
            }
        }
    }

    private fun deviceDisconnectConnect(url: String, path: String) {
        viewModelScope.launch {
            getService()?.disconnectDevice()
            getService()?.getSbSensorInfo()?.collectLatest {
                if (it.bluetoothState == BluetoothState.DisconnectedByUser) {
                    downloadFirmware(url, path)

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

    override fun whereTag(): String {
        return "Firmware"
    }


}
    