package kr.co.sbsolutions.newsoomirang.presenter

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.BLEService.Companion.DATA_ID
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import java.lang.ref.WeakReference


abstract class BaseServiceActivity : BaseActivity() {
    private lateinit var service: WeakReference<BLEService>

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, BLEService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    protected fun startSBService(am: ActionMessage, dataId: Int? = null) {
        Intent(this, BLEService::class.java).apply {
            action = am.msg
            dataId?.let { putExtra(DATA_ID, it) }
            service.get()?.startForegroundService(this)
        }
    }

    @CallSuper
    fun onServiceAvailable() {

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    service.get()?.let {
                        it.sbSensorInfo.collectLatest { info ->
                            if (info.bluetoothState == BluetoothState.Registered) {
                                service.get()?.connectDevice(info)
                            }
                            changeServiceViewModel()?.onChangeSBSensorInfo(info)
                        }
                    }
                }
            }
            launch {
                service.get()?.let {
                    it.spo2SensorInfo.collectLatest { info ->
                        changeServiceViewModel()?.onChangeSpO2SensorInfo(info)
                    }
                }
            }
            launch {
                service.get()?.let {
                    it.eegSensorInfo.collectLatest { info ->
                        changeServiceViewModel()?.onChangeEEGSensorInfo(info)
                    }
                }
            }
        }
    }

    private fun changeServiceViewModel(): BaseServiceViewModel? {
        return if(getViewModel() is BaseServiceViewModel){
            (getViewModel() as BaseServiceViewModel)
        }else{
            null
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = WeakReference((binder as BLEService.LocalBinder).service)
            service.get()?.let {
                changeServiceViewModel()?.setService(it)
            }
            onServiceAvailable()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service.clear()
        }
    }

}