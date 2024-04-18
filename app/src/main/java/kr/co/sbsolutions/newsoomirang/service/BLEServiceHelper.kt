package kr.co.sbsolutions.newsoomirang.service

import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kr.co.sbsolutions.newsoomirang.common.DataFlowLogHelper
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.ServiceLiveCheckWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.UploadWorkerHelper
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource

class BLEServiceHelper(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val bluetoothNetworkRepository: IBluetoothNetworkRepository,
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val settingDataRepository: SettingDataRepository,
    private val timeHelper: TimeHelper,
    private val noseRingHelper: NoseRingHelper,
    private val logHelper: LogHelper,
    private val uploadWorkerHelper: UploadWorkerHelper,
    private val serviceLiveCheckWorkerHelper: ServiceLiveCheckWorkerHelper,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationManager: NotificationManager,
    private val dataFlowLogHelper: DataFlowLogHelper = DataFlowLogHelper(logHelper),
    private var lifecycleScope: LifecycleCoroutineScope? = null,
    private var sbSensorUseCase: SBSensorUseCase? = null,
    private var timeCountUseCase: TimeCountUseCase? = null,
    private  var noseRingUseCase : NoseRingUseCase? = null,
) {

    private fun listenChannelMessage() {
        sbSensorUseCase?.listenChannelMessage()
    }
    private fun listenTimer(){
        timeCountUseCase?.listenTimer()
    }
    private  fun setCallVibrationNotifications(){
        noseRingUseCase?.setCallVibrationNotifications()
    }

    fun setLifecycleScope(lifecycleScope: LifecycleCoroutineScope) {
        this.lifecycleScope = lifecycleScope
        this.sbSensorUseCase = SBSensorUseCase(sbSensorDBRepository, settingDataRepository, bluetoothNetworkRepository, lifecycleScope, dataFlowLogHelper)
        this.timeCountUseCase = TimeCountUseCase(lifecycleScope, timeHelper , dataManager , notificationBuilder, notificationManager, noseRingHelper)
        this.noseRingUseCase = NoseRingUseCase(lifecycleScope, noseRingHelper,timeHelper,settingDataRepository,bluetoothNetworkRepository)
        listenChannelMessage()
        listenTimer()
        setCallVibrationNotifications()
    }

}