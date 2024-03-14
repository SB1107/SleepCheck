package kr.co.sbsolutions.newsoomirang.presenter.question.contactUs

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.domain.model.ContactDetail
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
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