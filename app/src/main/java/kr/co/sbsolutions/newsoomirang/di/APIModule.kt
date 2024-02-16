package kr.co.sbsolutions.newsoomirang.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.common.AuthInterceptor
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.presenter.login.KaKaoLoginHelper
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import kr.co.sbsolutions.newsoomirang.data.api.AuthServiceAPI
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.newsoomirang.data.bluetooth.BluetoothManageRepository
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothManageRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.AuthAPIRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.LoginRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteLoginDataSource
import kr.co.sbsolutions.newsoomirang.presenter.login.GoogleLoginHelper
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

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

    @Provides
    fun provideKaKaoLoginHelper(@ApplicationContext context: Context) = KaKaoLoginHelper(context)

    @Provides
    fun provideGoogleLoginHelper() = GoogleLoginHelper()
}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelBindsModule {
    @Binds
    abstract fun provideRemoteLoginDataSource(loginRepository: LoginRepository): RemoteLoginDataSource

    @Binds
    abstract fun bindBluetoothManageRepository(bluetoothManageRepository: BluetoothManageRepository): IBluetoothManageRepository

}

@Module
@InstallIn(SingletonComponent::class)
abstract class SingleToneBindingModule {
    @Binds
    abstract fun provideRemotePolicyDataSource(policyRepository: AuthAPIRepository): RemoteAuthDataSource

}