package kr.co.sbsolutions.newsoomirang.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataFlowLogHelper
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.ServiceLiveCheckWorkerHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.common.UploadWorkerHelper
import kr.co.sbsolutions.newsoomirang.data.firebasedb.FireBaseRealRepository
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource

class BLEServiceHelper(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val bluetoothNetworkRepository: IBluetoothNetworkRepository,
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val settingDataRepository: SettingDataRepository,
    private val timeHelper: TimeHelper,
    private val noseRingHelper: NoseRingHelper,
    private val logHelper: LogHelper,
    private val uploadWorkerHelper: UploadWorkerHelper,
    private val fireBaseRealRepository: FireBaseRealRepository,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationManager: NotificationManager,
    private val dataFlowLogHelper: DataFlowLogHelper = DataFlowLogHelper(logHelper),
    private var lifecycleScope: LifecycleCoroutineScope? = null,
    private var sbSensorUseCase: SBSensorUseCase? = null,
    private var timeCountUseCase: TimeCountUseCase? = null,
    private var noseRingUseCase: NoseRingUseCase? = null,
    private var blueToothUseCase: SBSensorBlueToothUseCase? = null,
    private var sbDataUploadingUseCase: SBDataUploadingUseCase? = null,
) {

    private fun listenChannelMessage() {
        sbSensorUseCase?.listenChannelMessage()
    }

    private fun listenTimer() {
        timeCountUseCase?.listenTimer()
    }

    private fun setCallVibrationNotifications() {
        noseRingUseCase?.setCallVibrationNotifications()
    }

    private fun listenRegisterSBSensor() {
        blueToothUseCase?.listenRegisterSBSensor()
    }

    private fun listenDataFlowForceFinish() {
        blueToothUseCase?.listenDataFlowForceFinish()
    }


    fun setLifecycleScope(context: Context, lifecycleScope: LifecycleCoroutineScope, lifecycleOwner: LifecycleOwner, packageName: String) {
        this.lifecycleScope = lifecycleScope
        fireBaseRealRepository.setLifecycleScope(lifecycleScope)

        this.sbDataUploadingUseCase = SBDataUploadingUseCase(settingDataRepository, lifecycleScope, logHelper, lifecycleOwner, uploadWorkerHelper)
        this.blueToothUseCase = SBSensorBlueToothUseCase(
            lifecycleScope,
            bluetoothNetworkRepository,
            dataFlowLogHelper,
            sbSensorDBRepository,
            settingDataRepository,
            dataManager,
            sbDataUploadingUseCase!!,
            fireBaseRealRepository,
            logHelper,
            packageName
        )
        this.noseRingUseCase = NoseRingUseCase(context, lifecycleScope, noseRingHelper, timeHelper, settingDataRepository, dataManager, blueToothUseCase)
        this.sbSensorUseCase = SBSensorUseCase(sbSensorDBRepository, settingDataRepository, blueToothUseCase, lifecycleScope, dataFlowLogHelper)
        this.timeCountUseCase = TimeCountUseCase(lifecycleScope, timeHelper, dataManager, notificationBuilder, notificationManager, noseRingHelper)
        listenChannelMessage()
        listenTimer()
        setCallVibrationNotifications()
        listenRegisterSBSensor()
        listenDataFlowForceFinish()
        this.blueToothUseCase?.setNoseRingUseCase(noseRingUseCase!!)
        this.sbDataUploadingUseCase?.setNoseRingUseCase(noseRingUseCase!!)

        // TODO: 리얼 데이터 베이스 실시간 감시 처리
        lifecycleScope.launch(Dispatchers.IO) {
            fireBaseRealRepository.listenerData(blueToothUseCase!!.getSensorName(), blueToothUseCase!!.getDataId().toString()){ onDataChange ->
                Log.e(TAG, "listenerData: $onDataChange" )
                blueToothUseCase?.setRealDataChange(onDataChange)
            }
            // TODO: 데이터 아이디 확인 메소드 샘플
            fireBaseRealRepository.getDataIdList(blueToothUseCase!!.getSensorName()).collectLatest {
                Log.e(TAG, "oneReadData:11 $it")
            }
            // TODO: 한번 데이터 아이디로 조회 용
//            fireBaseRealRepository.oneDataIdReadData(blueToothUseCase!!.getSensorName(), blueToothUseCase!!.getDataId().toString()).collectLatest {
//                Log.e(TAG, "oneReadData:11 ${it}")
//            }
        }
    }

    fun blueToothState(isEnabled: Boolean) {
        blueToothUseCase?.blueToothState(isEnabled)
    }

    fun blueToothConnectedDevice(device: BluetoothDevice?) {
        blueToothUseCase?.connectedDevice(device)
    }

    fun uploadingFinishForceCloseCallback(callback: ((Boolean) -> Unit)) {
        sbDataUploadingUseCase?.setCallback(callback)
        listenDataFlowForceFinish()
    }

    fun sbConnectDevice(context: Context, bluetoothAdapter: BluetoothAdapter?, isForceBleDeviceConnect: Boolean = false) {
        blueToothUseCase?.connectDevice(context, bluetoothAdapter, isForceBleDeviceConnect)
    }

    fun sbDisconnectDevice() {
        blueToothUseCase?.disconnectDevice()
    }

    private fun startTimer() {
        timeCountUseCase?.startTimer()
    }

    private fun stopTimer() {
        timeCountUseCase?.stopTimer()
    }

    fun releaseResource() {
        blueToothUseCase?.releaseResource()
    }

    fun startScheduler() {
        blueToothUseCase?.startScheduler()
    }

    fun registerDownloadCallback() {
        blueToothUseCase?.registerDownloadCallback()
    }

    private fun unregisterDownloadCallback() {
        blueToothUseCase?.unregisterDownloadCallback()
    }

    fun forceDataFlowDataUploadCancel() {
        blueToothUseCase?.forceDataFlowDataUploadCancel()
    }

    fun forceDataFlowDataUpload() {
        blueToothUseCase?.forceDataFlowDataUpload()
    }

    fun stopSBServiceForced() {
        blueToothUseCase?.stopSBServiceForced()
    }

    fun stopSBSensor(isCancel: Boolean = false) {
        blueToothUseCase?.stopSBSensor(isCancel)
        blueToothUseCase?.finishStop {
            finishSenor()
        }
    }

    fun removeDataId() {
        blueToothUseCase?.removeDataId()
    }

    private fun stopAudioClassification() {
        noseRingUseCase?.stopAudioClassification()
    }

    fun noSensorSeringMeasurement(isCancel: Boolean = false) {
        blueToothUseCase?.noSensorSeringMeasurement(isCancel)
        timeCountUseCase?.stopTimer()
    }

    suspend fun startSBService(context: Context, bluetoothAdapter: BluetoothAdapter?) {
        blueToothUseCase?.startSBService(context, bluetoothAdapter)
        val message = "${if (blueToothUseCase?.getSleepType() == SleepType.Breathing) "호흡" else "코골이"} 측정 중"
        timeCountUseCase?.setContentTitle(message)
        if (blueToothUseCase?.isBlueToothStateRegistered() == true) {
            blueToothUseCase?.setDataId()
            timeCountUseCase?.setTimeAndStart()
            noseRingUseCase?.setNoseRingDataAndStart()
        }
    }

    fun stopSBService() {
        val message = "${if (blueToothUseCase?.getSleepType() == SleepType.Breathing) "호흡" else "코골이"} 측정 종료"
        timeCountUseCase?.setContentTitle(message)
        logHelper.insertLog(message)
        blueToothUseCase?.stopScheduler()
        blueToothUseCase?.stopOperateDownloadSbSensor()
    }

    fun cancelSbService() {
        val message = "${if (blueToothUseCase?.getSleepType() == SleepType.Breathing) "호흡" else "코골이"} 측정 취소"
        logHelper.insertLog(message)
        blueToothUseCase?.stopScheduler()
        blueToothUseCase?.stopOperateDownloadSbSensor()
        blueToothUseCase?.deletePastList()
    }

    suspend fun checkDataSize(): Flow<Boolean> {
        return blueToothUseCase?.checkDataSize() ?: flow { emit(false) }
    }

    fun getSleepType(): SleepType {
        return blueToothUseCase?.getSleepType() ?: SleepType.Breathing
    }


    fun setContentIntent(pendingIntent: PendingIntent) {
        timeCountUseCase?.setContentIntent(pendingIntent)
    }

    fun startSBSensor(dataId: Int, sleepType: SleepType, hasSensor: Boolean = true) {
        blueToothUseCase?.startSBSensor(dataId, sleepType, hasSensor)
        if (hasSensor.not()) {
            waitStart()
        }
        blueToothUseCase?.checkWaitStart {
            waitStart()
        }
    }

    private fun waitStart() {
        blueToothUseCase?.waitStart()
        startTimer()
        noseRingUseCase?.startAudioClassification()
    }

    private fun finishSenor() {
        blueToothUseCase?.finishSenor()
        stopTimer()
        stopAudioClassification()
    }

    private fun resultMessageClear() {
        sbDataUploadingUseCase?.resultMessageClear()
    }

    fun getResultMessage(): String? {
        return sbDataUploadingUseCase?.getResultMessage()
    }

    fun finishService(isForcedClose: Boolean) {
        resultMessageClear()
        unregisterDownloadCallback()
        blueToothUseCase?.endNetworkSBSensor(isForcedClose)
        blueToothUseCase?.sendDownloadContinueCancel()
        finishSenor()
        noseRingUseCase?.clearData()
        logHelper.insertLog("finishService")
        lifecycleScope?.launch(Dispatchers.IO) {
            dataManager.setNoseRingTimer(0L)
            dataManager.setTimer(0)
        }
    }

    fun getSbSensorInfo(): StateFlow<BluetoothInfo> {
        return blueToothUseCase?.getSbSensorInfo() ?: MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR))
    }

    fun getDataFlowPopUp(): StateFlow<Boolean> {
        return sbDataUploadingUseCase?.getDataFlowPopUp() ?: MutableStateFlow(false)
    }

    fun getTimeHelper(): SharedFlow<Triple<Int, Int, Int>> {
        return timeCountUseCase?.getTimeHelper() ?: MutableStateFlow(Triple(0, 0, 0))
    }

    fun getTime(): Int {
        return timeCountUseCase?.getTime() ?: 0
    }

    fun getNotificationBuilder(): NotificationCompat.Builder {
        return timeCountUseCase?.getNotificationBuilder() ?: notificationBuilder
    }

    fun getNotificationManager(): NotificationManager {
        return timeCountUseCase?.getNotificationManager() ?: notificationManager
    }

    fun isBleDeviceConnect(): Pair<Boolean, String> {
        return blueToothUseCase?.isBleDeviceConnect() ?: Pair(false, "")

    }
}