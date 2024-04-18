package kr.co.sbsolutions.newsoomirang.common

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class LogWorkerHelper @Inject constructor(
    private val workManager: WorkManager
) {
    fun insertLog(message : String) {
        workManager.pruneWork()
        val timeStamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(System.currentTimeMillis())
        val  worker = OneTimeWorkRequestBuilder<LogWorker>().apply {
            addTag("log")
            setInputData(workDataOf("log" to message, "time" to timeStamp))
            setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
              1
                ,TimeUnit.MINUTES
            )
        }.build()
        workManager.enqueue(worker)
    }
}