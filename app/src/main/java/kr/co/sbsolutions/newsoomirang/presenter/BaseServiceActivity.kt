package kr.co.sbsolutions.newsoomirang.presenter

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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


    override fun onStart() {
        super.onStart()
        if (!isMyServiceRunning()) {
            startSBService(ActionMessage.StartSBService)
        }

        lifecycleScope.launch {
            BLEService.sbSensorInfo.collect {
                onChangeSBSensorInfo(it)
            }
        }

    }

    protected fun startSBService(am: ActionMessage) {
        Intent(this@BaseServiceActivity, BLEService::class.java).also {
            it.action = am.msg
            startForegroundService(it)
        }
    }

    open fun onChangeSBSensorInfo(info: BluetoothInfo) {}
    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}


}