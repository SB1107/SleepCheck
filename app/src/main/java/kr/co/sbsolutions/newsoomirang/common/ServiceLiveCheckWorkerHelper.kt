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
    private val uuid = UUID.randomUUID()
    fun serviceLiveCheck(): LiveData<WorkInfo> {
        val workRequest = PeriodicWorkRequestBuilder<ServiceLiveCheckWorker>(5, TimeUnit.MINUTES).apply {
            setId(uuid)
            addTag("ServiceLiveCheckWorkerHelper")
            setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
        }
            .build()
        workManager.enqueue(workRequest)
        return workManager.getWorkInfoByIdLiveData(uuid)
    }

    fun cancelWork() {
        workManager.cancelWorkById(uuid)
        workManager.cancelAllWorkByTag("ServiceLiveCheckWorkerHelper")
    }

}