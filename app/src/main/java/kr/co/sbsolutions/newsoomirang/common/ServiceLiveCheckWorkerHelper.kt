package kr.co.sbsolutions.newsoomirang.common

import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ServiceLiveCheckWorkerHelper @Inject constructor(
    private val workManager: WorkManager
) {
    private val uuid = UUID.nameUUIDFromBytes("ServiceLiveCheckWorkerHelper".toByteArray())
    private val tag = "ServiceLiveCheckWorkerHelper"
    fun serviceLiveCheck(): LiveData<WorkInfo> {
        val workRequest = PeriodicWorkRequestBuilder<ServiceLiveCheckWorker>(15, TimeUnit.MINUTES).apply {
            setId(uuid)
            addTag(tag)

            setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                2, TimeUnit.MINUTES
            )
        }.build()
        workManager.enqueue(workRequest)
        return workManager.getWorkInfoByIdLiveData(uuid)
    }

    fun cancelWork() {
        workManager.cancelWorkById(uuid)
        workManager.cancelAllWorkByTag(tag)
    }

}