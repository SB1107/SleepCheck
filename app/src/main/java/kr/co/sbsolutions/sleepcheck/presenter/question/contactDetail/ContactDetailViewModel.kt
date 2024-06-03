package kr.co.sbsolutions.sleepcheck.presenter.question.contactDetail

import dagger.hilt.android.lifecycle.HiltViewModel
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.presenter.BaseServiceViewModel
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