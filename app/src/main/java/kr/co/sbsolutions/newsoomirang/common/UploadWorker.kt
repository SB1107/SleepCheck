package kr.co.sbsolutions.newsoomirang.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.opencsv.CSVWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDBRepository
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.soomirang.db.SBSensorData
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val sbSensorDBRepository: SBSensorDBRepository,
    private val logWorkerHelper: LogWorkerHelper,
    private val remoteAuthDataSource: RemoteAuthDataSource
) : CoroutineWorker(context, params) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        Log.d(TAG, "error =  ${throwable.message}")
    }
    private val ioDispatchers = Dispatchers.IO + coroutineExceptionHandler

    override suspend fun doWork(): Result = coroutineScope {
        withContext(ioDispatchers) {
            val dataId = inputData.getInt("dataId", -1)
            val sleepType = inputData.getInt("sleepType", 0)
            val snoreTime = inputData.getLong("snoreTime", 0)
            val type = if (SleepType.Breathing.ordinal == sleepType) SleepType.Breathing else SleepType.NoSering
            if(dataId == -1){
                Result.failure(Data.Builder().apply { putString("reason", "dataId 오류") }.build())
            }
            exportLastFile(dataId, type, snoreTime).first()
        }
    }

    private suspend fun exportLastFile(dataId: Int, sleepType: SleepType, snoreTime: Long = 0)  = callbackFlow{
        withContext(ioDispatchers) {
            Log.e(TAG, "exportLastFile -dataId = $dataId sleepType = $sleepType  snoreTime = $snoreTime")
            val min = sbSensorDBRepository.getMinIndex(dataId)
            val max = sbSensorDBRepository.getMaxIndex(dataId)
            val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max)
            Log.d(TAG, "exportLastFile - Index From $min~$max = ${max - min + 1} / Data Size : $size")
            logWorkerHelper.insertLog("exportLastFile - Size : $size")
            if (size < 100) {
                Log.d(TAG, "exportLastFile - data size 1000 미만 : $size")
                logWorkerHelper.insertLog("exportLastFile -  size 100 미만 : $size")
                Result.failure(Data.Builder().apply { putString("reason", "size 100 미만") }.build())
            }
            val sbList = sbSensorDBRepository.getSelectedSensorDataListByIndex(dataId, min, max)
            val time = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date(System.currentTimeMillis()))
            val filePath = "${context.filesDir}/${time}($dataId).csv"
            val file = File(filePath)
            Log.d(TAG, "exportLastFile - make Start ${time}.csv")
            FileWriter(file).use { fw ->
                CSVWriter(fw).use { cw ->
//                    cw.writeNext(arrayOf("Index", "Time", "Capacitance", "calcAccX", "calcAccY", "calcAccZ", "accelerationX", "accelerationY", "accelerationZ", "moduleName", "deviceName"))
                    cw.writeNext(arrayOf("Index", "Time", "Capacitance", "calcAccX", "calcAccY", "calcAccZ"))
                    sbList.forEach { data ->
                        cw.writeNext(data.toArray())
                    }
                }
            }
            logWorkerHelper.insertLog("uploading: exportFile")
            uploading(dataId, file, sbList, sleepType = sleepType, snoreTime = snoreTime).collectLatest {
                trySend(it)
                close()
            }
        }
        awaitClose()
    }

    private suspend fun uploading(dataId: Int, file: File?, list: List<SBSensorData>, sleepType: SleepType, snoreTime: Long = 0) =
        callbackFlow {
            withContext(ioDispatchers) {
                val requestHelper = RequestHelper(this@withContext, dataManager = dataManager, tokenManager = tokenManager)
                    .apply {
                        setLogWorkerHelper(logWorkerHelper)
                    }
                logWorkerHelper.insertLog("서버 업로드 시작")
                Intent().also { intent ->
                    intent.setAction(Cons.NOTIFICATION_ACTION)
                    intent.setPackage(context.packageName)
                    context.sendBroadcast(intent)
                }
                requestHelper.request(request = { remoteAuthDataSource.postUploading(file = file, dataId = dataId, sleepType = sleepType, snoreTime = snoreTime) }, errorHandler = { error ->
                    logWorkerHelper.insertLog("uploading error: $error")
                    trySend(Result.retry())
                    close()
                }, showProgressBar = false)
                    .flowOn(Dispatchers.IO).collectLatest {
                        Log.e(TAG, "uploading:  collectLatest")
                        sbSensorDBRepository.deleteUploadedList(list)
                        file?.delete()
                        trySend(Result.success())
                        close()
                    }
                awaitClose()
            }
        }

}