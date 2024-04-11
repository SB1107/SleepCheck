package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import kotlinx.coroutines.Job
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState

class LogHelper(private val logWorkerHelper: LogWorkerHelper, private val coroutineScopeHandler: CoroutineScopeHandler) {

    fun insertLog(logMethod: () -> Unit) {
        val name = getClazzName(logMethod)
        logWorkerHelper.insertLog("Method : $name ")
    }

    fun insertLog(message: String) {
        var tempData = message
        if (tempData.contains("null")) {
            tempData = message.replace("null", "").replace("", "").trim()
        }
        logWorkerHelper.insertLog(tempData)
        Log.e(TAG, tempData)
    }

    fun insertLog(state: BluetoothState) {
        logWorkerHelper.insertLog(state.toString())
        Log.e(TAG, "insertLog: $state")
    }

    fun registerJob(job: Job, method: () -> Unit) {
        val tag = getClazzName(method)
        insertLog("registerJob = $tag")
        return coroutineScopeHandler.registerJob(job, tag)
    }

    fun registerJob(tag: String, job: Job) {
        insertLog("registerJob = $tag")
        return coroutineScopeHandler.registerJob(job, tag)
    }

    private fun getClazzName(request: () -> Unit): String {
        val regex = Regex("[\\d\\p{Punct}$]")
        val clazzName = regex.replace(request.javaClass.name.split(".").last(), "")

        return if (clazzName.contains("ViewModel")) {
            val index = clazzName.indexOf("ViewModel")
            clazzName.substring(index + "ViewModel".length)
        } else {
            clazzName
        }
    }
}