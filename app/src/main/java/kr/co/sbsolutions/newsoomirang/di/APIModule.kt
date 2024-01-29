package kr.co.sbsolutions.newsoomirang.di

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.ApplicationManager
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.newsoomirang.common.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import kr.co.sbsolutions.newsoomirang.common.AuthInterceptor
import kr.co.sbsolutions.newsoomirang.data.api.AuthServiceAPI
import kr.co.sbsolutions.newsoomirang.domain.repository.LoginRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.AuthAPIRepository
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteLoginDataSource
import kr.co.sbsolutions.newsoomirang.domain.repository.RemoteAuthDataSource
import kr.co.sbsolutions.newsoomirang.data.bluetooth.BluetoothManageRepository
import kr.co.sbsolutions.newsoomirang.data.server.ApiResponse
import kr.co.sbsolutions.newsoomirang.domain.bluetooth.repository.IBluetoothManageRepository
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

    @Singleton
    @Provides
    fun <T>providesRequestFlow(coroutineScope: CoroutineScope): CoroutineScope {
        if (!ApplicationManager.getNetworkCheck()) {
            coroutineScope.launch {
//                _errorMessage.emit("네트워크 연결이 되어 있지 않습니다. \n확인후 다시 실행해주세요")
                cancel("네트워크 오류")
            }
        }else{
            mJob = viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
                viewModelScope.launch(Dispatchers.Main) {
                    if(error.message != "Unable to create instance of class okhttp3.RequestBody. Registering an InstanceCreator or a TypeAdapter for this type, or adding a no-args constructor may fix this problem."){
                        _errorMessage.emit(error.localizedMessage ?: "Error occured! Please try again.")
                    }
                }
            })
    }

}

@Module
@InstallIn(ViewModelComponent::class)
abstract class ViewModelBindsModule {
    @Binds
    abstract fun provideRemoteLoginDataSource(loginRepository: LoginRepository): RemoteLoginDataSource

    @Binds
    abstract fun bindBluetoothManageRepository(bluetoothManageRepository: BluetoothManageRepository) : IBluetoothManageRepository

}

@Module
@InstallIn(SingletonComponent::class)
abstract class SingleToneBindingModule
{
    @Binds
    abstract fun provideRemotePolicyDataSource(policyRepository: AuthAPIRepository): RemoteAuthDataSource

}