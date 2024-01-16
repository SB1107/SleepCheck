package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.apiRequestFlow
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import javax.inject.Inject

class LoginRepository @Inject constructor(private val api: ServiceAPI) : RemoteLoginDataSource {

    override fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postLogin(loginModel = loginModel)
    }
}