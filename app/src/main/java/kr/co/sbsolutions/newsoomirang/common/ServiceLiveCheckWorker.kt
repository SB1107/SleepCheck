package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
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
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG

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
            val device = BLEService.getInstance()?.isBleDeviceConnect()
            val isConnect = device?.first ?: false
            val deviceName = device?.second ?: ""
            Log.e(TAG, "doWork: isServiceRunning = $isServiceRunning" )
            logHelper.insertLog("service live check = $isServiceRunning isConnect = $isConnect deviceName = $deviceName")
//            if(isConnect.not()){
//                BLEService.getInstance()?.forceBleDeviceConnect()
//            }
            if (isServiceRunning.not()) {
//                Intent(context, BLEService::class.java).apply {
//                    action = ActionMessage.ReStartSBService.msg
//                    context.startForegroundService(this)
                    logHelper.insertLog("ServiceLiveCheckWorker ${BLEService.getInstance()}")
//                }
            }
            return@withContext Result.success()
        }
    }
}