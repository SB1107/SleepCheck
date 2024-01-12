package kr.co.sbsolutions.newsoomirang.presenter

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.MainActivity
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.withsoom.utils.TokenManager
import javax.inject.Inject

@AndroidEntryPoint
class FCMPushService : FirebaseMessagingService(), LifecycleOwner {

    @Inject
    lateinit var tokenManager: TokenManager

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
            Log.d(TAG, "[FMS] UpdateFcm / MyFirebaseMessagingService: $token")
        }
        Log.d(TAG, "[FMS] Token created: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        //받은 remoteMessage의 값 출력해보기.   데이터메세지 / 알림 메세지
//        Log.d(TAG, "[FMS] Data / ${remoteMessage.data[DATA_KEY]}")
//        Log.d(TAG, "[FMS] Noti / title: ${remoteMessage.notification?.title} + body: ${remoteMessage.notification?.body}")

        Intent("ACTION_SEND_DATA").apply {
            putExtra(DATA_KEY, remoteMessage.data["dataId"])
            sendBroadcast(this)
        }
        sendDataMessage("측정이 완료되었어요.", "측정이 완료되었어요", "0")

        /*//알림 메세지의 경우
        remoteMessage.notification?.let {
            if (remoteMessage.data.isNotEmpty()) {
                remoteMessage.data[DATA_KEY]?.let { data -> sendDataMessage(it.title.toString(), it.body.toString(), data) }
                Log.d(TAG, "[FMS] remoteMessage.data: ${remoteMessage.data["dataId"]}")
            }else{
                sendDataMessage(it.title.toString(), it.body.toString(), "0")
            }
        }*/


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

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_NEW_TASK
        }
        intent.putExtra(DATA_KEY, data)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuild = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setAutoCancel(true)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify((System.currentTimeMillis()/1000).toInt(), notificationBuild.build())
    }

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle
}