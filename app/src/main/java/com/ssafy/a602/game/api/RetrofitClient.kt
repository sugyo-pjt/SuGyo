package com.ssafy.a602.game.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ssafy.a602.auth.interceptor.AuthInterceptor
import com.ssafy.a602.auth.interceptor.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.ssafy.a602.BuildConfig


object RetrofitClient {

    // ✅ Postman과 동일 호스트 + 끝에 / 유지
    private const val BASE_URL = "http://j13a602.p.ssafy.io/"

    private val gson: Gson by lazy {
        GsonBuilder().setLenient().create()
    }

    fun createOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)      // ✅ 저장된 앱 토큰 자동 부착
            .authenticator(tokenAuthenticator)    // ✅ 401 시 토큰 재발급 시도
            .addInterceptor(logging)              // ✅ 디버그 로깅
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // (레거시) 게임용 기본 클라
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    val rhythmApi: RhythmApi by lazy {
        retrofit.create(RhythmApi::class.java)
    }
}
