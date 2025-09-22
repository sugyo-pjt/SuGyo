package com.ssafy.a602.game.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ssafy.a602.auth.interceptor.AuthInterceptor
import com.ssafy.a602.auth.interceptor.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.ssafy.a602.BuildConfig

/**
 * 통합 RetrofitClient
 * - Moshi/Gson 양쪽 지원 (AUTO: Moshi → 실패 시 Gson)
 * - AuthInterceptor / TokenAuthenticator 선택 적용
 * - 디버그 빌드에서만 BODY 로깅
 */
object RetrofitClient {

    // Postman과 동일 호스트 (끝에 / 유지)
    private const val BASE_URL = "https://j13a602.p.ssafy.io/"

    enum class JsonEngine { MOSHI, GSON, AUTO }

    /** Gson & Moshi 인스턴스 */
    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    /** 공통 로깅 인터셉터 */
    private fun loggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

    /**
     * OkHttpClient 생성
     * - authInterceptor / tokenAuthenticator 는 선택적으로 주입
     */
    fun createOkHttpClient(
        authInterceptor: AuthInterceptor? = null,
        tokenAuthenticator: TokenAuthenticator? = null,
        connectTimeoutSec: Long = 15,
        readTimeoutSec: Long = 30,
        writeTimeoutSec: Long = 30
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(writeTimeoutSec, TimeUnit.SECONDS)

        if (authInterceptor != null) builder.addInterceptor(authInterceptor)
        if (tokenAuthenticator != null) builder.authenticator(tokenAuthenticator)

        return builder.build()
    }

    /**
     * ConverterFactories 구성
     * - AUTO: Moshi 먼저, 이후 Gson 추가(백업 파서처럼)
     * - MOSHI: Moshi만
     * - GSON: Gson만
     */
    private fun buildConverters(engine: JsonEngine): List<Converter.Factory> = when (engine) {
        JsonEngine.MOSHI -> listOf(MoshiConverterFactory.create(moshi))
        JsonEngine.GSON  -> listOf(GsonConverterFactory.create(gson))
        JsonEngine.AUTO  -> listOf(
            MoshiConverterFactory.create(moshi),
            GsonConverterFactory.create(gson)
        )
    }

    /**
     * Retrofit 생성
     */
    fun createRetrofit(
        okHttpClient: OkHttpClient,
        jsonEngine: JsonEngine = JsonEngine.AUTO
    ): Retrofit {
        val builder = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)

        // 순서대로 addConverterFactory
        buildConverters(jsonEngine).forEach { builder.addConverterFactory(it) }

        return builder.build()
    }

    /**
     * 타입 세이프 서비스 생성 헬퍼
     *
     * @param S Retrofit 인터페이스 타입
     */
    inline fun <reified S> newService(
        jsonEngine: JsonEngine = JsonEngine.AUTO,
        authInterceptor: AuthInterceptor? = null,
        tokenAuthenticator: TokenAuthenticator? = null
    ): S {
        val client = createOkHttpClient(
            authInterceptor = authInterceptor,
            tokenAuthenticator = tokenAuthenticator
        )
        val retrofit = createRetrofit(client, jsonEngine)
        return retrofit.create(S::class.java)
    }

    // ===== 레거시 호환: 비인증 기본 클라이언트/레트로핏/리듬 API =====

    /** (레거시) 비인증 OkHttpClient */
    private val legacyOkHttpClient: OkHttpClient by lazy {
        createOkHttpClient(authInterceptor = null, tokenAuthenticator = null,
            connectTimeoutSec = 30, readTimeoutSec = 30, writeTimeoutSec = 30)
    }

    /** (레거시) AUTO 파서 기반 Retrofit */
    private val legacyRetrofit: Retrofit by lazy {
        createRetrofit(legacyOkHttpClient, JsonEngine.AUTO)
    }

    /** (레거시) rhythmApi: 인증 없이 쓰던 기존 사용처 호환 */
    val rhythmApi: RhythmApi by lazy {
        legacyRetrofit.create(RhythmApi::class.java)
    }

    // ===== 권장: 인증 필요한 API를 안전하게 생성 =====
    fun authedRhythmApi(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        jsonEngine: JsonEngine = JsonEngine.AUTO
    ): RhythmApi {
        val client = createOkHttpClient(authInterceptor, tokenAuthenticator)
        val retrofit = createRetrofit(client, jsonEngine)
        return retrofit.create(RhythmApi::class.java)
    }
}
