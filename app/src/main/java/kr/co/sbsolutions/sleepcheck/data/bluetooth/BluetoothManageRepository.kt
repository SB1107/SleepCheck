package kr.co.sbsolutions.sleepcheck.data.bluetooth

import android.annotation.SuppressLint
import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothManageRepository
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BluetoothManageRepository @Inject constructor(private val dataManager: DataManager) : IBluetoothManageRepository {
    override suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean{
        return dataManager.saveBluetoothDevice(key.type.name, name, address)
    }

    override suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean {
        return dataManager.deleteBluetoothDevice(key.type.name)
    }
    override suspend fun getBluetoothDeviceName(key: SBBluetoothDevice): Flow<String?> {
        return  dataManager.getBluetoothDeviceName(key.type.name)
    }
}