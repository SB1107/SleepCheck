package kr.co.sbsolutions.newsoomirang.data.server

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import  kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.withsoom.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

object ApiHelper {
    private  val baseUrl = BuildConfig.SERVER_URL
//    @Inject lateinit var  tokenManager: TokenManager

    var logging: HttpLoggingInterceptor =
        HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BODY }

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(createOkHttpClient()).build()

    private val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .client(authCreateOkHttpClient()).build()

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder().addInterceptor(logging)
        return builder.build()
    }
    private fun authCreateOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder().addInterceptor(logging)
            .addInterceptor(Interceptor { chain ->
//                val token = runBlocking {
//                    tokenManager.getToken().first()
//                }
                val token = ""
                val request = chain.request().newBuilder()
                request.addHeader("Authorization", "Bearer $token").build()
                chain.proceed(request.build())
            })
        return builder.build()
    }

    fun <T> create(api: Class<T>): T = retrofit.create(api)
    fun <T> createAuth(api: Class<T>): T = authRetrofit.create(api)
}