package kr.co.sbsolutions.newsoomirang.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface DownloadServiceAPI {

    @Streaming
    @GET("/{path}/{fileName}")
    suspend fun getDownloadFile(@Path(value = "path" , encoded = true) path: String, @Path(value = "fileName") fileName: String): Response<ResponseBody>

}