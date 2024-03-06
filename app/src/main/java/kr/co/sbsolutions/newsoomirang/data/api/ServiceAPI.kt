package kr.co.sbsolutions.newsoomirang.data.api

import kr.co.sbsolutions.newsoomirang.data.entity.UserEntity
import kr.co.sbsolutions.newsoomirang.domain.model.SnsLoginModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ServiceAPI {

    //* 회원 > SNS 회원 로그인
    @POST("sns")
    suspend fun postLogin(@Body loginModel : SnsLoginModel): Response<UserEntity>


}