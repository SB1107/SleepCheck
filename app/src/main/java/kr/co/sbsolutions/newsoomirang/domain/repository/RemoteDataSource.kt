package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel

interface RemoteDataSource {
    suspend fun postLogin(loginModel: SnsLoginModel): Flow<UserEntity>
}
