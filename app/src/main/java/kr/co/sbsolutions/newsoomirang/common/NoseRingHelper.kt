package kr.co.sbsolutions.newsoomirang.common

import org.tensorflow.lite.support.label.Category
import javax.inject.Inject

class NoseRingHelper {
    private var mSnoreTime: Long = 0
    private var mLastEventTime: Long = 0
    private var mContSnoringTime: Long = 0
    private lateinit var  callback: () -> Unit
    fun noSeringResult(results: List<Category?>?, inferenceTime: Long?) {
        results?.forEach { value ->
            if (value?.index == 38) { // 코골이만 측정
                val currentTime = System.currentTimeMillis()
                if (currentTime - mLastEventTime < 10000) {
                    val timeDelta: Long = currentTime - mLastEventTime
                    mSnoreTime += timeDelta
                    mContSnoringTime += timeDelta
                    if (mContSnoringTime > 10000) {
                        if (::callback.isInitialized) {
                            callback.invoke()
                        }
//                        callVibrationNotifications()
                    }
                } else {
                    mContSnoringTime = 0
                }
                mLastEventTime = currentTime
                mSnoreTime += inferenceTime ?: 0
            }
        }
    }
    fun getSnoreTime() :Long {
        return mSnoreTime
    }
    fun  setCallVibrationNotifications(callback: (() -> Unit)){
        this.callback = callback
    }
}