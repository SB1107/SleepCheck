package kr.co.sbsolutions.newsoomirang.presenter

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.BLEService.Companion.DATA_ID


abstract class BaseServiceActivity : BaseActivity() {
    private var service: BLEService? = null

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
            service?.startForegroundService(this)
        }
    }

    @CallSuper
    fun onServiceAvailable() {

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    service?.let {
                        it.sbSensorInfo.collectLatest { info ->
                            changeServiceViewModel()?.onChangeSBSensorInfo(info)
                        }
                    }
                }
            }
            launch {
                service?.let {
                    it.spo2SensorInfo.collectLatest { info ->
                        changeServiceViewModel()?.onChangeSpO2SensorInfo(info)
                    }
                }
            }
            launch {
                service?.let {
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
            service = (binder as BLEService.LocalBinder).service
            service?.let {
                changeServiceViewModel()?.setService(it)
            }
            onServiceAvailable()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
        }
    }

}