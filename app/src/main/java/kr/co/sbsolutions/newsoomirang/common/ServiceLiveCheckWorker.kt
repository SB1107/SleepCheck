package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage

@HiltWorker
class ServiceLiveCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val logHelper: LogHelper

) : CoroutineWorker(context, workerParams) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        logHelper.insertLog("error =  ${throwable.message}")
    }
    private val ioDispatchers = Dispatchers.IO + coroutineExceptionHandler
    override suspend fun doWork(): Result = coroutineScope {
        withContext(ioDispatchers) {
            val isServiceRunning =  BLEService.getInstance()?.isForegroundServiceRunning() ?: false
            logHelper.insertLog("service live check = $isServiceRunning")
            if (isServiceRunning.not()) {
                Intent(context, BLEService::class.java).apply {
                    action = ActionMessage.StartSBService.msg
                    context.startForegroundService(this)
                    logHelper.insertLog("ServiceLiveCheckWorker 서비스 재시작 콜")
                }
            }
            return@withContext Result.success()
        }
    }
}