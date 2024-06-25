package kr.co.sbsolutions.sleepcheck.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons
import kr.co.sbsolutions.sleepcheck.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.LogHelper
import kr.co.sbsolutions.sleepcheck.common.NoseRingHelper
import kr.co.sbsolutions.sleepcheck.common.TimeHelper
import kr.co.sbsolutions.sleepcheck.service.BLEService.Companion.FOREGROUND_SERVICE_NOTIFICATION_ID
import java.util.Locale

class TimeCountUseCase(
    private val lifecycleScope: LifecycleCoroutineScope,
    private val timeHelper: TimeHelper,
    private val dataManager: DataManager,
    private val notificationBuilder: NotificationCompat.Builder,
    private val notificationManager: NotificationManager,
    private val noseRingHelper: NoseRingHelper,
    private  val logHelper: ILogHelper
) {

    fun listenTimer() {
        lifecycleScope.launch(IO) {
            timeHelper.measuringTimer.collectLatest {
                notificationBuilder.setContentText(String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third))
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
                    notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, notificationBuilder.build())
                }
                dataManager.setTimer(timeHelper.getTime())
                dataManager.setNoseRingTimer(noseRingHelper.getSnoreTime())
                dataManager.setCoughCount(noseRingHelper.getCoughCount())
                dataManager.setNoseRingCount(noseRingHelper.getSnoreCount())
            }
        }
    }
    private fun setTimer(){
        lifecycleScope.launch {
            dataManager.getTimer().first()?.let {
                logHelper.insertLog("setTime = $it")
                val starTime = dataManager.getStartTime().first()
                val nowTime =  System.currentTimeMillis()
                val tempResultTime =  nowTime  - starTime
                val resultSecond = tempResultTime / 1000
                logHelper.insertLog("resultSecond = $resultSecond")
                timeHelper.setTime(resultSecond.toInt())
            }
        }
    }
    fun setTimeAndStart(){
        logHelper.insertLog("setTimeAndStart")
        setTimer()
        startTimer()
    }

    fun startTimer() {
        logHelper.insertLog("startTimer")
        notVibrationNotifyChannelCreate()
        lifecycleScope.launch { timeHelper.startTimer(this) }
    }

    fun stopTimer() {
        timeHelper.stopTimer()
    }

    fun setContentTitle(message: String) {
        notificationBuilder.setContentTitle(message)
    }

    fun setContentIntent(pendingIntent: PendingIntent) {
        notificationBuilder.setContentIntent(pendingIntent)
    }

    private fun notVibrationNotifyChannelCreate() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun getTimeHelper(): SharedFlow<Triple<Int, Int, Int>> {
        return timeHelper.measuringTimer
    }

    fun getTime(): Int {
    return  timeHelper.getTime()
    }
    fun getNotificationBuilder() : NotificationCompat.Builder{
        return  notificationBuilder
    }
    fun getNotificationManager() : NotificationManager{
        return  notificationManager
    }
}