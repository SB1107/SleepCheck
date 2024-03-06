package kr.co.sbsolutions.newsoomirang.data.api

import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.entity.NoSeringResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UploadingEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.PolicyModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepCreateModel
import kr.co.sbsolutions.newsoomirang.domain.model.SleepDataRemoveModel
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
    suspend fun postJoinAgree(@Body policyModel : PolicyModel): Response<UserEntity>

    @FormUrlEncoded
    @POST("fcmupdate")
    suspend fun postFcmUpdate(@Field("fcm_key") newToken : String): Response<UserEntity>

    //로그 아웃
    @POST("logout")
    suspend fun postLogOut(): Response<UserEntity>

    //회원 탈퇴
    @FormUrlEncoded
    @POST("leave")
    suspend fun postLeave(@Field("leave_reason")  leaveReason : String): Response<BaseEntity>

    //수면 데이터 결과
    @GET("sleepdata/result")
    suspend fun getSleepDataResult(): Response<SleepResultEntity>


    //수면 데이터 결과 년도로 보기
    @GET("sleepdata/years")
    suspend fun getSleepDataYearsResult(@Query("toyear") year : String): Response<SleepDateEntity>

    //코골이 데이터 결과
    @GET("snoredata/result")
    suspend fun getSnoreDataResult(): Response<NoSeringResultEntity>

    //수면 데이터 날짜별 상세 보기
    @GET("sleepdata/yearsdetail")
    suspend fun sleepDataDetail(@Query("data_id") id: String): Response<SleepDetailEntity>

    //수면데이터 측정 시작
    @POST("sleepdata/createplus")
    suspend fun postSleepDataCreate(@Body createModel: SleepCreateModel): Response<SleepCreateEntity>

    @POST("sleepdata/delete")
    suspend fun postSleepDataDelete(@Body sleepDataRemoveModel: SleepDataRemoveModel): Response<BaseEntity>

    @Multipart
    @POST("sleepdata/uploadv2")
    suspend fun postUploading(@Part file : List<MultipartBody.Part>) : Response<UploadingEntity>

}