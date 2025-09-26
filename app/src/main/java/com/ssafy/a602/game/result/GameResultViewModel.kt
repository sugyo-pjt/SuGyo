package com.ssafy.a602.game.result

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.a602.game.data.GameDataManager
import com.ssafy.a602.game.data.GameMode
import com.ssafy.a602.game.play.collector.RhythmCollector
import com.ssafy.a602.game.play.dto.RhythmSaveRequest
import com.ssafy.a602.game.play.service.RhythmUploadService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class GameResultViewModel @Inject constructor(
    application: Application,
    private val rhythmUploadService: RhythmUploadService
) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "GameResultViewModel"
    }
    
    /**
     * Hard 모드와 채보만들기 모드일 때 리듬 데이터 업로드
     */
    fun uploadHardModeData() {
        viewModelScope.launch {
            try {
                // 현재 게임 모드 확인
                val currentMode = GameDataManager.getCurrentGameMode()
                Log.d(TAG, "게임 결과 화면: 현재 모드 = $currentMode")
                
                    if (currentMode == GameMode.CHART_CREATION) {
                        Log.d(TAG, "🎵 채보만들기 모드: MediaCodec 기반 일괄 처리 및 업로드 시작")

                        // 현재 선택된 곡의 ID 가져오기
                        val currentSong = GameDataManager.currentSong.value
                        val actualMusicId = currentSong?.id?.toIntOrNull() ?: 1
                        val actualMusicIdString = currentSong?.id ?: "1"
                        
                        Log.d(TAG, "현재 곡: ${currentSong?.title}, ID: $actualMusicIdString")

                        // ViewModel 기반 녹화 파일 확인
                        Log.d(TAG, "🎵 채보만들기 모드: ViewModel 기반 녹화 파일 확인")
                        
                        // 실제 녹화된 파일이 있는지 확인 (songId와 actualMusicId 모두 확인)
                        val application = getApplication<Application>()
                        val cacheDir = application.cacheDir
                        
                        // songId와 actualMusicId 모두 확인
                        val outputFile1 = File(cacheDir, "chart_creation_${actualMusicIdString}.mp4")
                        val outputFile2 = File(cacheDir, "chart_creation_${actualMusicId}.mp4")
                        
                        val outputFile = when {
                            outputFile1.exists() -> {
                                Log.d(TAG, "🎵 채보만들기 모드: songId 기반 파일 발견 - ${outputFile1.absolutePath}")
                                outputFile1
                            }
                            outputFile2.exists() -> {
                                Log.d(TAG, "🎵 채보만들기 모드: musicId 기반 파일 발견 - ${outputFile2.absolutePath}")
                                outputFile2
                            }
                            else -> null
                        }
                        
                        if (outputFile != null) {
                            Log.d(TAG, "🎵 채보만들기 모드: 실제 녹화 파일 발견 - ${outputFile.absolutePath}")
                            val videoUri = Uri.fromFile(outputFile)
                            processAndUpload(actualMusicId, videoUri)
                        } else {
                            Log.e(TAG, "🎵 채보만들기 모드: 녹화 파일이 없음")
                            Log.e(TAG, "확인한 경로들:")
                            Log.e(TAG, "  - ${outputFile1.absolutePath}")
                            Log.e(TAG, "  - ${outputFile2.absolutePath}")
                            Log.e(TAG, "⚠️ 실제 녹화가 진행되지 않았습니다. UI에서 CameraX 바인딩 및 녹화 시작이 필요합니다.")
                        }
                } else {
                    Log.d(TAG, "${currentMode?.displayName ?: "알 수 없는"} 모드 - 업로드 건너뜀")
                }
            } catch (e: Exception) {
                Log.e(TAG, "리듬 데이터 업로드 중 오류 발생", e)
            }
        }
    }

    /**
     * 채보만들기 모드: 녹화 완료 후 MediaCodec 분석 및 업로드
     * @param musicId 음악 ID
     * @param videoUri 녹화된 비디오 파일 Uri
     */
    fun processAndUpload(musicId: Int, videoUri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 채보만들기 모드: MediaCodec 분석 시작 - musicId=$musicId, uri=$videoUri")
            
            val landmarker = com.ssafy.a602.game.play.capture.LandmarkerManager(getApplication())
            
            // ViewModel 기반 MediaCodec 분석 (실제 구현)
            Log.d(TAG, "🎵 채보만들기 모드: MediaCodec 분석 시작")
            
            // 실제 MediaCodecFrameAnalyzer 사용
            val analyzer = com.ssafy.a602.game.play.capture.MediaCodecFrameAnalyzer()
            
            val segments = analyzer.extractAndAnalyzeWithMediaCodec(
                context = getApplication(),
                videoUri = videoUri,
                poseLandmarker = landmarker.pose,
                handLandmarker = landmarker.hand,
                onProgress = { count, ms -> 
                    Log.d(TAG, "프레임 분석 진행: $count, ${ms}ms")
                }
            )
            
            // RhythmSaveRequest 생성
            val rhythmData = RhythmSaveRequest(
                musicId = musicId,
                allFrames = segments
            )
            
            landmarker.close()
            Log.d(TAG, "🎵 채보만들기 모드: MediaCodec 분석 완료 - musicId=${rhythmData.musicId}, segments=${rhythmData.allFrames.size}")

            // 리듬 데이터 업로드 API 호출
            val uploadResult = rhythmUploadService.uploadRhythmDataWithRetry(
                request = rhythmData
            )

            Log.d(TAG, "🎵 채보만들기 모드: 리듬 데이터 업로드 결과 - ${if (uploadResult.isSuccess) "성공" else "실패"}")
            if (uploadResult.isFailure) {
                Log.e(TAG, "리듬 데이터 업로드 실패", uploadResult.exceptionOrNull())
            }
        } catch (e: Exception) {
            Log.e(TAG, "채보만들기 모드 처리 중 오류 발생", e)
        }
    }
}
