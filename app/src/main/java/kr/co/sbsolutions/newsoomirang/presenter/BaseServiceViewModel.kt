package kr.co.sbsolutions.newsoomirang.presenter

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BLEService
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import java.lang.ref.WeakReference

abstract class BaseServiceViewModel : BaseViewModel() {
    private lateinit var service: WeakReference<BLEService>
    abstract fun onChangeSBSensorInfo(info: BluetoothInfo)
    open fun onChangeSpO2SensorInfo(info: BluetoothInfo) {}
    open fun onChangeEEGSensorInfo(info: BluetoothInfo) {}
    fun setService(service: BLEService) {
        this.service = WeakReference(service)
    }
    fun getService() : BLEService?{
        return if (::service.isInitialized) {
            this.service.get()
        }else {
            null
        }

    }
}