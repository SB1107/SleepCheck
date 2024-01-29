package kr.co.sbsolutions.newsoomirang.presenter.main.history

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    dataManager: DataManager,
    tokenManager: TokenManager,
    private val authDataSource: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager , tokenManager) {
    private val _sleepWeekData: MutableSharedFlow<SleepDateEntity> = MutableSharedFlow()
    val sleepWeekData: SharedFlow<SleepDateEntity> = _sleepWeekData
    private  val _sleepDataDetailData : MutableSharedFlow<List<SleepDetailResult>> = MutableSharedFlow()
      val sleepDataDetailData : SharedFlow<List<SleepDetailResult>> = _sleepDataDetailData

     fun getWeekSleepData() {
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.getWeek() }
                .collectLatest {
                    _sleepWeekData.emit(it)
//                    getDetailSleepData(it)
//                    test(it)
                }
        }
    }

     fun getDetailSleepData(localDate: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.getSleepDataDetail(localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
                .collectLatest { sleepData ->
                    sleepData.result?.data?.let {
                        _sleepDataDetailData.emit(it)
                    }
                }
        }
    }

    override fun whereTag(): String {
        return  "History"
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