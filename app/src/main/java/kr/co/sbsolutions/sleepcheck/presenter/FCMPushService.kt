package kr.co.sbsolutions.sleepcheck.presenter

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.Cons
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.service.ILogHelper
import javax.inject.Inject

@AndroidEntryPoint
class FCMPushService : FirebaseMessagingService(), LifecycleOwner {

    @Inject
    lateinit var tokenManager: TokenManager
    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder
    @Inject
    lateinit var notificationManager: NotificationManager
    @Inject
    lateinit var logHelper: ILogHelper


    companion object {
        const val DATA_KEY = "dataId"
        const val TOKEN_ID = "tokenId"
    }

    // 등록 토큰이 앱 데이터 삭제, 재설치, 복원 등의 상황에서 변경 가능.
    // 때문에 앱에서 토큰이 갱신 될 경우 서버에 해당 토큰을 갱신됐다고 알려주는 콜백함수.
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        lifecycleScope.launch(Dispatchers.IO) {
            tokenManager.saveFcmToken(token)
            tokenManager.setDifferentValue()
            logHelper.insertLog("[FMS] UpdateFcm  FCMPushService: $token")
            Log.d(TAG, "[FMS] UpdateFcm  MyFirebaseMessagingService: $token")
        }
        Log.d(TAG, "[FMS] Token created: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)


        //받은 remoteMessage의 값 출력해보기.   데이터메세지 / 알림 메세지

        logHelper.insertLog("[FMS] Data  ${remoteMessage.data[DATA_KEY]}")
        logHelper.insertLog("[FMS] Noti  title: ${remoteMessage.notification?.title} + body: ${remoteMessage.notification?.body}")

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        @SuppressLint("InvalidWakeLockTag") val wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG")
        wakeLock.acquire(3000)
        sendDataMessage("측정이 완료되었어요.", "측정이 완료되었어요", "0")
    }

    // 등록된 토큰 확인
    fun initFirebase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {

            if (!it.isSuccessful) {
                // 토큰 요청 task가 실패 한 경우 처리
                Log.d(TAG, "initFirebase: failed", it.exception)
                return@addOnCompleteListener
            }
            // 토큰 요청 task가 성공한 경우 task의 result에 token 값이 내려온다.
            val token = it.result
            Log.d(TAG, "initFirebase: $token")
        }
    }


    private fun sendDataMessage(title: String, message: String, data: String) {
        val app = packageManager.getLaunchIntentForPackage(baseContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, app,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
        notificationBuilder.setContentIntent(pendingIntent)
        notificationBuilder.setContentTitle(title )
        notificationBuilder.setContentText(message)
        notificationBuilder.setAutoCancel(true)

        Intent().also { intent ->
            intent.setAction(Cons.NOTIFICATION_ACTION)
            intent.setPackage(baseContext.packageName)
            sendBroadcast(intent)
        }
        val channel = NotificationChannel(
            Cons.NOTIFICATION_CHANNEL_ID, Cons.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify((System.currentTimeMillis()/1000).toInt(), notificationBuilder.build())

    }

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle
}