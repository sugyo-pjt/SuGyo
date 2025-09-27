package com.ssafy.a602.game.play.input

import kotlinx.serialization.Serializable

val POSE_KEEP = (0..22).toSet()
val HAND_KEEP = (0..20).toSet()

// LM 클래스는 FramesBatch.kt에서 정의됨

fun toLMFiltered(
    src: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    keep: Set<Int>
): List<LM?> {
    if (src.isEmpty()) {
        // 인식된 랜드마크가 없으면 모든 keep 인덱스에 대해 null 반환
        return keep.map { null }
    }
    val max = src.size - 1
    return keep.map { i ->
        if (i in 0..max) {
            val lm = src[i]
            LM(lm.x(), lm.y(), lm.z(), lm.visibility().orElse(0.0f))
        } else {
            null // 인식되지 않은 랜드마크는 null로 반환
        }
    }
}
