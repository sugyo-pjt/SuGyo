package com.ssafy.a602.game.play.judge

import kotlin.math.abs
import kotlin.math.max

/**
 * MediaPipe 손과 포즈 데이터 정규화 및 유사도 계산 클래스
 * 
 * 주요 기능:
 * 1. 손 랜드마크 정규화 (21개 포인트)
 * 2. 팔 특징 벡터 추출 (어깨-팔꿈치-손목)
 * 3. 코사인 유사도 계산
 */
class HandNormalization private constructor() {
    
    companion object {
        private const val EPS: Double = 1e-8

        // MediaPipe Pose 33개 인덱스 기준 (Left / Right)
        private val ARM_IDS: Map<String, IntArray> = hashMapOf(
            "Left" to intArrayOf(11, 13, 15),   // 어깨, 팔꿈치, 손목
            "Right" to intArrayOf(12, 14, 16)
        )

        @JvmStatic
        fun handCoordinatesToXyzArray(handCoordinates: List<Coordinate>, W: Int, H: Int): Array<DoubleArray> {
            val pts = Array(handCoordinates.size) { DoubleArray(3) }
            for (i in handCoordinates.indices) {
                val c = handCoordinates[i]
                if (c == null) {
                    pts[i][0] = 0.0  // x 좌표
                    pts[i][1] = 0.0  // y 좌표
                    pts[i][2] = 0.0  // z 좌표 (깊이)
                } else {
                    pts[i][0] = c.x * W
                    pts[i][1] = c.y * H
                    pts[i][2] = c.z * W
                }
            }
            return pts
        }

        @JvmStatic
        fun safeNormalize(v: DoubleArray): DoubleArray {
            val n = kotlin.math.sqrt(v.sumOf { it * it })
            return if (n > EPS) v.map { it / n }.toDoubleArray() else v.map { it / EPS }.toDoubleArray()
        }

        @JvmStatic
        fun crossProduct(u: DoubleArray, v: DoubleArray): DoubleArray {
            val ux = u[0]; val uy = u[1]; val uz = u[2]
            val vx = v[0]; val vy = v[1]; val vz = v[2]
            return doubleArrayOf(
                uy * vz - uz * vy,
                uz * vx - ux * vz,
                ux * vy - uy * vx
            )
        }

        /**
         * 손 좌표(21x3)를 손목 원점/손바닥 좌표계로 회전/스케일 정규화
         */
        @JvmStatic
        fun normalizeHandXyz(points3d: Array<DoubleArray>): Array<DoubleArray> {
            val normalized = points3d.map { it.copyOf() }.toTypedArray()

            // 1) 손목 기준 평행이동 (0번 랜드마크)
            val wrist = normalized[0]
            for (i in normalized.indices) {
                for (j in 0..2) {
                    normalized[i][j] -= wrist[j]
                }
            }

            // 2) 좌표계 축 구성
            val xDirection = normalized[minOf(9, normalized.size - 1)]   // 손목→중지
            val a = normalized[minOf(5, normalized.size - 1)]            // 검지 MCP
            val b = normalized[minOf(17, normalized.size - 1)]           // 새끼 MCP

            var xAxis = safeNormalize(xDirection)
            var zAxis = safeNormalize(crossProduct(a, b))
            var yAxis = safeNormalize(crossProduct(zAxis, xAxis))
            zAxis = safeNormalize(crossProduct(xAxis, yAxis))

            // 3) 회전행렬 (행에 축 배치)
            val R = arrayOf(xAxis, yAxis, zAxis)

            // 4) 회전 적용
            for (i in normalized.indices) {
                val original = normalized[i].copyOf()
                for (j in 0..2) {
                    normalized[i][j] = 0.0
                    for (k in 0..2) {
                        normalized[i][j] += original[k] * R[j][k]
                    }
                }
            }

            // 5) 단위박스 스케일
            var maxAbs = EPS
            for (i in normalized.indices) {
                val r = normalized[i]
                maxAbs = max(maxAbs, abs(r[0]))
                maxAbs = max(maxAbs, abs(r[1]))
                maxAbs = max(maxAbs, abs(r[2]))
            }
            return if (maxAbs <= EPS) normalized else normalized.map { row -> row.map { it / maxAbs }.toDoubleArray() }.toTypedArray()
        }

        /**
         * 손 특징벡터 (정규화된 21점을 63차원 벡터로 플래튼)
         */
        @JvmStatic
        fun handFeatureVector(handCoordinates: List<Coordinate>, W: Int, H: Int): DoubleArray {
            if (handCoordinates.isEmpty()) return DoubleArray(0)
            val xyz = handCoordinatesToXyzArray(handCoordinates, W, H)
            val norm = normalizeHandXyz(xyz)
            val out = DoubleArray(norm.size * 3)
            var k = 0
            for (i in norm.indices) {
                val r = norm[i]
                out[k++] = r[0]; out[k++] = r[1]; out[k++] = r[2]
            }
            return out
        }

        /**
         * 팔 특징벡터 (상완/하완 2개 방향 벡터)
         */
        @JvmStatic
        fun armFeatureVector(poses: List<Pose>, W: Int, H: Int, side: String): DoubleArray? {
            // BODY 파트 찾기
            val bodyCoordinates = poses.firstOrNull { it.part == BodyPart.BODY }?.coordinates ?: return null
            if (bodyCoordinates.size < 33) return null

            val pts = handCoordinatesToXyzArray(bodyCoordinates, W, H)
            val armIds = ARM_IDS[side] ?: return null

            val shoulderIdx = armIds[0]
            val elbowIdx = armIds[1]
            val wristIdx = armIds[2]

            if (shoulderIdx >= bodyCoordinates.size || elbowIdx >= bodyCoordinates.size || wristIdx >= bodyCoordinates.size) {
                return null
            }

            val s = pts[shoulderIdx]
            val e = pts[elbowIdx]
            val w = pts[wristIdx]

            val upper = safeNormalize(doubleArrayOf(e[0] - s[0], e[1] - s[1], e[2] - s[2])) // 어깨→팔꿈치
            val lower = safeNormalize(doubleArrayOf(w[0] - e[0], w[1] - e[1], w[2] - e[2])) // 팔꿈치→손목

            // 6차원 (두 벡터를 나란히)
            return doubleArrayOf(
                upper[0], upper[1], upper[2],
                lower[0], lower[1], lower[2]
            )
        }

        @JvmStatic
        fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
            require(a.size == b.size) { "Arrays must have the same length" }
            val dot = a.zip(b) { ai, bi -> ai * bi }.sum()
            val na = kotlin.math.sqrt(a.sumOf { it * it }) + EPS
            val nb = kotlin.math.sqrt(b.sumOf { it * it }) + EPS
            return dot / (na * nb)
        }

