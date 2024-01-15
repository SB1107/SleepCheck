package kr.co.sbsolutions.newsoomirang.domain.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kr.co.sbsolutions.newsoomirang.data.api.AuthServiceAPI
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import javax.inject.Inject

class PolicyRepository @Inject constructor(private  val api: AuthServiceAPI) : RemotePolicyDataSource{
    override suspend fun postPolicy(policyModel: PolicyModel): Flow<UserEntity> = flow {
        val resp = api.postJoinAgree(policyModel = policyModel)
        if (resp.isSuccessful) {
            emit(resp.body()!!)
        }else{
            val data =  Gson().fromJson(resp.errorBody()?.string(), UserEntity::class.java)
            emit(data)
        }
    }
}