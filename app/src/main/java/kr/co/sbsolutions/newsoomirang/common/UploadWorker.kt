package kr.co.sbsolutions.newsoomirang.common

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.opencsv.CSVWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
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
    private val logHelper: LogHelper,
    private val remoteAuthDataSource: RemoteAuthDataSource
) : CoroutineWorker(context, params) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        Log.d(TAG, "error =  ${throwable.message}")
    }
    private val ioDispatchers = Dispatchers.IO + coroutineExceptionHandler

    override suspend fun doWork(): Result = coroutineScope {
        withContext(ioDispatchers) {
            val packageName = inputData.getString("packageName") ?: ""
            val dataId = inputData.getInt("dataId", -1)
            val sleepType = inputData.getInt("sleepType", 0)
            val snoreTime = inputData.getLong("snoreTime", 0)
            val isFilePass = inputData.getBoolean("isFilePass", false)
            val sensorName = inputData.getString("sensorName") ?: ""
            val type = if (SleepType.Breathing.ordinal == sleepType) SleepType.Breathing else SleepType.NoSering
            if (dataId == -1) {
                return@withContext Result.failure(Data.Builder().apply { putString("reason", "dataId 오류") }.build())
            }

            if (isFilePass) {
                uploading(packageName, dataId, null, emptyList(), sleepType = type, snoreTime = snoreTime, sensorName = sensorName).first()
            } else {
                Log.e(TAG, "exportLastFile -dataId = $dataId sleepType = $sleepType  snoreTime = $snoreTime")
                val min = sbSensorDBRepository.getMinIndex(dataId)
                val max = sbSensorDBRepository.getMaxIndex(dataId)
                val size = sbSensorDBRepository.getSelectedSensorDataListCount(dataId, min, max).last()
                Log.d(TAG, "exportLastFile - Index From $min~$max = ${max - min + 1} / Data Size : $size")
                logHelper.insertLog("exportLastFile - Size : $size")
                if (size < 1000) {
                    Log.d(TAG, "exportLastFile - data size 1000 미만 : $size")
                    logHelper.insertLog("exportLastFile -  size 1000 미만 : $size")
                    return@withContext Result.failure(Data.Builder().apply { putString("reason", "size 1000 미만") }.build())
                }
                val sbList = sbSensorDBRepository.getSelectedSensorDataListByIndex(dataId, min, max).last()
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
                logHelper.insertLog("uploading: exportFile")
                uploading(packageName, dataId, file, sbList, sleepType = type, snoreTime = snoreTime, sensorName = sensorName).first()
            }
        }
    }

    private suspend fun uploading(
        packageName: String,
        dataId: Int,
        file: File?,
        list: List<SBSensorData>,
        sleepType: SleepType,
        snoreTime: Long = 0,
        sensorName: String
    ) =
        callbackFlow {
            withContext(ioDispatchers) {
                val requestHelper = RequestHelper(this@withContext, dataManager = dataManager, tokenManager = tokenManager)
                    .apply {
                        setLogWorkerHelper(logHelper)
                    }
                logHelper.insertLog("서버 업로드 시작")
                Intent().also { intent ->
                    intent.setAction(Cons.NOTIFICATION_ACTION)
                    intent.setPackage(packageName)
                    context.sendBroadcast(intent)
                }
                requestHelper.request(
                    request = { remoteAuthDataSource.postUploading(file = file, dataId = dataId, sleepType = sleepType, snoreTime = snoreTime, sensorName = sensorName) },
                    errorHandler = { error ->
                        logHelper.insertLog("uploading error: $error")
                        trySend(Result.retry())
                        close()
                    },
                    showProgressBar = false
                )
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