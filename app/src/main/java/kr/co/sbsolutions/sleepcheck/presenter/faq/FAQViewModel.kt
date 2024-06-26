package kr.co.sbsolutions.sleepcheck.presenter.faq

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.entity.FAQResultData
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FAQViewModel @Inject constructor(
    private val dataManager: DataManager,
    private val tokenManager: TokenManager,
    private val authAPIRepository: RemoteAuthDataSource,
) : BaseViewModel(dataManager, tokenManager) {
    private  val _faqData : MutableStateFlow<FAQResultData?> = MutableStateFlow(null)
      val faqData : StateFlow<FAQResultData?> = _faqData

    fun getFAQList(language : String) {
        viewModelScope.launch(Dispatchers.IO) {
            request { authAPIRepository.getFAQ(language) }.collectLatest {
                it.result?.let { data ->
                    _faqData.emit(data)
                }
            }
        }
    }
}