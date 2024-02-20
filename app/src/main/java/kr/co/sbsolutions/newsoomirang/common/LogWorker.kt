package kr.co.sbsolutions.newsoomirang.common

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.db.LogDBDataRepository
import kr.co.sbsolutions.soomirang.db.LogData
import java.text.SimpleDateFormat
import java.util.Locale

@HiltWorker
class LogWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val logDBDataRepository: LogDBDataRepository,
    private val dataManager: DataManager,
    private val ff: FirebaseFirestore,
    private val timeId: String
) : CoroutineWorker(context, params) {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
        Log.d(TAG, "error =  ${throwable.message}")
    }
    private val ioDispatchers = Dispatchers.IO + coroutineExceptionHandler
    override suspend fun doWork(): Result = coroutineScope {
        withContext(ioDispatchers) {
            val message = inputData.getString("log") ?: ""
            LogData(0, SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(System.currentTimeMillis()), message).log()
        }
//        saveData(data)
//        val data = downloadSynchronously("https://www.google.com")
        Result.success()
    }

    private suspend fun LogData.log() {
        logDBDataRepository.insertLogData(this@log)
        dataManager.getUserName().first()?.let { name ->
            val logCollection = ff.collection("B2C").document(name).collection(timeId)
            val logDocument = logCollection.document("${this@log.time} - ${this@log.log}")

            logDocument.set(hashMapOf<Void, Void>())
        }
    }
}