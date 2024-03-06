package kr.co.sbsolutions.newsoomirang.presenter.main.history.detail

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    dataManager: DataManager,
    tokenManager: TokenManager,
    private val authDataSource: RemoteAuthDataSource
) : BaseViewModel(dataManager, tokenManager) {

    private val _sleepDataDetailData: MutableSharedFlow<SleepDetailResult> = MutableSharedFlow()
    val sleepDataDetailData: SharedFlow<SleepDetailResult> = _sleepDataDetailData
    fun getSleepData(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authDataSource.getSleepDataDetail(id) }
                .collectLatest {
                    it.result?.let {result ->
                        _sleepDataDetailData.emit(result)

                    }
                }
        }

    }
}