        // ====== Frame-level ======

        @JvmStatic
        fun calculateArmSimilarity(frame1: MotionFrame, frame2: MotionFrame, side: String, width: Int, height: Int): Double {
            val f1 = armFeatureVector(frame1.poses, width, height, side) ?: return 0.0
            val f2 = armFeatureVector(frame2.poses, width, height, side) ?: return 0.0
            return if (f1.size == f2.size) cosineSimilarity(f1, f2) else 0.0
        }

        @JvmStatic
        fun calculateHandSimilarity(frame1: MotionFrame, frame2: MotionFrame, width: Int, height: Int): Double {
            fun part(frame: MotionFrame, bp: BodyPart): List<Coordinate> =
                frame.poses.firstOrNull { it.part == bp }?.coordinates ?: emptyList()

            val lh1 = part(frame1, BodyPart.LEFT_HAND)
            val rh1 = part(frame1, BodyPart.RIGHT_HAND)
            val lh2 = part(frame2, BodyPart.LEFT_HAND)
            val rh2 = part(frame2, BodyPart.RIGHT_HAND)

            var sum = 0.0
            var cnt = 0
            if (lh1.isNotEmpty() && lh2.isNotEmpty()) { 
                val h1 = handFeatureVector(lh1, width, height)
                val h2 = handFeatureVector(lh2, width, height)
                if (h1.size == h2.size) {
                    sum += cosineSimilarity(h1, h2)
                    cnt++
                }
            }
            if (rh1.isNotEmpty() && rh2.isNotEmpty()) { 
                val h1 = handFeatureVector(rh1, width, height)
                val h2 = handFeatureVector(rh2, width, height)
                if (h1.size == h2.size) {
                    sum += cosineSimilarity(h1, h2)
                    cnt++
                }
            }
            return if (cnt == 0) 0.0 else sum / cnt
        }

        /**
         * 각 프레임 별 유사도 계산
         * (팔 Left/Right, 손 평균 → 최댓값 채택 or 프로젝트 정책에 맞춰 조정)
         */
        @JvmStatic
        fun calculateFrameSimilarity(frame1: MotionFrame, frame2: MotionFrame, width: Int, height: Int): Double {
            val leftArmSimilarity = calculateArmSimilarity(frame1, frame2, "Left", width, height)
            val rightArmSimilarity = calculateArmSimilarity(frame1, frame2, "Right", width, height)
            val handSimilarity = calculateHandSimilarity(frame1, frame2, width, height)

            // 기본 정책: 세 유사도 중 최댓값 사용 (원 자바 구조 준수)
            return max(max(leftArmSimilarity, rightArmSimilarity), handSimilarity)
        }
    }
}

// 필요한 데이터 클래스들
data class Coordinate(
    val x: Double,
    val y: Double,
    val z: Double,
    val w: Double
)

data class Pose(
    val part: BodyPart,
    val coordinates: List<Coordinate>
)

enum class BodyPart {
    BODY, LEFT_HAND, RIGHT_HAND
}
