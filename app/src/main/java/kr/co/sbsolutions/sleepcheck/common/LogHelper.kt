package kr.co.sbsolutions.sleepcheck.common

import android.util.Log
import kotlinx.coroutines.Job
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.sleepcheck.service.ILogHelper

class LogHelper(private val logWorkerHelper: LogWorkerHelper, private val coroutineScopeHandler: CoroutineScopeHandler)  :
    ILogHelper {

    override fun insertLog(logMethod: () -> Unit) {
        val name = getClazzName(logMethod)
        logWorkerHelper.insertLog("Method : $name ")
    }

    override fun insertLog(message: String) {
        var tempData = message
        if (tempData.contains("null")) {
            tempData = message.replace("null", "").replace("", "").trim()
        }
        logWorkerHelper.insertLog(tempData)
        Log.e(TAG, tempData)
    }

    override fun insertLog(state: BluetoothState) {
        logWorkerHelper.insertLog(state.toString())
        Log.e(TAG, "insertLog: $state")
    }

    override fun registerJob(job: Job, method: () -> Unit) {
        val tag = getClazzName(method)
        insertLog("registerJob = $tag")
        return coroutineScopeHandler.registerJob(job, tag,logWorkerHelper)
    }

    override fun registerJob(tag: String, job: Job) {
        insertLog("registerJob = $tag")
        return coroutineScopeHandler.registerJob(job, tag, logWorkerHelper)
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