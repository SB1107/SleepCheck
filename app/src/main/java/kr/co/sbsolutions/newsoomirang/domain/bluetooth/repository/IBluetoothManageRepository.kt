package kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice

interface IBluetoothManageRepository {
    suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean
    suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean
    suspend fun  getBluetoothDeviceName(key: SBBluetoothDevice): Flow<String?>

}