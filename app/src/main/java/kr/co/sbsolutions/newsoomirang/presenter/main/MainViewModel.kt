package kr.co.sbsolutions.newsoomirang.presenter.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.SBBluetoothDevice
import kr.co.sbsolutions.withsoom.utils.TokenManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(private  val dataManager: DataManager, private val tokenManager: TokenManager) : BaseServiceViewModel(dataManager, tokenManager) {
    private val _breathingResults: MutableSharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 1)
    private val _noSeringResults: MutableSharedFlow<Int> = MutableSharedFlow(extraBufferCapacity = 1)
    val breathingResults: SharedFlow<Int> = _breathingResults
    val noSeringResults: SharedFlow<Int> = _noSeringResults

    fun  sendMeasurementResults(){
        viewModelScope.launch {
                if (ApplicationManager.getBluetoothInfo().sleepType == SleepType.Breathing) {
                    Log.d(TAG, "RESULT: ${ApplicationManager.getBluetoothInfo().sleepType} ")
                    _breathingResults.emit(0)
                }else{
                    Log.d(TAG, "RESULT: ${ApplicationManager.getBluetoothInfo().sleepType} ")
                    _noSeringResults.emit(0)
                }
        }
    }

}

enum class ServiceCommend {
    START,STOP
}