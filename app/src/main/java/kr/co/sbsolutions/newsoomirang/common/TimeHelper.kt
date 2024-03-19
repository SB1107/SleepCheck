package kr.co.sbsolutions.newsoomirang.common

import androidx.compose.ui.util.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TimeHelper {
    lateinit var timerJob: Job
    private var time: Int = 0
    private val _measuringTimer: MutableSharedFlow<Triple<Int, Int, Int>> = MutableSharedFlow()
    val measuringTimer: SharedFlow<Triple<Int, Int, Int>> = _measuringTimer
    private var  pauseTimer : Boolean = false
    fun startTimer(scope: CoroutineScope) {
        time = 0
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
        runBlocking {
            _measuringTimer.emit(Triple(0, 0, 0))
        }
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (pauseTimer.not()) {
                    time += 1
                }
                val hour = time / 3600
                val minute = time % 3600 / 60
                val second = time % 60
                _measuringTimer.emit(Triple(hour, minute, second))
            }
        }
    }

    private fun timerJobCancel() {
        if (::timerJob.isInitialized) {
            timerJob.cancel()
        }
    }
    fun resetTimer() {
        if (pauseTimer) {
            pauseTimer = false
        }
    }
    fun pauseTimer() {
        this.pauseTimer = true
    }

    fun getTime(): Int {
        return time
    }

    fun stopTimer() {
        time = 0
        timerJobCancel()
    }
}