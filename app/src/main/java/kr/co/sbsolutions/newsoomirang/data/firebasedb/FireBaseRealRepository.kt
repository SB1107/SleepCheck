package kr.co.sbsolutions.newsoomirang.data.firebasedb

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Exclude
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import java.text.SimpleDateFormat
import java.util.Locale

class FireBaseRealRepository(private val realDatabase: FirebaseDatabase) {
    private var lifecycleScope: LifecycleCoroutineScope? = null

    fun setLifecycleScope(lifecycleScope: LifecycleCoroutineScope) {
        this.lifecycleScope = lifecycleScope
        val scoresRef = Firebase.database.reference
        scoresRef.keepSynced(true)
    }

    private val postListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            dataSnapshot.value?.let {
//                val realData = parserData(it)
                Log.e(TAG, "onDataChange: $it")
            }

        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.e(TAG, "loadPost:onCancelled", databaseError.toException())
        }
    }

    fun listenerData(sensorName: String, dataId: String) {
        realDatabase.reference.child("sensorNames").child(sensorName).child(dataId).addValueEventListener(postListener)
    }

    suspend fun isCheckSensorUse(sensorName: String): Boolean {
        return getDataIdList(sensorName).first()?.isNotEmpty() ?: false
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
            if (isCheckSensorUse(sensorName)) {

            }
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(System.currentTimeMillis())
            val data = RealData(sensorName = sensorName, dataId = dataId.toString(), userName = userName, sleepType = sleepType.name, timeStamp = timeStamp)
            realDatabase.reference.child("sensorNames").child(sensorName).child(dataId.toString()).setValue(data)
        }
    }

    fun remove(sensorName: String, dataId: Int) {
        realDatabase.reference.child("sensorNames").child(sensorName).child(dataId.toString()).removeValue()
    }

    fun getDataIdList(sensorName: String) = callbackFlow {
        realDatabase.reference.child("sensorNames").child(sensorName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.value?.let {
                    val tempData = it as Map<String, String>
                    trySend(tempData.keys.toList())
                    close()
                } ?: run {
                    Log.e(TAG, "oneReadData: data없음")
                    trySend(null)
                    cancel()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close()
            }
        })
        awaitClose()
    }

    fun oneDataIdReadData(sensorName: String, dataId: String): Flow<RealData?> = callbackFlow {
        realDatabase.reference.child("sensorNames").child(sensorName).child(dataId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.value?.let {
                    val tempData = parserData(it)
                    trySend(tempData)
                    close()
                } ?: run {
                    Log.e(TAG, "oneReadData: data없음")
                    trySend(null)
                    cancel()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
                close()
            }
        })
        awaitClose()
    }
}

@IgnoreExtraProperties
data class RealWriteValue(val dataId: String)

@IgnoreExtraProperties
data class RealData(val sensorName: String, val dataId: String, val userName: String, val sleepType: String, val timeStamp: String) {
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userName" to userName,
            "sleepType" to sleepType,
            "timeStamp" to timeStamp,
        )
    }
}