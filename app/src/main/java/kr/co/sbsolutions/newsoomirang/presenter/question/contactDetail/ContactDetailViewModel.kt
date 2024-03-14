package kr.co.sbsolutions.newsoomirang.presenter.question.contactDetail

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val questionRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager,tokenManager){
    override fun whereTag(): String {
        return "ContactDetail"
    }

}