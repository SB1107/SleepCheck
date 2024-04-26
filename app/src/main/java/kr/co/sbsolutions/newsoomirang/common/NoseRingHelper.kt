package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import org.tensorflow.lite.support.label.Category

class NoseRingHelper {
    private var mSnoreTime: Long = 0
    private var mLastEventTime: Long = 0
    private var mContSnoringTime: Long = 0
    private var mCoughCount: Int = 0
    private var mSnoreCount: Int = 0
    private lateinit var callback: () -> Unit
    fun noSeringResult(results: List<Category?>?, inferenceTime: Long?) {
        results?.forEach { value ->
            if (value?.index == 38) { // 코골이만 측정
                val currentTime = System.currentTimeMillis()
                if (currentTime - mLastEventTime < 100000) {
                    val timeDelta: Long = currentTime - mLastEventTime
                    mSnoreTime += timeDelta
                    mContSnoringTime += timeDelta
                    if (mContSnoringTime > 10000) {
                        if (::callback.isInitialized) {
                            callback.invoke()
                        }
//                        callVibrationNotifications()
                    }
                    Log.d(TAG, "timeDelta: $timeDelta  mSnoreTime: $mSnoreTime mContSnoringTime $mContSnoringTime")
                } else {
                    mContSnoringTime = 0
                }
                Log.d(TAG, "currentTime: $currentTime  mLastEventTime: $mLastEventTime  =  ${currentTime - mLastEventTime}")
                mLastEventTime = currentTime
                inferenceTime?.let {
                    mSnoreTime += it
                }
                Log.d(TAG, "currentTime: $currentTime  mLastEventTime: $mLastEventTime currentTime: $currentTime")
            }
            if (value?.index == 42) {
                mCoughCount++
                Log.d(TAG, "mCoughCount: $mCoughCount ")
            }
        }
    }

    fun snoreCountIncrease(){
        mSnoreCount +=1
    }

    fun getSnoreTime(): Long {
        return mSnoreTime
    }

    fun getCoughCount() : Int {
        return mCoughCount
    }

    fun getSnoreCount() : Int {
        return mSnoreCount
    }

    fun setCallVibrationNotifications(callback: (() -> Unit)) {
        this.callback = callback
    }
    fun setSnoreTime(time : Long){
        this.mSnoreTime  = time
    }
    fun setSnoreCount(count : Int){
      this.mSnoreCount = count
    }
    fun setCoughCount(count: Int){
        this.mCoughCount = count
    }
    fun clearData() {
        mSnoreTime = 0
        mLastEventTime = 0
        mContSnoringTime = 0
        mCoughCount = 0
        mSnoreCount = 0
    }
}