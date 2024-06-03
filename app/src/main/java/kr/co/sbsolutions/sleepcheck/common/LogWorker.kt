package kr.co.sbsolutions.sleepcheck.common

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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.domain.db.LogDBDataRepository
import kr.co.sbsolutions.soomirang.db.LogData

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
            val timeStamp = inputData.getString("time") ?: ""
            LogData(0, timeStamp, message).log().first()
        }
//        saveData(data)
//        val data = downloadSynchronously("https://www.google.com")
    }

    private suspend fun LogData.log() = callbackFlow {
        logDBDataRepository.insertLogData(this@log)
        Log.d(TAG, "log: ${this@log}")
        dataManager.getUserName().first()?.let { name ->
            val logCollection = ff.collection("SleepCheck AOS").document(name).collection(timeId)
            val logDocument = logCollection.document("${this@log.time} - ${this@log.log}")
            val task = logDocument.set(hashMapOf<Void, Void>())
            task.addOnCompleteListener{
                Log.d(TAG, "log 기록 Result.success()")
                trySend(Result.success())
                close()
            }
            task.addOnFailureListener {
                Log.d(TAG, "log 기록 Result.retry()")
                trySend(Result.retry())
                close()
            }
        }
        awaitClose()
    }
}