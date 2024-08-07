package kr.co.sbsolutions.sleepcheck.data.api

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
import kr.co.sbsolutions.sleepcheck.domain.model.CheckSensor
import kr.co.sbsolutions.sleepcheck.domain.model.ContactDetail
import kr.co.sbsolutions.sleepcheck.domain.model.PolicyModel
import kr.co.sbsolutions.sleepcheck.domain.model.SensorFirmVersion
import kr.co.sbsolutions.sleepcheck.domain.model.SignUpModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepCreateModel
import kr.co.sbsolutions.sleepcheck.domain.model.SleepDataRemoveModel
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface AuthServiceAPI {

    @POST("joinagree")
    suspend fun postJoinAgree(@Body policyModel: PolicyModel): Response<UserEntity>

    @FormUrlEncoded
    @POST("fcmupdate")
    suspend fun postFcmUpdate(@Field("fcm_key") newToken: String): Response<UserEntity>

    //로그 아웃
    @FormUrlEncoded
    @POST("logout")
    suspend fun postLogOut(@Field("app_kind") appKind: String = "R"): Response<UserEntity>

    //회원 탈퇴
    @FormUrlEncoded
    @POST("leave")
    suspend fun postLeave(@Field("leave_reason") leaveReason: String, @Field("app_kind") appKind: String = "R"): Response<BaseEntity>

    //수면 데이터 결과
    @GET("sleepdata/result")
    suspend fun getSleepDataResult(@Query("app_kind") appKind: String = "R"): Response<SleepResultEntity>


    //수면 데이터 결과 년도로 보기
    @GET("sleepdata/years")
    suspend fun getSleepDataYearsResult(@Query("toyear") year: String, @Query("app_kind") appKind: String = "R"): Response<SleepDateEntity>

    //코골이 데이터 결과
    @GET("snoredata/result")
    suspend fun getSnoreDataResult(@Query("app_kind") appKind: String = "R"): Response<NoSeringResultEntity>

    //수면 데이터 날짜별 상세 보기
    @GET("sleepdata/yearsdetail")
    suspend fun sleepDataDetail(@Query("data_id") id: String, @Query("language") language: String, @Query("app_kind") appKind: String = "R"): Response<SleepDetailEntity>

    //수면데이터 측정 시작
    @POST("sleepdata/createv2")
    suspend fun postSleepDataCreate(@Body createModel: SleepCreateModel): Response<SleepCreateEntity>

    @POST("sleepdata/delete")
    suspend fun postSleepDataDelete(@Body sleepDataRemoveModel: SleepDataRemoveModel): Response<BaseEntity>

    @Multipart
    @POST("sleepdata/uploadv2")
    suspend fun postUploading(@Part file: List<MultipartBody.Part>): Response<UploadingEntity>

    //센서 등록 사용가능여부 확인
    @POST("sleepdata/chksensor")
    suspend fun postChkSensor(@Body checkSensor: CheckSensor): Response<UserEntity>

    //문의 내용 조회
    @GET("sleepdata/viewappqa")
    suspend fun getContact(@Query("app_kind") appKind: String = "R"): Response<ContactEntity>

    @POST("sleepdata/regappqa")
    suspend fun postContactDetail(@Body contactDetail: ContactDetail): Response<BaseEntity>

    @POST("sleepdata/disconnect")
    suspend fun postDisconnect(@Body checkSensor: CheckSensor): Response<BaseEntity>

    @GET("sleepdata/viewappfaq")
    suspend fun getFAQ(@Query("language") language: String): Response<FAQEntity>

    @GET("sleepdata/chkversion")
    suspend fun getNewFirmVersion(@Query("number") id: String, @Query("language") language: String, @Query("app_kind") appKind: String = "R"): Response<FirmwareEntity>

    @POST("sleepdata/regversion")
    suspend fun postRegisterFirmVersion(@Body sensorFirmVersion: SensorFirmVersion): Response<BaseEntity>

    @GET("sleepdata/scoremsg")
    suspend fun getScoreMsg(@Query("score") score: String, @Query("type") type: String, @Query("language") language: String): Response<ScoreEntity>

    @POST("updateruser")
    suspend fun postSignUp(@Body signUpModel: SignUpModel): Response<UpdateUserEntity>

    @GET("rentalcompany")
    suspend fun getRentalCompany(): Response<RentalCompanyEntity>

    @FormUrlEncoded
    @POST("alarmset")
    suspend fun postAlarmSet(@Field("app_kind") appKind: String = "R", @Field("alarm") alarm : String): Response<BaseEntity>

    @GET("sleepdata/connectlink")
    suspend fun getConnectLink(@Query("data_id") dataId: Int): Response<ConnectLinkEntity>
}