package kr.co.sbsolutions.newsoomirang.presenter

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.newsoomirang.BLEService.Companion.DATA_ID
import kr.co.sbsolutions.newsoomirang.domain.repository.BleRepository
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import javax.inject.Inject


abstract class BaseServiceActivity : BaseActivity() {
    @Inject
    lateinit var bleRepository: BleRepository
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        lifecycleScope.launch(Dispatchers.IO){
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    BLEService.sbSensorInfo.collectLatest {
                        onChangeSBSensorInfo(it)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isMyServiceRunning()) {
            Intent(this@BaseServiceActivity ,BLEService::class.java).also {
                if(!isMyServiceRunning() || !BLEService.isServiceStarted){
                    it.action = ActionMessage.StartSBService.msg
                    startForegroundService(it)
                    Log.e("Adfsdf","Asdfsdf")
                }
            }
        }
    }
    protected fun startSBService(am: ActionMessage, dataId: Int? = null) {
        Intent(this, BLEService::class.java).apply {
            action = am.msg
            dataId?.let { putExtra(DATA_ID, it) }
            bleRepository.getBleService()?.startForegroundService(this)
        }
    }

    open fun onChangeSBSensorInfo(info: BluetoothInfo) {}
    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}


}