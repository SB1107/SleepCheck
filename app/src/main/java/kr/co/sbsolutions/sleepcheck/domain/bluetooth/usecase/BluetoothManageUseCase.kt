package kr.co.sbsolutions.sleepcheck.domain.bluetooth.usecase

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothManageRepository
import javax.inject.Inject

class BluetoothManageUseCase @Inject constructor(private val bluetoothManageRepository: IBluetoothManageRepository){
    suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean {
        return bluetoothManageRepository.registerSBSensor(key, name, address)
    }

    suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean {
        return bluetoothManageRepository.unregisterSBSensor(key)
    }
    suspend fun getBluetoothDeviceName(key: SBBluetoothDevice) : Flow<String?> {
        return bluetoothManageRepository.getBluetoothDeviceName(key)
    }
}