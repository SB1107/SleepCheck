package kr.co.sbsolutions.sleepcheck.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.sleepcheck.data.api.AuthServiceAPI
import kr.co.sbsolutions.sleepcheck.data.entity.BaseEntity
import kr.co.sbsolutions.sleepcheck.data.entity.ConnectLinkEntity
import kr.co.sbsolutions.sleepcheck.data.entity.ContactEntity
import kr.co.sbsolutions.sleepcheck.data.entity.FAQEntity
import kr.co.sbsolutions.sleepcheck.data.entity.FirmwareEntity
import kr.co.sbsolutions.sleepcheck.data.entity.NoSeringResultEntity
import kr.co.sbsolutions.sleepcheck.data.entity.RentalCompanyEntity
import kr.co.sbsolutions.sleepcheck.data.entity.ScoreEntity
import kr.co.sbsolutions.sleepcheck.data.entity.SleepCreateEntity
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDateEntity
import kr.co.sbsolutions.sleepcheck.data.entity.SleepDetailEntity
import kr.co.sbsolutions.sleepcheck.data.entity.SleepResultEntity
import kr.co.sbsolutions.sleepcheck.data.entity.UpdateUserEntity
import kr.co.sbsolutions.sleepcheck.data.entity.UploadingEntity
import kr.co.sbsolutions.sleepcheck.data.entity.UserEntity
import kr.co.sbsolutions.sleepcheck.data.server.ApiResponse
import kr.co.sbsolutions.sleepcheck.domain.apiRequestFlow
import kr.co.sbsolutions.sleepcheck.domain.model.CheckSensor
import kr.co.sbsolutions.sleepcheck.domain.model.ContactDetail
import kr.co.sbsolutions.sleepcheck.domain.model.PolicyModel
import kr.co.sbsolutions.sleepcheck.domain.model.SensorFirmVersion
import kr.co.sbsolutions.sleepcheck.domain.model.SignUpModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepCreateModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepType
import kr.co.sbsolutions.sleepcheck.domain.model.SnsLoginModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import javax.inject.Inject

class AuthAPIRepository @Inject constructor(private val api: AuthServiceAPI) : RemoteAuthDataSource {
    override fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postJoinAgree(policyModel = policyModel)
    }

    override fun postSignUp(company: String, name: String, birthday: String): Flow<ApiResponse<UpdateUserEntity>>  = apiRequestFlow {
        val signUpModel = SignUpModel(company, name, birthday)
        api.postSignUp(signUpModel)
    }
    override fun postLogout(): Flow<ApiResponse<UserEntity>> = apiRequestFlow {
        api.postLogOut()
    }

    override fun postSleepDataCreate(sleepCreateModel: SleepCreateModel): Flow<ApiResponse<SleepCreateEntity>> = apiRequestFlow {
        api.postSleepDataCreate(createModel = sleepCreateModel)
    }

    override fun postUploading(file: File?, dataId: Int, sleepType: SleepType, snoreTime: Long, snoreCount: Int, coughCount: Int, sensorName: String): Flow<ApiResponse<UploadingEntity>> =
        apiRequestFlow {
            val dataId = MultipartBody.Part.createFormData("data_id", dataId.toString())
            val appKind = MultipartBody.Part.createFormData("app_kind", "R")
            val snoreTime = MultipartBody.Part.createFormData("snore_time", "$snoreTime")
            val snore_count = MultipartBody.Part.createFormData("snore_count", "$snoreCount")
            val cough_count = MultipartBody.Part.createFormData("cough_count", "$coughCount")
            val sensorName = MultipartBody.Part.createFormData("number", sensorName)
            file?.let {
                val body = MultipartBody.Part.createFormData("file", "sumirang.csv", RequestBody.create("multipart/formdata".toMediaType(), file))
                api.postUploading(arrayListOf(body, dataId, appKind, snoreTime, snore_count, cough_count, sensorName))
            } ?: api.postUploading(arrayListOf(dataId, appKind, snoreTime, snore_count, cough_count, sensorName))
        }


    override fun getYear(year: String): Flow<ApiResponse<SleepDateEntity>> = apiRequestFlow {
        api.getSleepDataYearsResult(year)
    }

    override fun getSleepDataResult(): Flow<ApiResponse<SleepResultEntity>> = apiRequestFlow {
        api.getSleepDataResult()
    }

    override fun getSleepDataDetail(id: String, language: String): Flow<ApiResponse<SleepDetailEntity>> = apiRequestFlow {
        api.sleepDataDetail(id, language)
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

    override fun getContact(): Flow<ApiResponse<ContactEntity>> = apiRequestFlow {
        api.getContact()
    }

    override fun postContactDetail(contactDetail: ContactDetail): Flow<ApiResponse<BaseEntity>> = apiRequestFlow {
        api.postContactDetail(contactDetail)
    }

    override fun postDisconnect(sensorInfo: CheckSensor): Flow<ApiResponse<BaseEntity>> = apiRequestFlow {
        api.postDisconnect(sensorInfo)
    }

    override fun getFAQ(language: String): Flow<ApiResponse<FAQEntity>> = apiRequestFlow {
        api.getFAQ(language)
    }

    override fun getNewFirmVersion(device: String, language: String): Flow<ApiResponse<FirmwareEntity>> = apiRequestFlow {
        api.getNewFirmVersion(device, language)
    }

    override fun postRegisterFirmVersion(sensorFirmVersion: SensorFirmVersion): Flow<ApiResponse<BaseEntity>> = apiRequestFlow {
        api.postRegisterFirmVersion(sensorFirmVersion)
    }

    override fun getScoreMsg(score: String, type: String, language: String): Flow<ApiResponse<ScoreEntity>> = apiRequestFlow {
        api.getScoreMsg(score, type, language)
    }

    override fun getRentalCompany(): Flow<ApiResponse<RentalCompanyEntity>>  = apiRequestFlow {
        api.getRentalCompany()
    }

    override fun postRentalAlarm(isAlarm: Boolean): Flow<ApiResponse<BaseEntity>> = apiRequestFlow {
        api.postAlarmSet(alarm = if (isAlarm) "Y" else "N")
    }

    override fun getConnectLink(dataId: Int): Flow<ApiResponse<ConnectLinkEntity>> = apiRequestFlow {
        api.getConnectLink(dataId = dataId)
    }
}