package kr.co.sbsolutions.withsoom.domain.bluetooth.repository

import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice

interface IBluetoothManageRepository {
    suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean
    suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean
}