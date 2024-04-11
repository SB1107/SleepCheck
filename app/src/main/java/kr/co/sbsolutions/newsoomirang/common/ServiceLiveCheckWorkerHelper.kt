package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ServiceLiveCheckWorkerHelper @Inject constructor(
    private val workManager: WorkManager
) {
    private val uuid = UUID.randomUUID()
    private val tag = "ServiceLiveCheckWorkerHelper"
    fun serviceLiveCheck(): LiveData<WorkInfo> {
        val workRequest = PeriodicWorkRequestBuilder<ServiceLiveCheckWorker>(1, TimeUnit.MINUTES).apply {
            setId(uuid)
            addTag(tag)

            setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
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