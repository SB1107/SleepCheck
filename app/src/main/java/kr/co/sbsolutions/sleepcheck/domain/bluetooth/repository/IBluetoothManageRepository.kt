package kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice

interface IBluetoothManageRepository {
    suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean
    suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean
    suspend fun  getBluetoothDeviceName(key: SBBluetoothDevice): Flow<String?>

}