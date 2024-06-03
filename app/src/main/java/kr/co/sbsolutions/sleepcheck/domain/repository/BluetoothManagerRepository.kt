package kr.co.sbsolutions.sleepcheck.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothManageRepository
import javax.inject.Inject

class BluetoothManagerRepository @Inject constructor(private val bluetoothManageRepository: IBluetoothManageRepository): IBluetoothManageRepository {
    override suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String): Boolean {
        return  bluetoothManageRepository.registerSBSensor(key, name, address)
    }

    override suspend fun unregisterSBSensor(key: SBBluetoothDevice): Boolean {
        return bluetoothManageRepository.unregisterSBSensor(key)
    }

    override suspend fun getBluetoothDeviceName(key: SBBluetoothDevice): Flow<String?> {
        return  bluetoothManageRepository.getBluetoothDeviceName(key)
    }


}