package kr.co.sbsolutions.newsoomirang.domain.repository

import kotlinx.coroutines.flow.Flow
import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.entity.ContactEntity
import kr.co.sbsolutions.newsoomirang.data.entity.FAQEntity
import kr.co.sbsolutions.newsoomirang.data.entity.FirmwareEntity
import kr.co.sbsolutions.newsoomirang.data.entity.NoSeringResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UploadingEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.model.CheckSensor
import kr.co.sbsolutions.newsoomirang.domain.model.ContactDetail
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SensorFirmVersion
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepType
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import okhttp3.ResponseBody
import java.io.File

interface RemoteLoginDataSource {
     fun postLogin(loginModel: SnsLoginModel): Flow<ApiResponse<UserEntity>>
}
interface RemoteAuthDataSource {
     fun postPolicy(policyModel: PolicyModel): Flow<ApiResponse<UserEntity>>
     fun postLogout(): Flow<ApiResponse<UserEntity>>
     fun postSleepDataCreate(sleepCreateModel: SleepCreateModel) : Flow<ApiResponse<SleepCreateEntity>>
     fun postUploading(file : File?, dataId : Int, sleepType: SleepType, snoreTime: Long = 0, snoreCount : Int = 0, coughCount : Int = 0, sensorName : String) : Flow<ApiResponse<UploadingEntity>>
     fun getYear(year: String): Flow<ApiResponse<SleepDateEntity>>
     fun getSleepDataResult() : Flow<ApiResponse<SleepResultEntity>>
     fun getSleepDataDetail(id: String): Flow<ApiResponse<SleepDetailEntity>>
     fun postSleepDataRemove(sleepDataRemoveModel: SleepDataRemoveModel) : Flow<ApiResponse<BaseEntity>>
     fun getNoSeringDataResult() : Flow<ApiResponse<NoSeringResultEntity>>

     fun postNewFcmToken(newToken: String) : Flow<ApiResponse<UserEntity>>
     fun postLeave(leaveReason : String) : Flow<ApiResponse<BaseEntity>>
     fun postCheckSensor(sensorInfo: CheckSensor) : Flow<ApiResponse<UserEntity>>
     fun getContact() : Flow<ApiResponse<ContactEntity>>

     fun postContactDetail(contactDetail: ContactDetail) : Flow<ApiResponse<BaseEntity>>
     fun postDisconnect(sensorInfo: CheckSensor) : Flow<ApiResponse<BaseEntity>>

     fun getFAQ() : Flow<ApiResponse<FAQEntity>>
     
     fun getNewFirmVersion(deviceName: String) : Flow<ApiResponse<FirmwareEntity>>
     
     fun postRegisterFirmVersion(sensorFirmVersion: SensorFirmVersion) : Flow<ApiResponse<BaseEntity>>
}
interface RemoteDownload {
     fun getDownloadZipFile(path: String , fileName : String): Flow<ResponseBody>
}