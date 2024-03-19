package kr.co.sbsolutions.newsoomirang.presenter

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.BLEService.Companion.DATA_ID
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.BluetoothState
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import java.lang.ref.WeakReference


abstract class BaseServiceActivity : BluetoothActivity() {
    private lateinit var service: WeakReference<BLEService>

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, BLEService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
    }

    override fun onResume() {
        super.onResume()
    }

    protected fun startSBService(am: ActionMessage, dataId: Int? = null) {
        Intent(this, BLEService::class.java).apply {
            action = am.msg
            dataId?.let { putExtra(DATA_ID, it) }
            baseContext.startForegroundService(this)
        }
    }

    @CallSuper
    fun onServiceAvailable() {
        lifecycleScope.launch {
            launch {
                service.get()?.let {
                    it.sbSensorInfo.collectLatest { info ->
                        if (info.bluetoothState == BluetoothState.Registered) {
                            service.get()?.connectDevice(info)
                        } else if (info.bluetoothState == BluetoothState.Connected.Finish) {
                            when(info.cancelCheck){
                                true -> {
                                    changeServiceViewModel()?.setCommend(ServiceCommend.CANCEL)
                                    Log.d(TAG, "onServiceAvailable!!!!!!: true")
                                }
                                false -> {
                                    changeServiceViewModel()?.setCommend(ServiceCommend.STOP)
                                    Log.d(TAG, "onServiceAvailable!!!!!!: false")
                                }
                            }


                        }
                        ApplicationManager.setBluetoothInfo(info)
                    }
                }
            }
            launch {
                service.get()?.let {
                    it.spo2SensorInfo.collectLatest { info ->
            //                        changeServiceViewModel()?.onChangeSpO2SensorInfo(info)
                    }
                }
            }
            launch {
                service.get()?.let {
                    it.eegSensorInfo.collectLatest { info ->
//                        changeServiceViewModel()?.onChangeEEGSensorInfo(info)
                    }
                }
            }
        }
    }

    private fun changeServiceViewModel(): BaseServiceViewModel? {
        return if (getViewModel() is BaseServiceViewModel) {
            (getViewModel() as BaseServiceViewModel)
        } else {
            null
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = WeakReference((binder as BLEService.LocalBinder).service)
            ApplicationManager.setService(service)
            onServiceAvailable()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service.clear()
            ApplicationManager.serviceClear()
        }
    }
}