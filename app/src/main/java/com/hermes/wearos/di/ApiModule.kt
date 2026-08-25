package com.hermes.wearos.di

import com.hermes.wearos.core.auth.AuthManager
import com.hermes.wearos.data.api.HermesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideHermesApi(
        @com.hermes.wearos.core.network.AuthInterceptorOkHttpClient okHttpClient: OkHttpClient,
        json: Json,
        authManager: AuthManager
    ): HermesApi {
        return HermesApi(okHttpClient, json, authManager)
    }
}
