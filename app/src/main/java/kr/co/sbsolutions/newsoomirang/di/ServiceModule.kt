package kr.co.sbsolutions.newsoomirang.di

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_ID
import kr.co.sbsolutions.newsoomirang.presenter.splash.SplashActivity
import kr.co.sbsolutions.withsoom.data.repository.bluetooth.BluetoothNetworkRepository
import kr.co.sbsolutions.withsoom.domain.bluetooth.repository.IBluetoothNetworkRepository

@Module
@InstallIn(ServiceComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun bindBluetoothNetworkRepository(bluetoothNetworkRepository: BluetoothNetworkRepository) : IBluetoothNetworkRepository
//    @Binds
//    abstract fun bindApneaUploadRepository(apneaUploadRepository: ApneaUploadRepository) : IApneaUploadRepository

}
@Module
@InstallIn(ServiceComponent::class)
class ServiceModuleNotification {
    @ServiceScoped
    @Provides
    fun provideMainActivityPendingIntent(
        @ApplicationContext context: Context
    ) = PendingIntent.getActivity(
        context, NOTIFICATION_ID, Intent(context, SplashActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    @ServiceScoped
    @Provides
    fun provideNotificationBuilder(
        @ApplicationContext context: Context, pendingIntent: PendingIntent
    ) = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).setAutoCancel(false).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher) // TODO: Notification 아이콘 작업
        .setContentTitle("숨이랑 기기 연결 대기 중").setContentIntent(pendingIntent)

    @ServiceScoped
    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}