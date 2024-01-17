package kr.co.sbsolutions.newsoomirang.data.api

import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.data.model.FaqModel
import kr.co.sbsolutions.newsoomirang.data.model.ImageModel
import kr.co.sbsolutions.newsoomirang.data.model.NoticeModel
import kr.co.sbsolutions.newsoomirang.data.model.QnaModel
import kr.co.sbsolutions.newsoomirang.data.model.SleepModel
import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import okhttp3.MultipartBody
import okhttp3.ResponseBody
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
    @POST("sns")
    suspend fun postLogin(@Body loginModel : SnsLoginModel): Response<UserEntity>


}