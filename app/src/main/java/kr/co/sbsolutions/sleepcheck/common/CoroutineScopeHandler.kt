package kr.co.sbsolutions.sleepcheck.common

import android.util.Log
import kotlinx.coroutines.Job
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import java.util.concurrent.CancellationException

class CoroutineScopeHandler {
    companion object {
        private val viewModelScopeHandler: MutableMap<String, Job> = HashMap()
    }

    fun registerJob(job: Job, getClazzName : String, logWorkerHelper: LogWorkerHelper) {
        if (viewModelScopeHandler.containsKey(getClazzName)) {
            viewModelScopeHandler[getClazzName]?.cancel(CancellationException("중복 호출로 기존 것 취소"))
            logWorkerHelper.insertLog("중복 호출 기존 잡  캔슬")
        }

        viewModelScopeHandler[getClazzName] = job
        job.invokeOnCompletion {
            Log.e(TAG, "Job Done OR Cancel Method -> $getClazzName")
            viewModelScopeHandler.remove(getClazzName)
        }
    }
}