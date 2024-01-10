package kr.co.sbsolutions.newsoomirang.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import kr.co.sbsolutions.withsoom.data.repository.bluetooth.BluetoothNetworkRepository
import kr.co.sbsolutions.withsoom.domain.bluetooth.repository.IBluetoothNetworkRepository

@Module
@InstallIn(ServiceComponent::class)
abstract class ServiceModule {
    @Binds
    abstract fun bindBluetoothNetworkRepository(bluetoothNetworkRepository: BluetoothNetworkRepository) : IBluetoothNetworkRepository
}