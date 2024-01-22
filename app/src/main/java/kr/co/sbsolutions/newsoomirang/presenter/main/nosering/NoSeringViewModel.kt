package kr.co.sbsolutions.newsoomirang.presenter.main.nosering

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.MeasuringState
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import javax.inject.Inject

@HiltViewModel
class NoSeringViewModel  @Inject constructor(
    private val dataManager: DataManager,
    private val authAPIRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager) {

    private val _showMeasurementAlert: MutableSharedFlow<Boolean> = MutableSharedFlow()
    val showMeasurementAlert: SharedFlow<Boolean> = _showMeasurementAlert
    private val _measuringState: MutableSharedFlow<MeasuringState> = MutableSharedFlow()
    val measuringState: SharedFlow<MeasuringState> = _measuringState
    override fun onChangeSBSensorInfo(info: BluetoothInfo) {

    }
}