package kr.co.sbsolutions.newsoomirang.data.api

import kr.co.sbsolutions.newsoomirang.data.entity.BaseEntity
import kr.co.sbsolutions.newsoomirang.data.entity.NoSeringResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepCreateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailEntity
import kr.co.sbsolutions.newsoomirang.data.entity.SleepResultEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UploadingEntity
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.data.model.FaqModel
import kr.co.sbsolutions.newsoomirang.data.model.ImageModel
import kr.co.sbsolutions.newsoomirang.data.model.NoticeModel
import kr.co.sbsolutions.newsoomirang.data.model.QnaModel
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
import retrofit2.http.QueryMap

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

    //수면 데이터 결과 주로 보기
    @GET("sleepdata/week")
    suspend fun getSleepDataWeekResult(): Response<SleepDateEntity>

    //코골이 데이터 결과
    @GET("snoredata/result")
    suspend fun getSnoreDataResult(): Response<NoSeringResultEntity>

    //수면 데이터 날짜별 상세 보기
    @GET("sleepdata/detail")
    suspend fun sleepDataDetail(@Query("ended_at") day: String): Response<SleepDetailEntity>

    //수면데이터 측정 시작
    @POST("sleepdata/create")
    suspend fun postSleepDataCreate(@Body createModel: SleepCreateModel): Response<SleepCreateEntity>

    @POST("sleepdata/delete")
    suspend fun postSleepDataDelete(@Body sleepDataRemoveModel: SleepDataRemoveModel): Response<BaseEntity>

    @Multipart
    @POST("sleepdata/upload")
    suspend fun postUploading(@Part file : List<MultipartBody.Part>) : Response<UploadingEntity>

    @GET("notice/list")
    suspend fun getNoticeList(@QueryMap notice : Map<String , Any>) : Response<NoticeModel>

    @GET("notice/detail")
    suspend fun getNoticeDetail(@QueryMap notice : Map<String , Any>) : Response<NoticeModel>

    @GET("faq/list")
    suspend fun getFaqList(@QueryMap faq : Map<String , Any>) : Response<FaqModel>

    //--------------------------------------------------------------------------------------------
    // MARK : 1:1 문의
    //--------------------------------------------------------------------------------------------
    @GET("qa/list")
    suspend fun qaList(@QueryMap map: Map<String , Any>): Response<QnaModel>

    @GET("qa/detail")
    suspend fun qaDetail(@QueryMap map: Map<String, Any>): Response<QnaModel>

    @FormUrlEncoded
    @POST("qa/create")
    suspend fun postQACreate(@Body qna : QnaModel): Response<QnaModel>

    @FormUrlEncoded
    @POST("qa/delete")
    suspend fun postQADelete(@Body qna : QnaModel): Response<QnaModel>

    //--------------------------------------------------------------------------------------------
    // MARK : 파일
    //--------------------------------------------------------------------------------------------
    @Multipart
    @POST("imageupload")
    fun postImageUpload(@Part file: MultipartBody.Part): Response<ImageModel>

    @Multipart
    @POST("imageMultiUpload")
    fun postImageMultiUpload(@Part file: List<MultipartBody.Part>): Response<ImageModel>
}