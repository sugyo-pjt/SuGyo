package com.ssafy.a602.game.result

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.GameMode
import com.ssafy.a602.game.play.collector.RhythmCollector
import com.ssafy.a602.game.play.service.RhythmUploadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameResultViewModel @Inject constructor(
    private val rhythmUploadService: RhythmUploadService
) : ViewModel() {
    
    companion object {
        private const val TAG = "GameResultViewModel"
    }
    
    /**
     * Hard 모드일 때 리듬 데이터 업로드
     */
    fun uploadHardModeData() {
        viewModelScope.launch {
            try {
                // 현재 게임 모드 확인
                val currentMode = GameDataManager.getCurrentGameMode()
                Log.d(TAG, "게임 결과 화면: 현재 모드 = $currentMode")
                
                if (currentMode == GameMode.HARD) {
                    val rhythmCollector = GameDataManager.getRhythmCollector()
                    if (rhythmCollector != null) {
                        Log.d(TAG, "🔥 Hard 모드: 리듬 데이터 수집 및 업로드 시작")
                        
                        // 리듬 데이터 수집 완료
                        val rhythmData = rhythmCollector.onSongEnd()
                        Log.d(TAG, "🔥 Hard 모드: 리듬 데이터 수집 결과 - ${if (rhythmData != null) "성공" else "실패"}")
                        
                        if (rhythmData != null) {
                            Log.d(TAG, "🔥 Hard 모드: 리듬 데이터 업로드 시작 - musicId=${rhythmData.musicId}, segments=${rhythmData.allFrames.size}")
                            
                            // 리듬 데이터 업로드 API 호출 (토큰 자동 주입)
                            val uploadResult = rhythmUploadService.uploadRhythmDataWithRetry(
                                request = rhythmData
                            )
                            
                            Log.d(TAG, "🔥 Hard 모드: 리듬 데이터 업로드 결과 - ${if (uploadResult.isSuccess) "성공" else "실패"}")
                            if (uploadResult.isFailure) {
                                Log.e(TAG, "리듬 데이터 업로드 실패", uploadResult.exceptionOrNull())
                            }
                        } else {
                            Log.e(TAG, "리듬 데이터 수집 실패")
                        }
                    } else {
                        Log.d(TAG, "RhythmCollector가 null - 업로드 건너뜀")
                    }
                } else {
                    Log.d(TAG, "Easy 모드 - 업로드 건너뜀")
                }
            } catch (e: Exception) {
                Log.e(TAG, "리듬 데이터 업로드 중 오류 발생", e)
            }
        }
    }
}
