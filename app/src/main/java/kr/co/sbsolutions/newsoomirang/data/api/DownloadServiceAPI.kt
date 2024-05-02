package kr.co.sbsolutions.newsoomirang.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming

interface DownloadServiceAPI {

    @Streaming
    @GET
    suspend fun getDownloadFile(): Response<ResponseBody>

}