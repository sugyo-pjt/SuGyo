package com.ssafy.a602.search

import com.ssafy.a602.auth.TokenManager
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

// ───────────────────────────────────────────────────────────────────
// 데이터 모델 (새로운 API 스펙에 맞춤)
// ───────────────────────────────────────────────────────────────────
data class StudySearchItemDto(
    val wordId: Long,
    val word: String
)

data class StudyDetailDto(
    val wordId: Long,
    val word: String,
    val description: String,
    val videoUrl: String,
    val sameMotionWord: List<String>
)

// ───────────────────────────────────────────────────────────────────
// API 인터페이스 (새로운 API 스펙에 맞춤)
// ───────────────────────────────────────────────────────────────────
interface SearchApi {
    // GET /api/v1/study/search/{keyword}
    @GET("/api/v1/study/search/{keyword}")
    suspend fun searchWords(@Path("keyword") keyword: String): List<StudySearchItemDto>

    // GET /api/v1/study/detail/{wordId}
    @GET("/api/v1/study/detail/{wordId}")
    suspend fun getWordDetail(@Path("wordId") wordId: Long): StudyDetailDto
}

// ───────────────────────────────────────────────────────────────────
// API Provider (Hilt 사용)
// ───────────────────────────────────────────────────────────────────
@Singleton
class SearchApiProvider @Inject constructor(
    private val tokenManager: TokenManager
) {
    val api: SearchApi by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY
        }
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val accessToken = tokenManager.getAccessToken()
                
                val request = originalRequest.newBuilder()
                    .addHeader("User-Agent", "Android-App/1.0")
                    .addHeader("Accept", "application/json")
                    .apply {
                        if (!accessToken.isNullOrBlank()) {
                            addHeader("Authorization", "Bearer $accessToken")
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()
            
        Retrofit.Builder()
            .baseUrl("https://j13a602.p.ssafy.io/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SearchApi::class.java)
    }
}

// ───────────────────────────────────────────────────────────────────
// Repository (새로운 API 스펙에 맞춤)
// ───────────────────────────────────────────────────────────────────
@Singleton
class SearchRepository @Inject constructor(
    private val apiProvider: SearchApiProvider
) {
    private val api: SearchApi get() = apiProvider.api
    
    suspend fun search(keyword: String): Result<List<StudySearchItemDto>> = runCatching {
        if (keyword.isBlank()) emptyList() else api.searchWords(keyword.trim())
    }

    suspend fun detail(wordId: Long): Result<StudyDetailDto> = runCatching {
        api.getWordDetail(wordId)
    }
}
