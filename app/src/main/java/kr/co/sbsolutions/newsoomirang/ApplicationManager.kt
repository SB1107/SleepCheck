package kr.co.sbsolutions.newsoomirang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import java.lang.ref.WeakReference

@HiltAndroidApp
class ApplicationManager : Application() {
    private val _bluetoothInfoFlow: MutableStateFlow<BluetoothInfo> = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR))
    private val bluetoothInfoFlow: StateFlow<BluetoothInfo> = _bluetoothInfoFlow
    private  val _service : MutableStateFlow<WeakReference<BLEService>> = MutableStateFlow(WeakReference(null))
    private  val service : StateFlow<WeakReference<BLEService>> = _service
    init {
        instance = this
    }

    companion object {
        lateinit var instance: ApplicationManager
        fun getBluetoothInfo(): BluetoothInfo {
            return instance.bluetoothInfoFlow.value
        }
        fun getBluetoothInfoFlow(): StateFlow<BluetoothInfo> {
            return instance.bluetoothInfoFlow
        }
        fun setBluetoothInfo(info: BluetoothInfo) {
            instance._bluetoothInfoFlow.tryEmit(info)
        }
        fun setService(service : WeakReference<BLEService>){
            instance._service.tryEmit(service)
        }
        fun serviceClear(){
            instance._service.tryEmit(WeakReference(null))
        }
        fun getService() :  StateFlow<WeakReference<BLEService>>{
            return instance.service
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
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