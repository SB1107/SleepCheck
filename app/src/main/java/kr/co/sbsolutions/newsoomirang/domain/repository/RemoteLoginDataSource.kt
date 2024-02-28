package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.entity.NoSeringResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UploadingEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import java.io.File

interface RemoteLoginDataSource {
     fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>>
}
interface RemoteAuthDataSource {
     fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>>
     fun postLogout(): Flow<ApiResponse<UserEntity>>
     fun postSleepDataCreate(sleepCreateModel: SleepCreateModel) : Flow<ApiResponse<SleepCreateEntity>>
     fun postUploading(file : File?, dataId : Int, sleepType: SleepType, snoreTime: Long = 0) : Flow<ApiResponse<UploadingEntity>>
     fun getWeek(): Flow<ApiResponse<SleepDateEntity>>
     fun getSleepDataResult() : Flow<ApiResponse<SleepResultEntity>>
     fun getSleepDataDetail(endedAt: String): Flow<ApiResponse<SleepDetailEntity>>
     fun postSleepDataRemove(sleepDataRemoveModel: SleepDataRemoveModel) : Flow<ApiResponse<BaseEntity>>
     fun getNoSeringDataResult() : Flow<ApiResponse<NoSeringResultEntity>>

     fun postNewFcmToken(newToken: String) : Flow<ApiResponse<UserEntity>>
     fun postLeave(leaveReason : String) : Flow<ApiResponse<BaseEntity>>
}