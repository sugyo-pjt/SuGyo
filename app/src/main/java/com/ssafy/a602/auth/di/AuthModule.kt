package com.ssafy.a602.auth.di

import com.ssafy.a602.auth.api.AuthApiService
import com.ssafy.a602.auth.interceptor.AuthInterceptor
import com.ssafy.a602.auth.interceptor.TokenAuthenticator
import com.ssafy.a602.game.api.RetrofitClient
import com.ssafy.a602.game.api.RhythmApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * 인증 관련 의존성 주입 모듈
 * AuthApiService, AuthInterceptor, TokenAuthenticator 등을 제공
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    /**
     * AuthApiService 제공
     * AuthInterceptor와 TokenAuthenticator가 포함된 Retrofit 인스턴스를 사용
     */
    @Provides
    @Singleton
    fun provideAuthApiService(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): AuthApiService {
        val okHttpClient = RetrofitClient.createOkHttpClient(authInterceptor, tokenAuthenticator)
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(AuthApiService::class.java)
    }
    
    /**
     * 토큰 재발행용 AuthApiService 제공
     * 순환 참조를 방지하기 위해 인터셉터가 없는 별도 인스턴스
     */
    @Provides
    @Singleton
    @Named("TokenRefresh")
    fun provideTokenRefreshApiService(): AuthApiService {
        // 토큰 재발행용 별도 OkHttpClient (인터셉터 없음)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(AuthApiService::class.java)
    }
    
    /**
     * RhythmApi 제공
     * AuthInterceptor와 TokenAuthenticator가 포함된 Retrofit 인스턴스를 사용
     */
    @Provides
    @Singleton
    fun provideRhythmApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): RhythmApi {
        val okHttpClient = RetrofitClient.createOkHttpClient(authInterceptor, tokenAuthenticator)
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(RhythmApi::class.java)
    }
}
