package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.api.AuthServiceAPI
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.entity.NoSeringResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UploadingEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.apiRequestFlow
import kr.co.sbsolutions.newsoomirang.domain.model.CheckSensor
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

class AuthAPIRepository @Inject constructor(private val api: AuthServiceAPI) : RemoteAuthDataSource {
    override fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postJoinAgree(policyModel = policyModel)
    }

    override fun postLogout(): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postLogOut()
    }

    override fun postSleepDataCreate(sleepCreateModel: SleepCreateModel): Flow<ApiResponse<SleepCreateEntity>> = apiRequestFlow {
        api.postSleepDataCreate(createModel = sleepCreateModel)
    }

    override fun postUploading(file: File?, dataId: Int, sleepType: SleepType, snoreTime: Long, sensorName: String): Flow<ApiResponse<UploadingEntity>> = apiRequestFlow {
        val dataId = MultipartBody.Part.createFormData("data_id", dataId.toString())
        val appKind = MultipartBody.Part.createFormData("app_kind", "C")
        val snoreTime = MultipartBody.Part.createFormData("snore_time", "$snoreTime")
        val sensorName = MultipartBody.Part.createFormData("number", sensorName)
        file?.let {
            val body = MultipartBody.Part.createFormData("file", "sumirang.csv", RequestBody.create("multipart/formdata".toMediaType(), file))
            api.postUploading(arrayListOf(body, dataId, appKind, snoreTime , sensorName))
        } ?: api.postUploading(arrayListOf(dataId, appKind, snoreTime , sensorName))
    }


    override fun getYear(year: String): Flow<ApiResponse<SleepDateEntity>> = apiRequestFlow {
        api.getSleepDataYearsResult(year)
    }

    override fun getSleepDataResult(): Flow<ApiResponse<SleepResultEntity>> = apiRequestFlow {
        api.getSleepDataResult()
    }

    override fun getSleepDataDetail(endedAt: String): Flow<ApiResponse<SleepDetailEntity>> = apiRequestFlow {
        api.sleepDataDetail(endedAt)
    }

    override fun postSleepDataRemove(sleepDataRemoveModel: SleepDataRemoveModel): Flow<ApiResponse<BaseEntity>> = apiRequestFlow {
        api.postSleepDataDelete(sleepDataRemoveModel)
    }

    override fun getNoSeringDataResult(): Flow<ApiResponse<NoSeringResultEntity>> = apiRequestFlow {
        api.getSnoreDataResult()
    }

    override fun postNewFcmToken(newToken: String): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postFcmUpdate(newToken)
    }

    override fun postLeave(leaveReason: String): Flow<ApiResponse<BaseEntity>> = apiRequestFlow {
        api.postLeave(leaveReason)
    }

    override fun postCheckSensor(sensorInfo: CheckSensor): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postChkSensor(sensorInfo)
    }
}