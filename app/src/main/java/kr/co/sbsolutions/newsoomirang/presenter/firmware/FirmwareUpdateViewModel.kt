package kr.co.sbsolutions.newsoomirang.presenter.firmware

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.hasUpdate
import kr.co.sbsolutions.newsoomirang.data.api.DownloadServiceAPI
import kr.co.sbsolutions.newsoomirang.data.bluetooth.FirmwareData
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.domain.repository.DownloadAPIRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import no.nordicsemi.android.dfu.DfuServiceInitiator
import okhttp3.OkHttpClient
import okio.use
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class FirmwareUpdateViewModel
@Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
    @Named("Default")
    private val provideDefaultOkHttpClient: OkHttpClient,
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _checkFirmWaveVersion: MutableStateFlow<Pair<Boolean, DfuServiceInitiator?>> = MutableStateFlow(Pair(false, null))
    val checkFirmWaveVersion: StateFlow<Pair<Boolean, DfuServiceInitiator?>> = _checkFirmWaveVersion
    private var firmwareDataValue: FirmwareData? = null

    private fun downloadFirmware(url: String, path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            DownloadAPIRepository(
                Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create())
                    .client(provideDefaultOkHttpClient).build().create(DownloadServiceAPI::class.java)
            )
                .getDownloadZipFile()
                .collect {
                    when (it) {
                        is ApiResponse.Failure -> {
                            dismissProgressBar()
                            sendErrorMessage(it.errorCode.msg)
                        }

                        ApiResponse.Loading -> {
                            showProgressBar()
                        }

                        ApiResponse.ReAuthorize -> {}

                        is ApiResponse.Success -> {
                            dismissProgressBar()
                            val urls = url.split("/")
                            val fileName = urls.last()
                            val tempFile = File(path, fileName)
                            it.data.let { data ->
                                data.byteStream().use { inputStream ->
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
                                    _checkFirmWaveVersion.tryEmit(Pair(true, starter))
                                }
                                cancel()
                            }
                        }
                    }
                }
        }

    }

    fun getFirmwareVersion(path: String) {
        viewModelScope.launch {
            getService()?.getFirmwareVersion()?.collectLatest { firmwareData ->
                if (firmwareData == null) {
                    _checkFirmWaveVersion.tryEmit(Pair(false, null))
                    cancel()
                    return@collectLatest
                }

                request { authAPIRepository.getNewFirmVersion(firmwareData.deviceName) }.collectLatest { firmware ->
                    firmware.result?.let { result ->
                        if (hasUpdate(firmwareData.firmwareVersion, result.newFirmVer ?: "1.0.0")) {
                            result.url?.let { url ->
                                firmwareDataValue = firmwareData
                                Log.e(TAG, "getFirmwareVersion: ${url}", )
                                deviceDisconnectConnect(url, path)
                            }
                            cancel()
                            delay(100)
                        } else {
                            _checkFirmWaveVersion.tryEmit(Pair(false, null))
                            cancel()
                            delay(100)
                        }
                    } ?: run {
                        _checkFirmWaveVersion.tryEmit(Pair(false, null))
                        cancel()
                        delay(100)
                    }
                }

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
    