package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UploadWorkerHelper @Inject constructor(
    private val workManager: WorkManager
) {
    fun uploadData(uploadData: UploadData): LiveData<WorkInfo> {
        val uuid = UUID.randomUUID()
        val worker = OneTimeWorkRequestBuilder<UploadWorker>().apply {
            addTag("upload")
            setId(uuid)
            setInputData(
                workDataOf(
                    "packageName" to uploadData.packageName,
                    "dataId" to uploadData.dataId,
                    "sleepType" to uploadData.sleepType.ordinal,
                    "snoreTime" to uploadData.snoreTime,
                    "snoreCount" to uploadData.snoreCount,
                    "coughCount" to uploadData.coughCount,
                    "isFilePass" to uploadData.isFilePass,
                    "sensorName" to uploadData.sensorName
                )
            )
            setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
        }.build()
        workManager.enqueue(worker)
        Log.d(TAG, "uploadData: 된다된다.")
        return workManager.getWorkInfoByIdLiveData(uuid)
    }
}

data class UploadData(
    var packageName: String = "",
    val dataId: Int,
    val sleepType: SleepType = SleepType.Breathing,
    val snoreTime: Long = 0,
    val snoreCount: Int = 0,
    val coughCount: Int = 0,
    var sensorName: String = "",
    val isFilePass: Boolean = false
)