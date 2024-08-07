package kr.co.sbsolutions.sleepcheck.domain.repository

import kotlinx.coroutines.flow.Flow
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
import kr.co.sbsolutions.sleepcheck.domain.model.CheckSensor
import kr.co.sbsolutions.sleepcheck.domain.model.ContactDetail
import kr.co.sbsolutions.sleepcheck.domain.model.PolicyModel
import kr.co.sbsolutions.sleepcheck.domain.model.SensorFirmVersion
import kr.co.sbsolutions.sleepcheck.domain.model.SignUpModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepCreateModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepType
import kr.co.sbsolutions.sleepcheck.domain.model.SnsLoginModel
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

interface RemoteLoginDataSource {
     fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>>
}
interface RemoteAuthDataSource {
     fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>>
     fun postSignUp(company: String, name: String, birthday: String): Flow<ApiResponse<UpdateUserEntity>>
     fun postLogout(): Flow<ApiResponse<UserEntity>>
     fun postSleepDataCreate(sleepCreateModel: SleepCreateModel) : Flow<ApiResponse<SleepCreateEntity>>
     fun postUploading(file : File?, dataId : Int, sleepType: SleepType, snoreTime: Long = 0, snoreCount : Int = 0, coughCount : Int = 0, sensorName : String) : Flow<ApiResponse<UploadingEntity>>
     fun getYear(year: String): Flow<ApiResponse<SleepDateEntity>>
     fun getSleepDataResult() : Flow<ApiResponse<SleepResultEntity>>
     fun getSleepDataDetail(id: String, language : String): Flow<ApiResponse<SleepDetailEntity>>
     fun postSleepDataRemove(sleepDataRemoveModel: SleepDataRemoveModel) : Flow<ApiResponse<BaseEntity>>
     fun getNoSeringDataResult() : Flow<ApiResponse<NoSeringResultEntity>>

     fun postNewFcmToken(newToken: String) : Flow<ApiResponse<UserEntity>>
     fun postLeave(leaveReason : String) : Flow<ApiResponse<BaseEntity>>
     fun postCheckSensor(sensorInfo: CheckSensor) : Flow<ApiResponse<UserEntity>>
     fun getContact() : Flow<ApiResponse<ContactEntity>>

     fun postContactDetail(contactDetail: ContactDetail) : Flow<ApiResponse<BaseEntity>>
     fun postDisconnect(sensorInfo: CheckSensor) : Flow<ApiResponse<BaseEntity>>

     fun getFAQ(language:String) : Flow<ApiResponse<FAQEntity>>
     
     fun getNewFirmVersion(deviceName: String, language: String) : Flow<ApiResponse<FirmwareEntity>>
     
     fun postRegisterFirmVersion(sensorFirmVersion: SensorFirmVersion) : Flow<ApiResponse<BaseEntity>>

     fun getScoreMsg(score : String, type: String, language : String): Flow<ApiResponse<ScoreEntity>>

     fun getRentalCompany() : Flow<ApiResponse<RentalCompanyEntity>>
     fun postRentalAlarm(isAlarm : Boolean) : Flow<ApiResponse<BaseEntity>>
     fun getConnectLink(dataId: Int): Flow<ApiResponse<ConnectLinkEntity>>
}
interface RemoteDownload {
     fun getDownloadZipFile(path: String , fileName : String): Flow<ResponseBody>
}