package kr.co.sbsolutions.newsoomirang.service

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.common.UploadData
import kr.co.sbsolutions.newsoomirang.common.UploadWorkerHelper
import kr.co.sbsolutions.newsoomirang.data.firebasedb.FireBaseRealRepository
import kr.co.sbsolutions.newsoomirang.domain.db.SettingDataRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.service.BLEService.Companion.FINISH
import kr.co.sbsolutions.newsoomirang.service.BLEService.Companion.UPLOADING

class SBDataUploadingUseCase(
    private val settingDataRepository: SettingDataRepository,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val logHelper: LogHelper,
    private val lifecycleOwner: LifecycleOwner,
    private val uploadWorkerHelper: UploadWorkerHelper,
    private var callback: ((Boolean) -> Unit)? = null
) {
    private var sbSensorBlueToothUseCase: SBSensorBlueToothUseCase? = null
    private var noseRingUseCase: NoseRingUseCase? = null
    private val _resultMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _dataFlowPopUp: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val dataFlowPopUp: StateFlow<Boolean> = _dataFlowPopUp

    fun setDataUploadingUseCase(sbSensorBlueToothUseCase: SBSensorBlueToothUseCase) {
        this.sbSensorBlueToothUseCase = sbSensorBlueToothUseCase
    }

    fun setNoseRingUseCase(noseRingUseCase: NoseRingUseCase) {
        this.noseRingUseCase = noseRingUseCase
    }

    fun setCallback(callback: (Boolean) -> Unit) {
        this.callback = callback
    }

    suspend fun dataFlowPopUpShow() {
        _dataFlowPopUp.emit(true)
    }

    suspend fun dataFlowPopUpDismiss() {
        _dataFlowPopUp.emit(false)
    }

    suspend fun uploading(packageName: String, sensorName: String, dataId: Int, forceClose: Boolean = false, isFilePass: Boolean = false) {
        val sleepType = if (settingDataRepository.getSleepType() == SleepType.Breathing.name) SleepType.Breathing else SleepType.NoSering
        logHelper.insertLog("uploading:${sleepType}  업로드")
        _resultMessage.emit(UPLOADING)
        val data = UploadData(
            dataId = dataId,
            sleepType = sleepType,
            snoreTime = noseRingUseCase?.getSnoreTime() ?: 0,
            snoreCount = noseRingUseCase?.getSnoreCount() ?: 0,
            coughCount = noseRingUseCase?.getCoughCount() ?: 0
        )
        uploadWorker(data, packageName, sensorName, forceClose, isFilePass)
    }

    fun getFinishForceCloseCallback(): ((Boolean) -> Unit)? {
        return callback
    }

    fun resultMessageClear() {
        lifecycleScope.launch(IO) {
            _resultMessage.emit(null)
        }
    }

    fun getResultMessage(): String? {
        return _resultMessage.value
    }

    private suspend fun uploadWorker(uploadData: UploadData, packageName: String, sensorName: String, forceClose: Boolean, isFilePass: Boolean = false) {
        _resultMessage.emit(UPLOADING)
        lifecycleScope.launch(Dispatchers.Main) {
            val newUploadData = uploadData.copy(packageName = packageName, sensorName = sensorName, isFilePass = isFilePass)
            uploadWorkerHelper.uploadData(newUploadData)
                .observe(lifecycleOwner) { workInfo: WorkInfo? ->
                    if (workInfo != null) {
                        when (workInfo.state) {
                            WorkInfo.State.ENQUEUED -> {}
                            WorkInfo.State.RUNNING -> {}
                            WorkInfo.State.FAILED -> {
                                lifecycleScope.launch(IO) {
                                    val reason = workInfo.outputData.getString("reason")
                                    logHelper.insertLog("서버 업로드 실패 - ${workInfo.outputData.keyValueMap}")
                                    if (reason == null) {
                                        uploadWorkerHelper.cancelWork()
                                        uploadWorker(uploadData, packageName, sensorName, forceClose, isFilePass)
                                    }
                                }
                            }

                            WorkInfo.State.BLOCKED -> {}
                            WorkInfo.State.CANCELLED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서버 업로드 취소}")
                                }
                            }

                            WorkInfo.State.SUCCEEDED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서버 업로드 종료")
                                    _resultMessage.emit(FINISH)
                                    sbSensorBlueToothUseCase?.uploadingFinish()
                                    callback?.invoke(forceClose)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun getDataFlowPopUp(): StateFlow<Boolean> {
        return dataFlowPopUp
    }
}