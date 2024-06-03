package kr.co.sbsolutions.sleepcheck.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.sleepcheck.common.AuthInterceptor
import kr.co.sbsolutions.sleepcheck.common.DataManager
import kr.co.sbsolutions.sleepcheck.common.KaKaoLinkHelper
import kr.co.sbsolutions.sleepcheck.presenter.login.KaKaoLoginHelper
import kr.co.sbsolutions.sleepcheck.common.TokenManager
import kr.co.sbsolutions.sleepcheck.data.api.AuthServiceAPI
import kr.co.sbsolutions.sleepcheck.data.api.DownloadServiceAPI
import kr.co.sbsolutions.sleepcheck.data.api.ServiceAPI
import kr.co.sbsolutions.sleepcheck.data.bluetooth.BluetoothManageRepository
import kr.co.sbsolutions.sleepcheck.data.bluetooth.BluetoothNetworkRepository
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothManageRepository
import kr.co.sbsolutions.sleepcheck.domain.bluetooth.repository.IBluetoothNetworkRepository
import kr.co.sbsolutions.sleepcheck.domain.repository.AuthAPIRepository
import kr.co.sbsolutions.sleepcheck.domain.repository.LoginRepository
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.sleepcheck.domain.repository.RemoteLoginDataSource
import kr.co.sbsolutions.sleepcheck.presenter.login.GoogleLoginHelper
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
    @Named("Default")
    fun provideRetrofitBuilder() = Retrofit.Builder().baseUrl("https://svc1.soomirang.kr/api/").addConverterFactory(GsonConverterFactory.create())

    @Singleton
    @Provides
    @Named("Download")
    fun provideDownloadRetrofitBuilder() = Retrofit.Builder().baseUrl("http://sb-solutions1.net/").addConverterFactory(GsonConverterFactory.create())

    @Singleton
    @Provides
    fun provideInfoApiService(@Named("Auth") okHttpClient: OkHttpClient, @Named("Default") retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(AuthServiceAPI::class.java)

    @Singleton
    @Provides
    fun provideDefaultApiService(@Named("Default") okHttpClient: OkHttpClient,  @Named("Default") retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(ServiceAPI::class.java)

    @Singleton
    @Provides
    fun provideDownloadApiService(@Named("Default") okHttpClient: OkHttpClient,  @Named("Download") retrofit: Retrofit.Builder) = retrofit.client(okHttpClient).build().create(DownloadServiceAPI::class.java)

    @Provides
    fun provideKaKaoLoginHelper(@ApplicationContext context: Context) = KaKaoLoginHelper(context)

    @Provides
    fun provideKaKaoLinkHelper(@ApplicationContext context: Context) = KaKaoLinkHelper(context)

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

    @Binds
    abstract fun bindBluetoothNetworkRepository(bluetoothNetworkRepository: BluetoothNetworkRepository): IBluetoothNetworkRepository

}