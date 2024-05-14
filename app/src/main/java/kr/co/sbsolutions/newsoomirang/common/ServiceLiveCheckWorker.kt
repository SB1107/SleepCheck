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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.presenter.ActionMessage
import kr.co.sbsolutions.newsoomirang.service.BLEService

@HiltWorker
class ServiceLiveCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val logHelper: LogHelper,
    private val dataManager: DataManager

) : CoroutineWorker(context, workerParams) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        logHelper.insertLog("error =  ${throwable.message}")
    }
    private val ioDispatchers = Dispatchers.IO + coroutineExceptionHandler
    override suspend fun doWork(): Result = coroutineScope {
        withContext(ioDispatchers) {
            val isServiceRunning = BLEService.getInstance()?.isForegroundServiceRunning() ?: false
            val device = BLEService.getInstance()?.isBleDeviceConnect()
            val isConnect = device?.first ?: false
            val deviceName = device?.second ?: ""
            Log.e(TAG, "doWork: isServiceRunning = $isServiceRunning")
            logHelper.insertLog("service live check = $isServiceRunning isConnect = $isConnect deviceName = $deviceName")
            val hasSensor = dataManager.getHasSensor().first()
            if (isServiceRunning.not()) {
                if (dataManager.getStartTime().first().isTwelveHoursPassed()) {
                    logHelper.insertLog("ServiceLiveCheckWorker 측정후  12시간 지남")
                    return@withContext Result.success()
                }
                Intent(context, BLEService::class.java).apply {
                    action = ActionMessage.StartSBService.msg
                    context.startForegroundService(this)
                    logHelper.insertLog("ServiceLiveCheckWorker ${BLEService.getInstance()}")
                }
            }
            if (isConnect.not() && hasSensor && isServiceRunning) {
                BLEService.getInstance()?.connectDevice(true)
                logHelper.insertLog("ServiceWorker connect call")
            }
            return@withContext Result.success()
        }
    }
}