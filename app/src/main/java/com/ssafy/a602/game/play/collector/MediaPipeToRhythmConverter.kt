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
    fun convertToPoses(pose: List<LM>, left: List<LM>, right: List<LM>): List<PoseDto> {
        return listOf(
            // BODY 포즈 (전체 23개 랜드마크)
            PoseDto(
                part = Part.BODY,
                coordinates = pose.map { lm ->
                    CoordinateDto(
                        x = lm.x?.toDouble() ?: 0.0,
                        y = lm.y?.toDouble() ?: 0.0,
                        z = lm.z?.toDouble() ?: 0.0,
                        w = lm.w?.toDouble() ?: 0.0
                    )
                }
            ),
            // LEFT_HAND 포즈 (21개 랜드마크)
            PoseDto(
                part = Part.LEFT_HAND,
                coordinates = left.map { lm ->
                    CoordinateDto(
                        x = lm.x?.toDouble() ?: 0.0,
                        y = lm.y?.toDouble() ?: 0.0,
                        z = lm.z?.toDouble() ?: 0.0,
                        w = lm.w?.toDouble() ?: 0.0
                    )
                }
            ),
            // RIGHT_HAND 포즈 (21개 랜드마크)
            PoseDto(
                part = Part.RIGHT_HAND,
                coordinates = right.map { lm ->
                    CoordinateDto(
                        x = lm.x?.toDouble() ?: 0.0,
                        y = lm.y?.toDouble() ?: 0.0,
                        z = lm.z?.toDouble() ?: 0.0,
                        w = lm.w?.toDouble() ?: 0.0
                    )
                }
            )
        )
    }
}
