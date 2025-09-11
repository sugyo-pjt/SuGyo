package com.ssafy.a602.game.play.input

import kotlinx.serialization.Serializable

val POSE_KEEP = (0..22).toSet()
val HAND_KEEP = (0..20).toSet()

@Serializable
data class LM(val x: Float, val y: Float, val z: Float?, val w: Float? = null)

fun toLMFiltered(
    src: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    keep: Set<Int>
): List<LM> {
    if (src.isEmpty()) return emptyList()
    val max = src.size - 1
    return keep.asSequence()
        .filter { it in 0..max }
        .map { i ->
            val lm = src[i]
            LM(lm.x(), lm.y(), lm.z(), lm.visibility().orElse(null))
        }.toList()
}
