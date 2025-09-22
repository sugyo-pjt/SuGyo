package com.ssafy.a602.game.play.input

@kotlinx.serialization.Serializable
data class FramePack(
    val tsMs: Long,
    val pose: List<LM?>,   // pose 0..22 (23) - null 가능
    val left: List<LM?>,   // left hand 0..20 (21) - null 가능
    val right: List<LM?>   // right hand 0..20 (21) - null 가능
) {
    // 전송용 FramePack (tsMs 제외)
    @kotlinx.serialization.Serializable
    data class UploadFrame(
        val pose: List<LM?>,
        val left: List<LM?>,
        val right: List<LM?>
    )
    
    fun toUploadFrame(): UploadFrame = UploadFrame(pose, left, right)
}
