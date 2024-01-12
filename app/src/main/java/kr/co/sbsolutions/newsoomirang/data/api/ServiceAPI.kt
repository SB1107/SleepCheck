package kr.co.sbsolutions.newsoomirang.data.api

import kr.co.sbsolutions.newsoomirang.data.model.FaqModel
import kr.co.sbsolutions.newsoomirang.data.model.ImageModel
import kr.co.sbsolutions.newsoomirang.data.model.NoticeModel
import kr.co.sbsolutions.newsoomirang.data.model.QnaModel
import kr.co.sbsolutions.newsoomirang.data.model.SleepModel
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.QueryMap

interface ServiceAPI {

    //* 회원 > SNS 회원 로그인
    @FormUrlEncoded
    @POST
    suspend fun postLogin(@Body loginModel : SnsLoginModel): Response<UserEntity>

    //로그 아웃
    @FormUrlEncoded
    @POST
    suspend fun postLogOut(): Response<UserEntity>

    //회원 탈퇴
    @FormUrlEncoded
    @POST
    suspend fun postLeave(@Body userModel : UserEntity): Response<UserEntity>

    //수면 데이터 결과
    @GET("sleepdata/result")
    suspend fun getSleepDataResult(): Response<SleepModel>

    //수면 데이터 결과 주로 보기
    @GET("sleepdata/week")
    suspend fun getSleepDataWeekResult(): Response<SleepModel>

    //코골이 데이터 결과
    @GET("snoredata/result")
    suspend fun snoreDataResult(): Response<SleepModel>

    //수면 데이터 날짜별 상세 보기
    @GET("sleepdata/detail")
    suspend fun sleepDataDetail(@Body sleepModel : SleepModel): Response<SleepModel>

    //수면데이터 측정 시작
    @FormUrlEncoded
    @POST
    suspend fun postSleepDataCreate(@Body sleepModel : SleepModel): Response<SleepModel>

    @FormUrlEncoded
    @POST("sleepdata/delete")
    suspend fun  postSleepDataDelete(@Body sleepModel : SleepModel): Response<SleepModel>

    @Multipart
    @POST("sleepdata/upload")
    suspend fun uploading(@Part file : List<MultipartBody.Part>) : Response<SleepModel>

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