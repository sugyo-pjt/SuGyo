package com.ssafy.a602.game.play.collector

import com.ssafy.a602.game.play.dto.*
import com.ssafy.a602.game.play.input.LM

/**
 * MediaPipe 결과를 리듬게임 데이터 형식으로 변환
 */
object MediaPipeToRhythmConverter {
    
    /**
     * MediaPipe LM 리스트를 PoseDto로 변환
     */
    fun convertToPoses(pose: List<LM?>, left: List<LM?>, right: List<LM?>): List<PoseDto> {
        return listOf(
            // BODY 포즈 (전체 23개 랜드마크)
            PoseDto(
                part = "BODY",
                coordinates = pose.map { lm ->
                    lm?.let {
                        CoordinateDto(
                            x = it.x?.toFloat(),
                            y = it.y?.toFloat(),
                            z = it.z?.toFloat(),
                            w = it.w?.toFloat()
                        )
                    } ?: CoordinateDto(x = null, y = null, z = null, w = null)
                }
            ),
            // LEFT_HAND 포즈 (21개 랜드마크)
            PoseDto(
                part = "LEFT_HAND",
                coordinates = left.map { lm ->
                    lm?.let {
                        CoordinateDto(
                            x = it.x?.toFloat(),
                            y = it.y?.toFloat(),
                            z = it.z?.toFloat(),
                            w = it.w?.toFloat()
                        )
                    } ?: CoordinateDto(x = null, y = null, z = null, w = null)
                }
            ),
            // RIGHT_HAND 포즈 (21개 랜드마크)
            PoseDto(
                part = "RIGHT_HAND",
                coordinates = right.map { lm ->
                    lm?.let {
                        CoordinateDto(
                            x = it.x?.toFloat(),
                            y = it.y?.toFloat(),
                            z = it.z?.toFloat(),
                            w = it.w?.toFloat()
                        )
                    } ?: CoordinateDto(x = null, y = null, z = null, w = null)
                }
            )
        )
    }
}
