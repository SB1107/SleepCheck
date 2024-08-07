package kr.co.sbsolutions.sleepcheck.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.co.sbsolutions.sleepcheck.service.ILogHelper

class TimeHelper(private val logHelper: ILogHelper) {
    lateinit var timerJob: Job
    private var time: Int = 0
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer
    fun startTimer(scope: CoroutineScope, startTime: Long) {
        logHelper.insertLog("TimeHelper = startTimer")
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
        runBlocking {
            _measuringTimer.emit(Triple(0, 0, 0))
        }
        timerJob = scope.launch {
            logHelper.insertLog("TimeHelper = launch")
            while (true) {
                delay(1000)
                time += 1
//                val hour = time / 3600
//                val minute = time % 3600 / 60
//                val second = time % 60
//                _measuringTimer.emit(Triple(hour, minute, second))
                val currentTimeMillis = System.currentTimeMillis()
                val diffTime = currentTimeMillis - startTime
                val seconds = (diffTime / 1000) % 60
                val minutes = (diffTime / (1000 * 60)) % 60
                val hours = (diffTime / (1000 * 60 * 60))
                _measuringTimer.emit(Triple(hours.toInt(), minutes.toInt(), seconds.toInt()))
            }
        }
    }

    private fun timerJobCancel() {
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
    }

    fun getTime(): Int {
        return time
    }

    fun setTime(time: Int) {
        this.time = time
    }
    fun clearTime(){
        time = 0
        runBlocking {
            _measuringTimer.emit(Triple(0, 0, 0))
        }
    }

    fun stopTimer() {
        time = 0
        timerJobCancel()
    }
}