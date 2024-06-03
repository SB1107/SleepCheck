package kr.co.sbsolutions.sleepcheck.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.sleepcheck.data.api.ServiceAPI
import kr.co.sbsolutions.sleepcheck.data.entity.UserEntity
import kr.co.sbsolutions.sleepcheck.data.server.ApiResponse
import kr.co.sbsolutions.sleepcheck.domain.apiRequestFlow
import kr.co.sbsolutions.sleepcheck.domain.model.SnsLoginModel
import javax.inject.Inject

class LoginRepository @Inject constructor(private val api: ServiceAPI) : RemoteLoginDataSource {

    override fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postLogin(loginModel = loginModel)
    }
}