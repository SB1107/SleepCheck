package kr.co.sbsolutions.sleepcheck.presenter.firmware

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.ApplicationManager
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.common.getLanguage
import kr.co.sbsolutions.sleepcheck.common.hasUpdate
import kr.co.sbsolutions.sleepcheck.data.bluetooth.FirmwareData
import kr.co.sbsolutions.sleepcheck.data.bluetooth.FirmwareDataModel
import kr.co.sbsolutions.sleepcheck.data.bluetooth.FirmwareUpdateModel
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.model.SensorFirmVersion
import kr.co.sbsolutions.sleepcheck.domain.repository.DownloadAPIRepository
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceViewModel
import no.nordicsemi.android.dfu.DfuServiceInitiator
import java.io.File
import javax.inject.Inject


@HiltViewModel
class FirmwareUpdateViewModel
@Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    private val downloadAPIRepository: DownloadAPIRepository,
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _checkFirmwareVersion: MutableStateFlow<FirmwareDataModel> = MutableStateFlow(FirmwareDataModel())
    val checkFirmWaveVersion: StateFlow<FirmwareDataModel> = _checkFirmwareVersion
    private val _firmwareUpdate: MutableStateFlow<FirmwareUpdateModel?> = MutableStateFlow(null)
    val firmwareUpdate: StateFlow<FirmwareUpdateModel?> = _firmwareUpdate
    private var serverFirmwareVersion: String? = null
    private val _nextAPICall: MutableSharedFlow<Pair<String, FirmwareData>> = MutableSharedFlow()

    private fun downloadFirmware(model: FirmwareUpdateModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val urls = model.url.replace("http://sb-solutions1.net/", "").split("/")
            val urlPath = urls.dropLast(1).joinToString("/")
            val fileName = urls.last()
            showProgressBar()
            downloadAPIRepository.getDownloadZipFile(urlPath, fileName)
                .collect {
                    val tempFile = File(model.filePath, fileName)
                    it.byteStream().use { inputStream ->
                        tempFile.outputStream().use { outStream ->
                            inputStream.copyTo(outStream)
                        }
                    }
                    val starter = DfuServiceInitiator(model.deviceAddress)
                        .setDeviceName(model.deviceName)
                        .setKeepBond(true)
                    starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
                    starter.setPrepareDataObjectDelay(300L)
                    starter.setZip(Uri.fromFile(tempFile))
                    _checkFirmwareVersion.tryEmit(
                        FirmwareDataModel(
                            isShow = true,
                            dfuServiceInitiator = starter,
                            firmwareVersion = model.firmwareVersion,
                            deviceName = model.deviceName,
                            deviceAddress = model.deviceAddress,
                            serverFirmwareVersion = serverFirmwareVersion ?: "1.0.0"
                        )
                    )
                }
        }
    }

    private fun getAPICall(path: String, firmwareData: FirmwareData) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.getNewFirmVersion(firmwareData.deviceName, ApplicationManager.instance.baseContext.getLanguage()) }
                .collectLatest { firmware ->
                    firmware.result?.let { result ->
                        serverFirmwareVersion = result.newFirmVer ?: "1.0.0"
                        val currentVersion = if (result.sensorVer.isNullOrEmpty()) {
                            firmwareData.firmwareVersion
                        } else {
                            result.sensorVer
                        }
                        /*Log.d(TAG, "getAPICall: ${hasUpdate(firmwareData.firmwareVersion, serverFirmwareVersion!!)}")
                        Log.d(TAG, "getAPICall1: ${firmwareData.firmwareVersion}")
                        Log.d(TAG, "getAPICall2: ${serverFirmwareVersion!!}")*/
                        if (hasUpdate(currentVersion, serverFirmwareVersion!!)) {
                            result.url?.let { url ->
                                _firmwareUpdate.tryEmit(
                                    FirmwareUpdateModel(
                                        filePath = path,
                                        url = url,
                                        firmwareVersion = currentVersion,
                                        updateVersion = serverFirmwareVersion!!,
                                        deviceName = firmwareData.deviceName,
                                        deviceAddress = firmwareData.deviceAddress,
                                        updateDetail = result.desc.toString()
                                    )
                                )
                            }
                        } else {
                            _checkFirmwareVersion.tryEmit(FirmwareDataModel())
                        }
                    } ?: run {
                        _checkFirmwareVersion.tryEmit(FirmwareDataModel())
                        cancel()
                        delay(100)
                    }
                }
        }


    }

    fun getFirmwareVersion(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val address = dataManager.getBluetoothDeviceAddress(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
            val deviceName = dataManager.getBluetoothDeviceName(SBBluetoothDevice.SB_SOOM_SENSOR.type.name).first()
            if (address == null || deviceName == null || getService()?.getSbSensorInfo()?.value?.bluetoothState == BluetoothState.DisconnectedByUser
                || getService()?.getSbSensorInfo()?.value?.bluetoothState == BluetoothState.DisconnectedNotIntent
                || getService()?.getSbSensorInfo()?.value?.bluetoothState == BluetoothState.Connecting) {
                _checkFirmwareVersion.tryEmit(FirmwareDataModel())
                dismissProgressBar()
                sendErrorMessage(ApplicationManager.instance.baseContext.getString(R.string.firmupdate_error_message))
                cancel()
                delay(100)
                return@launch
            }
            getService()?.getFirmwareVersion()?.collectLatest { firmwareData ->
                if (firmwareData == null) {
                    _nextAPICall.emit(
                        Pair(
                            path, FirmwareData(
                                firmwareVersion = "0.0.1",
                                deviceName = deviceName,
                                deviceAddress = address
                            )
                        )
                    )
                    return@collectLatest
                }
                _nextAPICall.emit(Pair(path, firmwareData))
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _nextAPICall.collectLatest {
                val (filePath, data) = it
                getAPICall(filePath, data)
                cancel()
                delay(100)
                return@collectLatest
            }
        }
    }

    private fun deviceDisconnectConnect(model: FirmwareUpdateModel) {
        viewModelScope.launch {
            getService()?.disconnectDevice()
            getService()?.getSbSensorInfo()?.collectLatest {
                if (it.bluetoothState == BluetoothState.DisconnectedByUser) {
                    downloadFirmware(model)

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
                insertLog("deviceConnect: 펌 완료 -- 상태 ${it.bluetoothState}")
                if (it.bluetoothState == BluetoothState.Connected.Init ||
                    it.bluetoothState == BluetoothState.Connected.Ready) {
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

    fun firmwareUpdateCall(model: FirmwareUpdateModel) {
        deviceDisconnectConnect(model)
    }
    
    fun sendFirmwareUpdate() {
        updateFirmVersion()
    }
    
    private fun updateFirmVersion() {
        viewModelScope.launch {
            _firmwareUpdate.value?.let {
                request { authAPIRepository.postRegisterFirmVersion(SensorFirmVersion(it.deviceName, it.updateVersion)) }.collectLatest { result ->
                    result.success
                    Log.d(TAG, "3333333333:  성공 성공 ${result.success}")
                }
            }
        }
    }

    override fun whereTag(): String {
        return "Firmware"
    }


}
    