package kr.co.sbsolutions.newsoomirang.di

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.common.AuthInterceptor
import kr.co.sbsolutions.newsoomirang.common.DataManager
import kr.co.sbsolutions.newsoomirang.domain.db.SBSensorDataDao
import kr.co.sbsolutions.soomirang.db.SBSensorDataBase
import kr.co.sbsolutions.withsoom.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object  ApplicationModule {
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context) = SBSensorDataBase.getDatabase(context)

    @Provides
    fun provideLogDataDao(db : SBSensorDataBase) = db.logDataDao()


    @Singleton
    @Provides
    @Named("Auth")
    fun provideOkHttpClient(authInterceptor: AuthInterceptor) : OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
//            .authenticator(authAuthenticator)
            .build()
    }

    @Singleton
    @Provides
    @Named("Default")
    fun provideDefaultOkHttpClient() : OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        return OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
    }
    @Singleton
    @Provides
    fun provideDataManager(@ApplicationContext context: Context) = DataManager(context)

    @Singleton
    @Provides
    fun provideAuthInterceptor(tokenManager: TokenManager) = AuthInterceptor(tokenManager)

    @Singleton
    @Provides
    fun provideTokenManager(@ApplicationContext context: Context)  = TokenManager(context)
    @Singleton
    @Provides
    fun provideSBSensorDAO(sbSensorDatabase: SBSensorDataBase) : SBSensorDataDao = sbSensorDatabase.sbSensorDAO()

    @Singleton
    @Provides
    fun provideBluetoothAdapter(@ApplicationContext context: Context) = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

}