package kr.co.sbsolutions.newsoomirang.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.sbsolutions.newsoomirang.data.api.ServiceAPI
import kr.co.sbsolutions.newsoomirang.data.server.ApiHelper

@Module
@InstallIn(SingletonComponent::class)
abstract class APIModule {
    companion object {
        @Provides
        fun provideDanteServerApi() = ApiHelper.create(ServiceAPI::class.java)
    }
}