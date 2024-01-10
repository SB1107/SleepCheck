package kr.co.sbsolutions.newsoomirang.data.server

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import  kr.co.sbsolutions.newsoomirang.BuildConfig
object ApiHelper {
    private const val baseUrl = BuildConfig.SERVER_URL
    var logging: HttpLoggingInterceptor =
        HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BODY }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient()).build()

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder().addInterceptor(logging)
        return builder.build()
    }

    fun <T> create(api: Class<T>): T = retrofit.create(api)
}