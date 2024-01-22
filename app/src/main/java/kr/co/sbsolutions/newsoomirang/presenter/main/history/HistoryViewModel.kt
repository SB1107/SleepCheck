package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.util.Log
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.data.model.SleepModel
import kr.co.sbsolutions.newsoomirang.data.server.ApiHelper
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import kr.co.sbsolutions.withsoom.domain.bluetooth.entity.BluetoothInfo
import okhttp3.ResponseBody
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val authDataSource: RemoteAuthDataSource
) : BaseServiceViewModel() {
    private val _sleepWeekData: MutableSharedFlow<SleepModel> = MutableSharedFlow()
    val sleepWeekData: SharedFlow<SleepModel> = _sleepWeekData

    init {
        getWeekSleepData()
    }

    override fun onChangeSBSensorInfo(info: BluetoothInfo) {
        Log.d(TAG, "[HVM] : $info ")
    }

    private fun getWeekSleepData() {
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.getWeek() }
                .collectLatest {
//                    getDetailSleepData(it)
                    Log.d(TAG, "getWeekSleepData: $it")
//                    test(it)
                }
        }
    }

    private fun getDetailSleepData(sleepModel: SleepModel) {
        viewModelScope.launch {
            request { authDataSource.sleepDataDetail(sleepModel) }
                .collectLatest { sleepData ->
                    Log.d(TAG, "getWeekSleepData: $sleepData")
                }
        }
    }

//    fun test(sleepModel: SleepModel) {
//        APIRouter.api().sleepdataDetail(ApiHelper.getInstance().convert(sleepModel)).enqueue(RocatCallback(mActivity, object : ResponseListener() {
//            fun successResponse(response: Any) {
//                val sleepResponse = response as SleepModel
//                if (sleepResponse.getSuccess()) {
//                    val result: SleepModel = sleepResponse.getResult()
//                    mHistoryList.clear()
//                    for (value in result.getData()) {
//                        val historyItem = HistoryItem(value)
//                        val historySleepItem = HistorySleepItem(value)
//                        historyItem.addSubItem(historySleepItem)
//                        mHistoryList.add(historyItem)
//                    }
//                    mHistoryAdapter.setNewData(mHistoryList)
//                }
//            }
//
//            fun errorResponse(response: ResponseBody?) {}
//        }))
//    }
}