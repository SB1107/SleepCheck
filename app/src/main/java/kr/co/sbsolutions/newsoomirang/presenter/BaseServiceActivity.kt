package kr.co.sbsolutions.newsoomirang.presenter

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.BLEService.Companion.DATA_ID
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothState
import java.lang.ref.WeakReference
import javax.inject.Inject


abstract class BaseServiceActivity : BaseActivity() {
    private lateinit var service: WeakReference<BLEService>
    private lateinit var bluetoothActivityResultLauncher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter
    private  var bind : Boolean  = false
    override fun onStart() {
        super.onStart()
        if (ApplicationManager.getService().value.get() == null) {
            bindService(Intent(this, BLEService::class.java), serviceConnection, BIND_AUTO_CREATE)
        } else {
            service = ApplicationManager.getService().value
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBluetoothActive()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bind) {
            unbindService(serviceConnection)
        }

    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            showAlertDialogWithCancel(R.string.common_title, "블루투스를 활성화가 필요합니다. \n활성화 하시겠습니까?",
                confirmButtonText = R.string.setting_bluetooth_connect,
                confirmAction = {
                    requestBluetoothActivation()
                },
                cancelAction = {
                    finish()
                }
            )
        }
    }

    protected fun startSBService(am: ActionMessage, dataId: Int? = null) {
        Intent(this, BLEService::class.java).apply {
            action = am.msg
            dataId?.let { putExtra(DATA_ID, it) }
            ApplicationManager.getService().value.get()?.startForegroundService(this)
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
                            changeServiceViewModel()?.setCommend(ServiceCommend.STOP)
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
            service = WeakReference((binder as BLEService.LocalBinder).getService())
            ApplicationManager.setService(service)
            onServiceAvailable()
            bind = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ApplicationManager.serviceClear()
            bind = false
        }
    }

    private fun onBluetoothActive() {
        bluetoothActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 사용자가 블루투스를 활성화했을 때의 처리

            } else {
                // 사용자가 블루투스를 활성화하지 않았을 때의 처리
                finish()
            }
        }
//        requestBluetoothActivation()
    }

    private fun requestBluetoothActivation() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothActivityResultLauncher.launch(enableBluetoothIntent)
    }

}