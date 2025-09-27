package com.ssafy.a602.game.play.answer

/**
 * 정답 프레임 데이터
 * @param index 프레임 인덱스
 * @param pose 포즈 특징량 (FloatArray)
 * @param left 왼손 특징량 (FloatArray)
 * @param right 오른손 특징량 (FloatArray)
 */
data class AnswerFrame(
    val index: Int,
    val pose: FloatArray,
    val left: FloatArray,
    val right: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnswerFrame

        if (index != other.index) return false
        if (!pose.contentEquals(other.pose)) return false
        if (!left.contentEquals(other.left)) return false
        if (!right.contentEquals(other.right)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + pose.contentHashCode()
        result = 31 * result + left.contentHashCode()
        result = 31 * result + right.contentHashCode()
        return result
    }
}
