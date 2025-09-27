package com.ssafy.a602.game.play.judge

/**
 * 사용자 프레임 특징량 데이터
 * @param timestampMs 타임스탬프 (ms)
 * @param pose 포즈 특징량
 * @param left 왼손 특징량
 * @param right 오른손 특징량
 */
data class FrameFeature(
    val timestampMs: Long,
    val pose: FloatArray,
    val left: FloatArray,
    val right: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FrameFeature

        if (timestampMs != other.timestampMs) return false
        if (!pose.contentEquals(other.pose)) return false
        if (!left.contentEquals(other.left)) return false
        if (!right.contentEquals(other.right)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestampMs.hashCode()
        result = 31 * result + pose.contentHashCode()
        result = 31 * result + left.contentHashCode()
        result = 31 * result + right.contentHashCode()
        return result
    }
}
