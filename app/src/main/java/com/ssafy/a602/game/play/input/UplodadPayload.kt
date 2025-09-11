package com.ssafy.a602.game.play.input

@kotlinx.serialization.Serializable
data class UploadPayload(
    val wordId: String,
    val centerMs: Long,
    val frames: List<FramePack>,
    val schema: Schema = Schema()
) {
    @kotlinx.serialization.Serializable
    data class Schema(
        val poseCount: Int = 23, val handCount: Int = 21, val coords: Int = 3,
        val order: String = "pose,left,right",
        val indices: Map<String, List<Int>> = mapOf(
            "pose" to POSE_KEEP.toList(), "hand" to HAND_KEEP.toList()
        )
    )
}
