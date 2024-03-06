package kr.co.sbsolutions.newsoomirang.di

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.NOTIFICATION_CHANNEL_ID
import kr.co.sbsolutions.newsoomirang.common.NoseRingHelper
import kr.co.sbsolutions.newsoomirang.common.TimeHelper

//@Module
//@InstallIn(ServiceComponent::class)
//abstract class ServiceModule {
//    @Singleton
//    @Binds
//    abstract fun bindBluetoothNetworkRepository(bluetoothNetworkRepository: BluetoothNetworkRepository): IBluetoothNetworkRepository
//
//    //    @Binds
////    abstract fun bindApneaUploadRepository(apneaUploadRepository: ApneaUploadRepository) : IApneaUploadRepository
////    @Binds
////    abstract fun provideRemotePolicyDataSource(policyRepository: AuthAPIRepository): RemoteAuthDataSource
//
//}

@Module
@InstallIn(ServiceComponent::class)
class ServiceModuleNotification {

    @ServiceScoped
    @Provides
    fun provideNotificationBuilder(
        @ApplicationContext context: Context
    ) = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID).setAutoCancel(true).setOngoing(true).setSmallIcon(R.mipmap.ic_launcher) // TODO: Notification 아이콘 작업
        .setContentTitle("숨이랑 기기 연결 대기 중").setPriority(NotificationCompat.PRIORITY_HIGH)


    @ServiceScoped
    @Provides
    fun provideNotificationManager(@ApplicationContext context: Context) = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @ServiceScoped
    @Provides
    fun provideTimeHelperManager() = TimeHelper()

    @ServiceScoped
    @Provides
    fun provideNoseRingManager() = NoseRingHelper()

}