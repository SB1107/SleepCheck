package kr.co.sbsolutions.newsoomirang.presenter.question

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.presenter.BaseServiceViewModel
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val dataManager: DataManager,
    tokenManager: TokenManager,
    private val questionRepository: RemoteAuthDataSource
) :BaseServiceViewModel (dataManager, tokenManager ) {

    override fun whereTag(): String {
        return "Question"
    }
    init {

    }

    fun sendQuestion(text: String){
        viewModelScope.launch(Dispatchers.IO) {
        }

    }



}