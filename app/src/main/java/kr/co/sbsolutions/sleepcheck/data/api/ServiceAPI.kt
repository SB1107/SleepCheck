package kr.co.sbsolutions.sleepcheck.data.api

import kr.co.sbsolutions.sleepcheck.data.entity.UserEntity
import kr.co.sbsolutions.sleepcheck.domain.model.SnsLoginModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ServiceAPI {

    //* 회원 > SNS 회원 로그인
    @POST("sns")
    suspend fun postLogin(@Body loginModel : SnsLoginModel): Response<UserEntity>


}