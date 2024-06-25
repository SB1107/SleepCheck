package kr.co.sbsolutions.sleepcheck.service

import kotlinx.coroutines.Job
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.BluetoothState



interface ILogHelper {

    fun insertLog(logMethod: () -> Unit)

    fun insertLog(message: String)

    fun insertLog(state: BluetoothState)

    fun registerJob(job: Job, method: () -> Unit)

    fun registerJob(tag: String, job: Job)
}

interface INoseRingHelper {

    fun getSnoreTime(): Long
    fun getSnoreCount(): Int
    fun getCoughCount(): Int
    fun stopAudioClassification()
    fun snoreCountIncrease()
}
