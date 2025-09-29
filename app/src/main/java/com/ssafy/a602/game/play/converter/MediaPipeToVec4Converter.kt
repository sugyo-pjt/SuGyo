package com.ssafy.a602.game.play.converter

import com.ssafy.a602.game.play.dto.Vec4
import com.ssafy.a602.game.play.input.LM
import android.util.Log

/**
 * MediaPipe → Vec4 변환 & 좌우/미러 보정
 * 전면 카메라 미러링으로 서버와 좌표계가 다를 수 있어 서버의 HandNormalization 좌표 전제와 동일하게 맞춤
 */
object MediaPipeToVec4Converter {
    private const val TAG = "MediaPipeToVec4Converter"
    
    /**
     * MediaPipe Landmark 리스트를 Vec4 리스트로 변환
     */
    fun toVec4List(points: List<LM?>, mirrorX: Boolean = true): List<Vec4?> {
        return points.map { p ->
            p?.let { landmark ->
                // null 체크 및 기본값 설정
                val x = landmark.x ?: 0f
                val y = landmark.y ?: 0f
                val z = landmark.z ?: 0f
                val w = landmark.w ?: 1f
                
                val mirroredX = if (mirrorX) (1f - x) else x
                Vec4(
                    x = mirroredX,
                    y = y,
                    z = z,
                    w = w
                )
            }
        }
    }
    
    /**
     * MediaPipe 좌표를 서버 기준에 맞게 변환
     * - 전면 카메라 미러링 적용
     * - 좌우 손 스왑 (필요시)
     */
    fun convertForServer(
        bodyLm: List<LM?>,
        leftLm: List<LM?>,
        rightLm: List<LM?>,
        mirrorX: Boolean = true,
        swapHands: Boolean = false
    ): Triple<List<Vec4?>, List<Vec4?>, List<Vec4?>> {
        val body = toVec4List(bodyLm, mirrorX)
        val left = toVec4List(if (swapHands) rightLm else leftLm, mirrorX)
        val right = toVec4List(if (swapHands) leftLm else rightLm, mirrorX)
        
        Log.v(TAG, "좌표 변환: body=${body.size}, left=${left.size}, right=${right.size}")
        
        return Triple(body, left, right)
    }
}
