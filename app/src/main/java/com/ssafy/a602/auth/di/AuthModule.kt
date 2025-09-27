package com.ssafy.a602.auth.di

import com.ssafy.a602.auth.api.AuthApiService
import com.ssafy.a602.auth.interceptor.AuthInterceptor
import com.ssafy.a602.auth.interceptor.TokenAuthenticator
import com.ssafy.a602.game.api.RetrofitClient
import com.ssafy.a602.game.api.RhythmApi
import com.ssafy.a602.game.api.RhythmResultApi
import com.ssafy.a602.learning.api.StudyApiService
import com.ssafy.a602.term.data.remote.TermApi
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

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    // === 기존 제공자들 (그대로) =========================================
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

    @Provides
    @Singleton
    @Named("TokenRefresh")
    fun provideTokenRefreshApiService(): AuthApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val ok = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = RetrofitClient.createRetrofit(ok)
        return retrofit.create(AuthApiService::class.java)
    }

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

    @Provides
    @Singleton
    fun provideRhythmResultApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): RhythmResultApi {
        val okHttpClient = RetrofitClient.createOkHttpClient(authInterceptor, tokenAuthenticator)
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(RhythmResultApi::class.java)
    }
    

    // === 디버그 전용: StudyApiService는 "강제 토큰 1개만" 붙여 호출 =========
    // → 다른 인터셉터/Authenticator 미사용 (헤더 충돌 방지)
    @Provides
    @Singleton
    fun provideStudyApiService(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): StudyApiService {
        val okHttpClient = RetrofitClient.createOkHttpClient(authInterceptor, tokenAuthenticator)
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(StudyApiService::class.java)
    }

    // === 약관 API 서비스 =========================================
    @Provides
    @Singleton
    fun provideTermApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): TermApi {
        val okHttpClient = RetrofitClient.createOkHttpClient(authInterceptor, tokenAuthenticator)
        val retrofit = RetrofitClient.createRetrofit(okHttpClient)
        return retrofit.create(TermApi::class.java)
    }
}
