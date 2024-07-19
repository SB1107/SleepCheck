package kr.co.sbsolutions.sleepcheck.presenter.firmware

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.ApplicationManager
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.RequestHelper
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.common.getLanguage
import kr.co.sbsolutions.sleepcheck.common.hasUpdate
import kr.co.sbsolutions.sleepcheck.data.bluetooth.FirmwareData
import kr.co.sbsolutions.sleepcheck.data.entity.BaseEntity
import kr.co.sbsolutions.sleepcheck.data.server.ApiResponse
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource

class FirmwareHelper(private val remoteAuthDataSource: RemoteAuthDataSource, private  val dataManager: DataManager, private  val  tokenManager: TokenManager) {
    private val _updateCheckResult: MutableSharedFlow<Boolean> = MutableSharedFlow()
    private lateinit var requestHelper: RequestHelper

    fun getFirmwareVersion(lifecycleScope: CoroutineScope, bluetoothState: BluetoothState?, firmwareData: Flow<FirmwareData?>?): SharedFlow<Boolean> {
        this.requestHelper = RequestHelper(lifecycleScope, tokenManager = tokenManager , dataManager)
        lifecycleScope.launch(Dispatchers.IO) {
            bluetoothState?.let { infoState ->
                if (infoState == BluetoothState.Connected.Ready ||
                    infoState == BluetoothState.Connected.Init ||
                    infoState == BluetoothState.Connected.End
                ) {
                    firmwareData?.collectLatest { deviceInfo ->
                        Log.e(TAG, "getFirmwareVersion11: ${deviceInfo?.firmwareVersion}")
                        if (deviceInfo?.firmwareVersion.isNullOrEmpty()) {
                            _updateCheckResult.emit(true)
                            cancel()
                            delay(100)
                            return@collectLatest
                        }
                        deviceInfo?.let { getNewFirmVersion(lifecycleScope, it) }
                    }
                }else{
                    _updateCheckResult.emit(false)
                }
            }
        }
        return _updateCheckResult.asSharedFlow()
    }

    private suspend fun <T : BaseEntity> request(showProgressBar: Boolean = false, request: () -> Flow<ApiResponse<T>>): Flow<T> {
        return requestHelper.request(request, showProgressBar = showProgressBar)
    }

    private fun getNewFirmVersion(lifecycleScope: CoroutineScope, deviceInfo: FirmwareData?) {
        lifecycleScope.launch {
            deviceInfo?.let { info ->
                request { remoteAuthDataSource.getNewFirmVersion(info.deviceName, ApplicationManager.instance.baseContext.getLanguage()) }.collectLatest { result ->
                    Log.d(TAG, "getFirmwareVersion: $result")
                    result.result?.newFirmVer?.let { newFirmVer ->
                        val currentVersion = if (result.result.sensorVer.isNullOrEmpty()) {
                            deviceInfo.firmwareVersion
                        } else {
                            result.result.sensorVer
                        }
                        val hasUpdate = hasUpdate(currentVer = currentVersion, compareVer = newFirmVer)
                        _updateCheckResult.emit(hasUpdate)
                        Log.d(TAG, "getFirmwareVersion000: $hasUpdate")
                        cancel()
                        delay(100)
                    }
                }
            }
        }
    }
}