package kr.co.sbsolutions.sleepcheck.presenter.question.contactUs

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.entity.BaseEntity
import kr.co.sbsolutions.sleepcheck.domain.model.ContactDetail
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class ContactUsViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val questionRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager,tokenManager) {

    private val _contactResultData : MutableSharedFlow<BaseEntity> = MutableSharedFlow()
    val contactResultData :SharedFlow<BaseEntity> = _contactResultData

    override fun whereTag(): String {
        return "ContactUs"
    }

    fun sendDetail(title:String, detail:String){
        viewModelScope.launch {
            request { questionRepository.postContactDetail(contactDetail = ContactDetail(title = title, detail = detail)) }.collectLatest { result ->
                if (result.success) {
                    _contactResultData.emit(result)
                }
            }
        }
    }
}