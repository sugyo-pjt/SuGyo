package com.ssafy.a602

import android.app.Application
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.RealApiService
import com.ssafy.a602.game.api.RhythmApi
import com.ssafy.a602.auth.TokenManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application() {

    @Inject lateinit var realApiService: RealApiService
    @Inject lateinit var rhythmApi: RhythmApi
    @Inject lateinit var tokenManager: TokenManager

    override fun onCreate() {
        super.onCreate()

        // ✅ 디버그 빌드에서는 항상 깨끗한 상태로 시작 (이전 로그인 토큰 혼선 방지)
        if (BuildConfig.DEBUG) {
            tokenManager.clearTokens()
        }

        // (기존 게임 데이터 주입 그대로 유지)
        GameDataManager.injectServices(realApiService, rhythmApi)
    }
}
