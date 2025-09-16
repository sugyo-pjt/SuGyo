package com.ssafy.a602

import android.app.Application
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.RealApiService
import com.ssafy.a602.game.api.RhythmApi
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Hilt를 사용하기 위한 Application 클래스
 */
@HiltAndroidApp
class HiltApplication : Application() {
    
    @Inject
    lateinit var realApiService: RealApiService
    
    @Inject
    lateinit var rhythmApi: RhythmApi
    
    override fun onCreate() {
        super.onCreate()
        
        // GameDataManager에 서비스들 주입
        GameDataManager.injectServices(realApiService, rhythmApi)
    }
}
