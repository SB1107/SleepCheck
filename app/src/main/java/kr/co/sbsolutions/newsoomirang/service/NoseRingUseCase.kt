package kr.co.sbsolutions.newsoomirang.service

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository

class NoseRingUseCase(
    private var lifecycleScope: LifecycleCoroutineScope,
    private val noseRingHelper: NoseRingHelper,
    private val timeHelper: TimeHelper,
    private val settingDataRepository: SettingDataRepository,
    private val bluetoothNetworkRepository: IBluetoothNetworkRepository,
) {

    fun setCallVibrationNotifications() {
        noseRingHelper.setCallVibrationNotifications {
            lifecycleScope.launch(IO) {
                val onOff = settingDataRepository.getSnoringOnOff()
                if (onOff) {
                    if (timeHelper.getTime() > (60 * 30)) {
                        callVibrationNotifications(settingDataRepository.getSnoringVibrationIntensity())
                    }
                }
            }
        }
    }
    private fun callVibrationNotifications(intensity: Int) {
        bluetoothNetworkRepository.callVibrationNotifications(intensity)
    }

}


