package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import kotlinx.coroutines.Job
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG

class CoroutineScopeHandler {
    companion object {
        private val viewModelScopeHandler: MutableMap<String, Job> = HashMap()
    }

    fun registerJob(job: Job, getClazzName : String) {
        if (viewModelScopeHandler.containsKey(getClazzName)) {
            return
        }
        viewModelScopeHandler[getClazzName] = job
        job.invokeOnCompletion {
            Log.e(TAG, "Job Done OR Cancel Method -> $getClazzName")
            viewModelScopeHandler.remove(getClazzName)
        }
    }
}