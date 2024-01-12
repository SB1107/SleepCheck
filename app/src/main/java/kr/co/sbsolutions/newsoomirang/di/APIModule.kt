package kr.co.sbsolutions.newsoomirang.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.common.AuthInterceptor
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.newsoomirang.domain.repository.LoginRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteDataSource
import kr.co.sbsolutions.withsoom.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class APIModule {
    @Singleton
    @Provides
    fun provideRetrofitBuilder() = Retrofit.Builder().baseUrl(BuildConfig.SERVER_URL).addConverterFactory(GsonConverterFactory.create())

    @Singleton
    @Provides
    @Named("Default")
    fun provideSoomirangServerApi(@Named("Default") okHttpClient: OkHttpClient, retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(ServiceAPI::class.java)

    @Singleton
    @Provides
    @Named("Auth")
    fun provideSoomirangAuthServerApi(@Named("Auth") okHttpClient: OkHttpClient, retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(ServiceAPI::class.java)

    @Singleton
    @Provides
    @Named("Default")
    fun provideDefaultOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    }
    @Singleton
    @Provides
    @Named("Auth")
    fun provideOkHttpClient(authInterceptor: AuthInterceptor) : OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
    @Singleton
    @Provides
    fun provideAuthInterceptor(tokenManager: TokenManager) = AuthInterceptor(tokenManager)


}