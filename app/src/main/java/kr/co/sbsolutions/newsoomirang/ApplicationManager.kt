package kr.co.sbsolutions.newsoomirang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApplicationManager : Application() {

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

    }

    private fun createNotificationChannel(){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //android api26 이상 부터는 channel을 생성과 중요도 설정을 해야한다.
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        channel.enableLights(true)
        channel.enableVibration(true)

        notificationManager.createNotificationChannel(channel)
    }
}