package kr.co.sbsolutions.newsoomirang.data.bluetooth

import android.annotation.SuppressLint
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothManageRepository
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BluetoothManageRepository @Inject constructor(private val dataManager: DataManager) : IBluetoothManageRepository {
    override suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean{
        return dataManager.saveBluetoothDevice(key.type.name, name, address)
    }

    override suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean {
        return dataManager.deleteBluetoothDevice(key.type.name)
    }
}