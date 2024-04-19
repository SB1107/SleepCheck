package kr.co.sbsolutions.newsoomirang.data.firebasedb

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import java.text.SimpleDateFormat
import java.util.Locale

class FireBaseRealRepository(private val realDatabase: FirebaseDatabase) {
    private var lifecycleScope: LifecycleCoroutineScope? = null

    fun setLifecycleScope(lifecycleScope: LifecycleCoroutineScope) {
        this.lifecycleScope = lifecycleScope
    }

    private val postListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            dataSnapshot.value?.let {
                val realData = parserData(it)
                Log.e(TAG, "onDataChange: $realData")
            }

        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.e(TAG, "loadPost:onCancelled", databaseError.toException())
        }
    }
 fun listenerData(sensorName: String, dataId: String){
     realDatabase.reference.equalTo(sensorName).equalTo(dataId).addValueEventListener(postListener)
 }

    private fun isCheckSensorUse(sensorName: String, dataId: String) = callbackFlow<RealData?> {
        lifecycleScope?.launch(Dispatchers.IO) {
            send(oneReadData(sensorName, dataId).first())
            close()
        }
        awaitClose()
    }

    private fun parserData(data: Any): RealData {
        val tempData = data as Map<String, String>
        val realData = RealData(
            sensorName = tempData["sensorName"] ?: "",
            dataId = tempData["dataId"] ?: "",
            userName = tempData["userName"] ?: "",
            sleepType = tempData["sleepType"] ?: "",
            timeStamp = tempData["timeStamp"] ?: "",
        )
        return realData
    }

    fun writeValue(sensorName: String, dataId: Int, sleepType: SleepType, userName: String) {
        lifecycleScope?.launch(Dispatchers.IO) {
            isCheckSensorUse(sensorName, dataId.toString()).first()?.let {
                Log.e(TAG, "writeValue: error 기존 데이터 있음", )
            } ?: run {
                val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(System.currentTimeMillis())
                val data = RealData(sensorName = sensorName, dataId = dataId.toString(), userName = userName, sleepType = sleepType.name, timeStamp = timeStamp)
                realDatabase.reference.setValue(data)
            }
        }

    }

    fun oneReadData(sensorName: String, dataId: String): Flow<RealData?> = callbackFlow {
        realDatabase.reference.equalTo(sensorName).equalTo(dataId).get().addOnSuccessListener {
            Log.e(TAG, "oneReadData: ${it.value}")
            it.value?.let { data ->
                trySend(parserData(data))
            } ?: trySend(null)
        }.addOnFailureListener {
                Log.e(TAG, "addOnFailureListener: $it")
            }
        awaitClose()
    }

}

@IgnoreExtraProperties
data class RealData(val sensorName: String, val dataId: String, val userName: String, val sleepType: String, val timeStamp: String) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "sensorName" to sensorName,
            "dataId" to dataId,
            "userName" to userName,
            "sleepType" to sleepType,
            "timeStamp" to timeStamp,
        )
    }
}