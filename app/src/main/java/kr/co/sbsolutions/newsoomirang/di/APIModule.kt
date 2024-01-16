package kr.co.sbsolutions.newsoomirang.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.withsoom.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import kr.co.sbsolutions.newsoomirang.common.AuthInterceptor
import kr.co.sbsolutions.newsoomirang.data.api.AuthServiceAPI
import kr.co.sbsolutions.newsoomirang.domain.repository.LoginRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.PolicyRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteLoginDataSource
import kr.co.sbsolutions.newsoomirang.domain.repository.RemotePolicyDataSource
import kr.co.sbsolutions.withsoom.data.repository.bluetooth.BluetoothManageRepository
import kr.co.sbsolutions.withsoom.domain.bluetooth.repository.IBluetoothManageRepository
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
class APIModule {
    @Singleton
    @Provides
    fun provideTokenManager(@ApplicationContext context: Context) = TokenManager(context)

    @Singleton
    @Provides
    fun provideDataManager(@ApplicationContext context: Context) = DataManager(context)

    @Singleton
    @Provides
    @Named("Auth")
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }


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
    fun provideAuthAuthenticator(tokenManager: TokenManager) = AuthInterceptor(tokenManager)

    @Singleton
    @Provides
    fun provideRetrofitBuilder() = Retrofit.Builder().baseUrl("https://svc1.soomirang.kr/api/").addConverterFactory(GsonConverterFactory.create())

    @Singleton
    @Provides
    fun provideInfoApiService(@Named("Auth") okHttpClient: OkHttpClient, retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(AuthServiceAPI::class.java)

    @Singleton
    @Provides
    fun provideDefaultApiService(@Named("Default") okHttpClient: OkHttpClient, retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(ServiceAPI::class.java)

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelBindsModule {
    @Binds
    abstract fun provideRemoteLoginDataSource(loginRepository: LoginRepository): RemoteLoginDataSource

    @Binds
    abstract fun provideRemotePolicyDataSource(policyRepository: PolicyRepository): RemotePolicyDataSource

    @Binds
    abstract fun bindBluetoothManageRepository(bluetoothManageRepository: BluetoothManageRepository) : IBluetoothManageRepository

}