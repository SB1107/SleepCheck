package kr.co.sbsolutions.newsoomirang

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.NetworkUtil
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import java.lang.ref.WeakReference

@HiltAndroidApp
class ApplicationManager : Application() {
    private val _bluetoothInfoFlow: MutableStateFlow<BluetoothInfo> = MutableStateFlow(BluetoothInfo(SBBluetoothDevice.SB_SOOM_SENSOR))
    private val bluetoothInfoFlow: StateFlow<BluetoothInfo> = _bluetoothInfoFlow
//    private  val _service : MutableStateFlow<WeakReference<BLEService>> = MutableStateFlow(WeakReference(null))
//    private  val service : StateFlow<WeakReference<BLEService>> = _service
    private  val _networkCheck : MutableStateFlow<Boolean> = MutableStateFlow(false)
    private  val networkCheck : StateFlow<Boolean> = _networkCheck
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
//        fun setService(service : WeakReference<BLEService>){
//            instance._service.tryEmit(service)
//        }
//        fun serviceClear(){
//            instance._service.value.clear()
//        }
//        fun getService() :  StateFlow<WeakReference<BLEService>>{
//            return StateFlow<WeakReference<BLEService>>()
//        }
        fun getNetworkCheck() : Boolean {
            return instance.networkCheck.value
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        NetworkUtil.registerNetworkCallback(getSystemService(ConnectivityManager::class.java), networkCallback)
    }
    // 네트워크 체크를 위한 Callback 함수
    private val networkCallback: ConnectivityManager.NetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
//            logger.system.info("${NetworkUtil.getTransportName(getSystemService(ConnectivityManager::class.java))} network is available.")
            _networkCheck.tryEmit(true)
        }

        override fun onLost(network: Network) {
//            logger.system.info("Network is lost.")
            _networkCheck.tryEmit(false)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            // logger.system.info("Default Network Capabilities has changed...")
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            // logger.system.info("Default Network LinkProperties has changed...")
        }
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