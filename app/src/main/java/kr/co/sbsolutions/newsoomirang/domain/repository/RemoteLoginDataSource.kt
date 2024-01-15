package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel

interface RemoteLoginDataSource {
    suspend fun postLogin(loginModel: SnsLoginModel): Flow<UserEntity>
}
interface RemotePolicyDataSource{
    suspend fun postPolicy(policyModel: PolicyModel): Flow<UserEntity>
}