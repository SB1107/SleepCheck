package kr.co.sbsolutions.newsoomirang.common

import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import javax.inject.Inject

class LogWorkerHelper @Inject constructor(
    private val workManager: WorkManager
) {
    fun insertLog(message : String) {
        val  worker = OneTimeWorkRequestBuilder<LogWorker>().apply {
            setConstraints(Constraints.Builder().run {
                setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            })
            addTag("log")
            setInputData(workDataOf("log" to  message.plus( " To Worker")))
        }.build()
        workManager.enqueue(worker)
    }
}