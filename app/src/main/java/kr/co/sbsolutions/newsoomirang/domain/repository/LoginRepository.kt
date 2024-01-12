package kr.co.sbsolutions.newsoomirang.domain.repository

import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import javax.inject.Inject

class LoginRepository @Inject constructor(private val api: ServiceAPI) : RemoteDataSource {

    override suspend fun postLogin(loginModel: SnsLoginModel): Flow<UserEntity> = flow {
        val resp = api.postLogin(loginModel = loginModel)
        if (resp.isSuccessful) {
            emit(resp.body()!!)
        }else{
            val data =  Gson().fromJson(resp.errorBody()?.string(), UserEntity::class.java)
            emit(data)
        }
    }
}