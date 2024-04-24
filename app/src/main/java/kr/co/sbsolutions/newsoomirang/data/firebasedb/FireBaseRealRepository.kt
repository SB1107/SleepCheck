package kr.co.sbsolutions.newsoomirang.data.firebasedb

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.firebase.database.ChildEventListener
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.LogHelper
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import java.text.SimpleDateFormat
import java.util.Locale

class FireBaseRealRepository(private val realDatabase: FirebaseDatabase, private val logHelper: LogHelper) {
    private var lifecycleScope: LifecycleCoroutineScope? = null
    private var callback: ((RealData) -> Unit)? = null
    private var dataId: String? = null

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            Log.e(TAG, "onChildAdded: ${snapshot.value}")
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            Log.e(TAG, "onChildChanged: ${snapshot.value}")
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            snapshot.value?.let {
                val data = parserData(it)
                if (data.dataId == dataId) {
                    callback?.invoke(data)
                    Log.e(TAG, "onChildRemoved: idataid 매칭")
                }
                logHelper.insertLog("onChildRemoved: ${snapshot.value}")
            }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            Log.e(TAG, "onChildMoved: ${snapshot.value}")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e(TAG, "onCancelled: ${error.message}")
        }
    }

    fun setLifecycleScope(lifecycleScope: LifecycleCoroutineScope) {
        this.lifecycleScope = lifecycleScope
        val scoresRef = Firebase.database.reference
        scoresRef.keepSynced(true)
    }

    fun removeListener(sensorName: String) {
        realDatabase.reference.child("sensorNames").child(sensorName).removeEventListener(listener)
    }

    fun listenerData(sensorName: String, dataId: String, callback: (RealData) -> Unit) {
        removeListener(sensorName)
        this.callback = callback
        this.dataId = dataId
        Log.e(TAG, "listenerData:dataId = ${dataId} ")
        realDatabase.reference.child("sensorNames").child(sensorName).addChildEventListener(listener)
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
                getDataIdList(sensorName).first()?.filter { it != dataId.toString() }?.map {
                    Log.d(TAG, "writeValue: $it")
                    remove(sensorName, it)
                }
            }
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(System.currentTimeMillis())
            val data = RealData(sensorName = sensorName, dataId = dataId.toString(), userName = userName, sleepType = sleepType.name, timeStamp = timeStamp)
            realDatabase.reference.child("sensorNames").child(sensorName).child(dataId.toString()).setValue(data)
        }
    }

    fun remove(sensorName: String, dataId: String) {
        realDatabase.reference.child("sensorNames").child(sensorName).child(dataId).removeValue()
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
                    Log.e(TAG, "oneReadData: tempData")
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
data class RealData(val sensorName: String , val dataId: String , val userName: String , val sleepType: String, val timeStamp: String)