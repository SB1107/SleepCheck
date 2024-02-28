package kr.co.sbsolutions.newsoomirang.common

import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UploadWorkerHelper @Inject constructor(
    private val workManager: WorkManager
){
    fun  uploadData(dataId : Int , isForcedClose : Boolean, sleepType : SleepType, snoreTime : Long) :  LiveData<WorkInfo> {
        val uuid = UUID.randomUUID()
        val  worker = OneTimeWorkRequestBuilder<UploadWorker>().apply {
            addTag("upload")
            setId(uuid)
            setInputData(workDataOf("dataId" to  dataId))
            setInputData(workDataOf("isForcedClose" to  isForcedClose))
            setInputData(workDataOf("sleepType" to  sleepType))
            setInputData(workDataOf("snoreTime" to  snoreTime))
            setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1
                , TimeUnit.MINUTES
            )
        }.build()
       workManager.enqueue(worker)
        return  workManager.getWorkInfoByIdLiveData(uuid)
    }
}