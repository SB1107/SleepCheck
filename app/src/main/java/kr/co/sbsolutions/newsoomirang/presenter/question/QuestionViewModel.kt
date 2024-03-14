package kr.co.sbsolutions.newsoomirang.presenter.question

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
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.entity.ContactData
import kr.co.sbsolutions.newsoomirang.data.entity.ContactEntity
import kr.co.sbsolutions.newsoomirang.data.entity.ContactResultData
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val questionRepository: RemoteAuthDataSource
) : BaseServiceViewModel(dataManager, tokenManager) {

    private val _contactResultData: MutableSharedFlow<ContactEntity> = MutableSharedFlow()
    val contactResultData: SharedFlow<ContactEntity> = _contactResultData

    override fun whereTag(): String {
        return "Question"
    }


    fun sendQuestion(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
        }
    }

    fun getContactList() {
        viewModelScope.launch {
            request { questionRepository.getContact() }.collectLatest { it ->
                it.result?.let { resultData ->
                    _contactResultData.emit(ContactEntity(result = resultData))
                    Log.d(TAG, "getContactList: ${resultData} ")
                }
            }
        }
    }
}