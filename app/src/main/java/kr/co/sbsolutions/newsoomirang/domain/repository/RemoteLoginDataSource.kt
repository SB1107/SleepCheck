package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel

interface RemoteLoginDataSource {
     fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>>
}
interface RemotePolicyDataSource{
     fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>>
}