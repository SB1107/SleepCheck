package kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository

import kr.co.sbsolutions.newsoomirang.domain.bluetooth.entity.SBBluetoothDevice

interface IBluetoothManageRepository {
    suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean
    suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean
}