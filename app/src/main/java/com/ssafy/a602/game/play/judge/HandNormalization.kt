package com.ssafy.a602.game.play.judge

import kotlin.math.sqrt

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
        // 수치 계산 안정성을 위한 작은 값
        private const val EPS = 1e-8f

        // MediaPipe Pose 랜드마크 인덱스 (어깨-팔꿈치-손목)
        private val ARM_IDS = mapOf(
            "Left" to intArrayOf(11, 13, 15),   // 왼쪽: 어깨(11) - 팔꿈치(13) - 손목(15)
            "Right" to intArrayOf(12, 14, 16)   // 오른쪽: 어깨(12) - 팔꿈치(14) - 손목(16)
        )

        /**
         * 손 좌표 리스트를 3D 좌표 배열로 변환
         */
        @JvmStatic
        fun handCoordinatesToXyzArray(handCoordinates: List<Coordinate>, W: Int, H: Int): Array<FloatArray> {
            val points = Array(handCoordinates.size) { FloatArray(3) }
            for (i in handCoordinates.indices) {
                val coordinate = handCoordinates[i]
                points[i][0] = (coordinate.x * W).toFloat()  // x 좌표
                points[i][1] = (coordinate.y * H).toFloat()  // y 좌표
                points[i][2] = (coordinate.z * W).toFloat()  // z 좌표 (깊이)
            }
            return points
        }

        /**
         * 안전한 벡터 정규화 (0으로 나누기 방지)
         */
        @JvmStatic
        fun safeNormalize(vector: FloatArray): FloatArray {
            val norm = sqrt(vector.sumOf { (it * it).toDouble() }).toFloat()
            return if (norm > EPS) {
                vector.map { it / norm }.toFloatArray()  // 정상적인 정규화
            } else {
                vector.map { it / EPS }.toFloatArray()   // 0벡터 방지용
            }
        }

        /**
         * 3D 벡터 외적 계산
         */
        @JvmStatic
        fun crossProduct(a: FloatArray, b: FloatArray): FloatArray {
            val result = FloatArray(3)
            // 외적 공식: a × b = (a_y*b_z - a_z*b_y, a_z*b_x - a_x*b_z, a_x*b_y - a_y*b_x)
            result[0] = a[1] * b[2] - a[2] * b[1]
            result[1] = a[2] * b[0] - a[0] * b[2]
            result[2] = a[0] * b[1] - a[1] * b[0]
            return result
        }

        /**
         * 손 3D 좌표 정규화 (핵심 알고리즘)
         */
        @JvmStatic
        fun normalizeHandXyz(points3d: Array<FloatArray>): Array<FloatArray> {
            val normalizedPoints = points3d.map { it.copyOf() }.toTypedArray()

            // 1단계: 손목(0번 포인트) 기준으로 평행 이동
            val wrist = normalizedPoints[0]
            for (i in normalizedPoints.indices) {
                for (j in 0..2) {
                    normalizedPoints[i][j] -= wrist[j]
                }
            }

            // 2단계: 손 좌표계 구성을 위한 기준점 설정
            val xDirection = normalizedPoints[9]   // 손목→중지 방향 (x축)
            val a = normalizedPoints[5]            // 검지 MCP
            val b = normalizedPoints[17]           // 새끼 MCP

            // 3단계: 직교 좌표계 구성
            val xAxis = safeNormalize(xDirection)               // x축: 손목→중지 방향
            val zAxis = safeNormalize(crossProduct(a, b))       // z축: 손바닥 법선 벡터
            val yAxis = safeNormalize(crossProduct(zAxis, xAxis)) // y축: x,z에 수직
            val finalZAxis = safeNormalize(crossProduct(xAxis, yAxis))          // z축 재계산 (직교성 보장)

            // 4단계: 회전 행렬 적용
            val rotationMatrix = arrayOf(
                xAxis,   // 새로운 x축
                yAxis,   // 새로운 y축
                finalZAxis    // 새로운 z축
            )
            
            // 회전 적용
            for (i in normalizedPoints.indices) {
                val original = normalizedPoints[i].copyOf()
                for (j in 0..2) {
                    normalizedPoints[i][j] = 0f
                    for (k in 0..2) {
                        normalizedPoints[i][j] += original[k] * rotationMatrix[j][k]
                    }
                }
            }

            // 5단계: 크기 정규화 (손목→중지 거리로 스케일링)
            val scale = sqrt(xDirection.sumOf { (it * it).toDouble() }).toFloat()
            val finalScale = if (scale <= EPS) EPS else scale  // 0으로 나누기 방지
            for (i in normalizedPoints.indices) {
                for (j in 0..2) {
                    normalizedPoints[i][j] /= finalScale
                }
            }

            return normalizedPoints
        }

        /**
         * 손 특징 벡터 추출
         */
        @JvmStatic
        fun handFeatureVector(handCoordinates: List<Coordinate>, W: Int, H: Int): FloatArray {
            // 1단계: 픽셀 좌표로 변환
            val points3d = handCoordinatesToXyzArray(handCoordinates, W, H)
            // 2단계: 정규화 적용
            val normalizedPoints = normalizeHandXyz(points3d)

            // 3단계: 1차원 벡터로 변환 (x1,y1,z1, x2,y2,z2, ...)
            val featureVector = FloatArray(normalizedPoints.size * 3)
            for (i in normalizedPoints.indices) {
                val row = normalizedPoints[i]
                featureVector[i * 3] = row[0]     // x 좌표
                featureVector[i * 3 + 1] = row[1] // y 좌표
                featureVector[i * 3 + 2] = row[2] // z 좌표
            }

            return featureVector
        }

        /**
         * 팔 특징 벡터 추출
         */
        @JvmStatic
        fun armFeatureVector(poses: List<Pose>, W: Int, H: Int, side: String): FloatArray? {
            if (poses.isEmpty()) {
                return null
            }

            // BODY 부분만 찾기
            var bodyCoordinates: List<Coordinate>? = null
            for (pose in poses) {
                if (pose.part == BodyPart.BODY) {
                    bodyCoordinates = pose.coordinates
                    break
                }
            }

            if (bodyCoordinates == null || bodyCoordinates.size < 33) {
                return null // 포즈 데이터가 충분하지 않음
            }

            // 포즈 데이터를 픽셀 좌표로 변환
            val points = handCoordinatesToXyzArray(bodyCoordinates, W, H)
            val armIds = ARM_IDS[side] ?: return null

            // 팔 관절 포인트 추출
            val shoulderIdx = armIds[0]  // 어깨
            val elbowIdx = armIds[1]     // 팔꿈치
            val wristIdx = armIds[2]     // 손목

            if (shoulderIdx >= bodyCoordinates.size ||
                elbowIdx >= bodyCoordinates.size ||
                wristIdx >= bodyCoordinates.size) {
                return null
            }

            val sPoint = points[shoulderIdx]
            val ePoint = points[elbowIdx]
            val wPoint = points[wristIdx]

            // 방향 벡터 계산 및 정규화
            val v1 = safeNormalize(floatArrayOf(
                ePoint[0] - sPoint[0],
                ePoint[1] - sPoint[1],
                ePoint[2] - sPoint[2]
            ))  // 어깨→팔꿈치 (상완)
            val v2 = safeNormalize(floatArrayOf(
                wPoint[0] - ePoint[0],
                wPoint[1] - ePoint[1],
                wPoint[2] - ePoint[2]
            ))  // 팔꿈치→손목 (하완)

            // 6차원 벡터로 결합 [상완x,y,z, 하완x,y,z]
            val result = FloatArray(6)
            System.arraycopy(v1, 0, result, 0, 3)
            System.arraycopy(v2, 0, result, 3, 3)

            return result
        }

        /**
         * 코사인 유사도 계산
         */
        @JvmStatic
        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size) {
                throw IllegalArgumentException("Arrays must have the same length")
            }

            // 코사인 유사도 공식: cos(θ) = (A·B) / (|A|×|B|)
            val dotProduct = a.zip(b) { ai, bi -> ai * bi }.sum()
            val normA = sqrt(a.sumOf { (it * it).toDouble() }).toFloat() + EPS          // A의 크기
            val normB = sqrt(b.sumOf { (it * it).toDouble() }).toFloat() + EPS          // B의 크기

            return dotProduct / (normA * normB)
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
