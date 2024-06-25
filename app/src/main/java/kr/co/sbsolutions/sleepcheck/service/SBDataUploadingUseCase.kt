package kr.co.sbsolutions.sleepcheck.service

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.UploadData
import kr.co.sbsolutions.sleepcheck.common.UploadWorkerHelper
import kr.co.sbsolutions.sleepcheck.domain.db.SettingDataRepository
import kr.co.sbsolutions.sleepcheck.domain.model.SleepType
import kr.co.sbsolutions.sleepcheck.service.BLEService.Companion.FINISH
import kr.co.sbsolutions.sleepcheck.service.BLEService.Companion.UPLOADING

class SBDataUploadingUseCase(
    private val settingDataRepository: SettingDataRepository,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val logHelper: ILogHelper,
    private val lifecycleOwner: LifecycleOwner,
    private val uploadWorkerHelper: UploadWorkerHelper,
    private var callback: ((Boolean) -> Unit)? = null,
    private val INoseRingHelper: INoseRingHelper,
) {
    private val _resultMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    private val _dataFlowPopUp: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val dataFlowPopUp: StateFlow<Boolean> = _dataFlowPopUp
    private val dataInsufficientUploadFail: MutableSharedFlow<String> = MutableSharedFlow()

    fun setCallback(callback: (Boolean) -> Unit) {
        this.callback = callback
    }

    suspend fun dataFlowPopUpShow() {
        _dataFlowPopUp.emit(true)
    }

    suspend fun dataFlowPopUpDismiss() {
        _dataFlowPopUp.emit(false)
    }

    suspend fun uploading(
        packageName: String,
        sensorName: String,
        dataId: Int,
        forceClose: Boolean = false,
        isFilePass: Boolean = false,
        isForced: Boolean = false,
        uploadSucceededCallback: () -> Unit
    ) {
        val sleepType =
            if (settingDataRepository.getSleepType() == SleepType.Breathing.name) SleepType.Breathing else SleepType.NoSering
        logHelper.insertLog("uploading:${sleepType}  업로드")
        _resultMessage.emit(UPLOADING)
        val data = UploadData(
            dataId = dataId,
            sleepType = sleepType,
            snoreTime = INoseRingHelper.getSnoreTime(),
            snoreCount = INoseRingHelper.getSnoreCount(),
            coughCount = INoseRingHelper.getCoughCount()
        )
        uploadWorker(
            data,
            packageName,
            sensorName,
            forceClose,
            isFilePass,
            isForced,
            uploadSucceededCallback
        )
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

    private suspend fun uploadWorker(
        uploadData: UploadData,
        packageName: String,
        sensorName: String,
        forceClose: Boolean,
        isFilePass: Boolean = false,
        isForced: Boolean = false,
        uploadSucceededCallback: () -> Unit
    ) {
        _resultMessage.emit(UPLOADING)
        lifecycleScope.launch(Dispatchers.Main) {
            val newUploadData = uploadData.copy(
                packageName = packageName,
                sensorName = sensorName,
                isFilePass = isFilePass
            )
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
                                        uploadWorker(
                                            uploadData,
                                            packageName,
                                            sensorName,
                                            forceClose,
                                            isFilePass,
                                            uploadSucceededCallback = uploadSucceededCallback
                                        )
                                    } else {
                                        dataInsufficientUploadFail.emit("${reason}으로 데이터 업로드를 실패했습니다.")
                                        if (isForced) {
                                            callback?.invoke(true)
                                        }
                                    }
                                }
                            }

                            WorkInfo.State.BLOCKED -> {}
                            WorkInfo.State.CANCELLED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서버 업로드 취소")
                                }
                            }

                            WorkInfo.State.SUCCEEDED -> {
                                lifecycleScope.launch(IO) {
                                    logHelper.insertLog("서버 업로드 종료")
                                    _resultMessage.emit(FINISH)
                                    uploadSucceededCallback.invoke()
                                    callback?.invoke(forceClose)
                                }
                            }
                        }
                    }
                }
        }
    }

    fun getUploadFailError(): SharedFlow<String> {
        return dataInsufficientUploadFail.asSharedFlow()
    }

    fun getDataFlowPopUp(): StateFlow<Boolean> {
        return dataFlowPopUp
    }
}