package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.api.AuthServiceAPI
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.apiRequestFlow
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import javax.inject.Inject

class AuthAPIRepository @Inject constructor(private  val api: AuthServiceAPI) : RemoteAuthDataSource{
    override  fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postJoinAgree(policyModel = policyModel)
    }

    override fun postLogout(): Flow<ApiResponse<UserEntity>> = apiRequestFlow{
        api.postLogOut()
    }

    override fun postSleepDataCreate(sleepCreateModel: SleepCreateModel): Flow<ApiResponse<SleepCreateEntity>> = apiRequestFlow {
        api.postSleepDataCreate(createModel = sleepCreateModel)
    }

}