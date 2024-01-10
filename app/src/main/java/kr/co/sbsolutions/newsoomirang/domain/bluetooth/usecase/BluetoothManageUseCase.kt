package kr.co.sbsolutions.withsoom.domain.bluetooth.usecase

import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.domain.bluetooth.repository.IBluetoothManageRepository
import javax.inject.Inject

class BluetoothManageUseCase @Inject constructor(private val bluetoothManageRepository: IBluetoothManageRepository){
    suspend fun registerSBSensor(key: SBBluetoothDevice, name: String, address: String) : Boolean {
        return bluetoothManageRepository.registerSBSensor(key, name, address)
    }

    suspend fun unregisterSBSensor(key: SBBluetoothDevice) : Boolean {
        return bluetoothManageRepository.unregisterSBSensor(key)
    }
}