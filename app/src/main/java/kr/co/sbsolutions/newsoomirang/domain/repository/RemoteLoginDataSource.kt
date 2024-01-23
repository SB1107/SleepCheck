package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UploadingEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.model.SleepModel
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import okhttp3.RequestBody
import java.io.File

interface RemoteLoginDataSource {
     fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>>
}
interface RemoteAuthDataSource{
     fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>>
     fun postLogout(): Flow<ApiResponse<UserEntity>>
     fun postSleepDataCreate(sleepCreateModel: SleepCreateModel) : Flow<ApiResponse<SleepCreateEntity>>
     fun postUploading(file : File, dataId : Int, sleepType: SleepType, snoreTime: Long = 0) : Flow<ApiResponse<UploadingEntity>>
     fun getWeek(): Flow<ApiResponse<SleepModel>>
     fun getSleepDataResult() : Flow<ApiResponse<SleepResultEntity>>
     fun sleepDataDetail(sleepModel: SleepModel): Flow<ApiResponse<SleepModel>>
     fun postSleepDataRemove(sleepDataRemoveModel: SleepDataRemoveModel) : Flow<ApiResponse<RequestBody>>

}