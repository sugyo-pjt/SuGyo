package com.sugyo.domain.game.service;

import com.sugyo.domain.game.dto.Coordinate;
import com.sugyo.domain.game.dto.Pose;
import com.sugyo.domain.game.domain.BodyPart;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MediaPipe 손과 포즈 데이터 정규화 및 유사도 계산 클래스
 *
 * 주요 기능:
 * 1. 손 랜드마크 정규화 (21개 포인트)
 * 2. 팔 특징 벡터 추출 (어깨-팔꿈치-손목)
 * 3. 코사인 유사도 계산
 */
public class HandNormalization {

    // 수치 계산 안정성을 위한 작은 값
    private static final double EPS = 1e-8;

    // MediaPipe Pose 랜드마크 인덱스 (어깨-팔꿈치-손목)
    private static final Map<String, int[]> ARM_IDS = new HashMap<String, int[]>() {{
        put("Left", new int[]{11, 13, 15});   // 왼쪽: 어깨(11) - 팔꿈치(13) - 손목(15)
        put("Right", new int[]{12, 14, 16});  // 오른쪽: 어깨(12) - 팔꿈치(14) - 손목(16)
    }};

    /**
     * 손 좌표 리스트를 3D 좌표 행렬로 변환
     */
    public static RealMatrix handCoordinatesToXyzArray(List<Coordinate> handCoordinates, int W, int H) {
        double[][] points = new double[handCoordinates.size()][3];
        for (int i = 0; i < handCoordinates.size(); i++) {
            Coordinate coordinate = handCoordinates.get(i);
            if(coordinate == null){
                points[i][0] = 0;  // x 좌표
                points[i][1] = 0;  // y 좌표
                points[i][2] = 0;  // z 좌표 (깊이)
            }else{
                points[i][0] = coordinate.x() * W;  // x 좌표
                points[i][1] = coordinate.y() * H;  // y 좌표
                points[i][2] = coordinate.z() * W;  // z 좌표 (깊이)
            }
        }
        return new Array2DRowRealMatrix(points);
    }

    /**
     * 안전한 벡터 정규화 (0으로 나누기 방지)
     *
     * @param vector 정규화할 벡터
     * @return 단위 벡터
     */
    public static RealVector safeNormalize(RealVector vector) {
        double norm = vector.getNorm();
        if (norm > EPS) {
            return vector.mapDivide(norm);  // 정상적인 정규화
        } else {
            return vector.mapDivide(EPS);   // 0벡터 방지용
        }
    }

    /**
     * 3D 벡터 외적 계산
     * 두 벡터에 수직인 벡터를 구함 (손바닥 법선 벡터 계산용)
     *
     * @param a 첫 번째 벡터
     * @param b 두 번째 벡터
     * @return 외적 결과 벡터
     */
    public static RealVector crossProduct(RealVector a, RealVector b) {
        double[] result = new double[3];
        // 외적 공식: a × b = (a_y*b_z - a_z*b_y, a_z*b_x - a_x*b_z, a_x*b_y - a_y*b_x)
        result[0] = a.getEntry(1) * b.getEntry(2) - a.getEntry(2) * b.getEntry(1);
        result[1] = a.getEntry(2) * b.getEntry(0) - a.getEntry(0) * b.getEntry(2);
        result[2] = a.getEntry(0) * b.getEntry(1) - a.getEntry(1) * b.getEntry(0);
        return new ArrayRealVector(result);
    }

    /**
     * 손 3D 좌표 정규화 (핵심 알고리즘)
     *
     * 과정:
     * 1) 손목 기준 평행 이동 (translation)
     * 2) 손 좌표계 구성 (rotation)
     * 3) 크기 정규화 (scaling)
     *
     * @param points3d 원본 3D 좌표 행렬 (21x3)
     * @return 정규화된 3D 좌표 행렬
     */
    public static RealMatrix normalizeHandXyz(RealMatrix points3d) {
        RealMatrix normalizedPoints = points3d.copy();

        // 1단계: 손목(0번 포인트) 기준으로 평행 이동
        RealVector wrist = normalizedPoints.getRowVector(0);
        for (int i = 0; i < normalizedPoints.getRowDimension(); i++) {
            normalizedPoints.setRowVector(i, normalizedPoints.getRowVector(i).subtract(wrist));
        }

        // 2단계: 손 좌표계 구성을 위한 기준점 설정
        RealVector xDirection = normalizedPoints.getRowVector(9);   // 손목→중지 방향 (x축)
        RealVector a = normalizedPoints.getRowVector(5);            // 검지 MCP
        RealVector b = normalizedPoints.getRowVector(17);           // 새끼 MCP

        // 3단계: 직교 좌표계 구성
        RealVector xAxis = safeNormalize(xDirection);               // x축: 손목→중지 방향
        RealVector zAxis = safeNormalize(crossProduct(a, b));       // z축: 손바닥 법선 벡터
        RealVector yAxis = safeNormalize(crossProduct(zAxis, xAxis)); // y축: x,z에 수직
        zAxis = safeNormalize(crossProduct(xAxis, yAxis));          // z축 재계산 (직교성 보장)

        // 4단계: 회전 행렬 적용
        double[][] rotationMatrix = {
            xAxis.toArray(),   // 새로운 x축
            yAxis.toArray(),   // 새로운 y축
            zAxis.toArray()    // 새로운 z축
        };
        RealMatrix R = new Array2DRowRealMatrix(rotationMatrix).transpose();
        normalizedPoints = normalizedPoints.multiply(R);

        // 5단계: 크기 정규화 (손목→중지 거리로 스케일링)
        double scale = xDirection.getNorm();
        if (scale <= EPS) {
            scale = EPS;  // 0으로 나누기 방지
        }
        normalizedPoints = normalizedPoints.scalarMultiply(1.0 / scale);

        return normalizedPoints;
    }

    /**
     * 손 특징 벡터 추출
     * 정규화된 손 좌표를 1차원 특징 벡터로 변환
     *
     * @param handCoordinates 손 좌표 리스트 (21개)
     * @param W 화면 너비
     * @param H 화면 높이
     * @return 손 특징 벡터 (63차원: 21개 포인트 × 3개 좌표)
     */
    public static double[] handFeatureVector(List<Coordinate> handCoordinates, int W, int H) {
        // 1단계: 픽셀 좌표로 변환
        RealMatrix points3d = handCoordinatesToXyzArray(handCoordinates, W, H);
        // 2단계: 정규화 적용
        RealMatrix normalizedPoints = normalizeHandXyz(points3d);

        // 3단계: 1차원 벡터로 변환 (x1,y1,z1, x2,y2,z2, ...)
        double[] featureVector = new double[normalizedPoints.getRowDimension() * 3];
        for (int i = 0; i < normalizedPoints.getRowDimension(); i++) {
            double[] row = normalizedPoints.getRow(i);
            featureVector[i * 3] = row[0];     // x 좌표
            featureVector[i * 3 + 1] = row[1]; // y 좌표
            featureVector[i * 3 + 2] = row[2]; // z 좌표
        }

        return featureVector;
    }

    /**
     * 팔 특징 벡터 추출
     * Pose 리스트에서 BODY 부분만 추출하여 팔 자세를 특징화
     */
    public static double[] armFeatureVector(List<Pose> poses, int W, int H, String side) {
        if (poses == null || poses.isEmpty()) {
            return null;
        }

        // BODY 부분만 찾기
        List<Coordinate> bodyCoordinates = null;
        for (Pose pose : poses) {
            if (pose.part() == BodyPart.BODY) {
                bodyCoordinates = pose.coordinates();
                break;
            }
        }

        if (bodyCoordinates == null || bodyCoordinates.size() < 33) {
            return null; // 포즈 데이터가 충분하지 않음
        }

        // 포즈 데이터를 픽셀 좌표로 변환
        RealMatrix points = handCoordinatesToXyzArray(bodyCoordinates, W, H);
        int[] armIds = ARM_IDS.get(side);

        if (armIds == null) {
            return null;
        }

        // 팔 관절 포인트 추출
        int shoulderIdx = armIds[0];  // 어깨
        int elbowIdx = armIds[1];     // 팔꿈치
        int wristIdx = armIds[2];     // 손목

        if (shoulderIdx >= bodyCoordinates.size() ||
            elbowIdx >= bodyCoordinates.size() ||
            wristIdx >= bodyCoordinates.size()) {
            return null;
        }

        RealVector sPoint = points.getRowVector(shoulderIdx);
        RealVector ePoint = points.getRowVector(elbowIdx);
        RealVector wPoint = points.getRowVector(wristIdx);

        // 방향 벡터 계산 및 정규화
        RealVector v1 = safeNormalize(ePoint.subtract(sPoint));  // 어깨→팔꿈치 (상완)
        RealVector v2 = safeNormalize(wPoint.subtract(ePoint));  // 팔꿈치→손목 (하완)

        // 6차원 벡터로 결합 [상완x,y,z, 하완x,y,z]
        double[] result = new double[6];
        System.arraycopy(v1.toArray(), 0, result, 0, 3);
        System.arraycopy(v2.toArray(), 0, result, 3, 3);

        return result;
    }

    /**
     * 코사인 유사도 계산
     * 두 벡터 간의 유사도를 -1~1 범위로 계산 (1에 가까울수록 유사)
     *
     * @param a 첫 번째 특징 벡터
     * @param b 두 번째 특징 벡터
     * @return 코사인 유사도 (-1 ~ 1)
     */
    public static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Arrays must have the same length");
        }

        RealVector vectorA = new ArrayRealVector(a);
        RealVector vectorB = new ArrayRealVector(b);

        // 코사인 유사도 공식: cos(θ) = (A·B) / (|A|×|B|)
        double dotProduct = vectorA.dotProduct(vectorB);  // 내적
        double normA = vectorA.getNorm() + EPS;          // A의 크기
        double normB = vectorB.getNorm() + EPS;          // B의 크기

        return dotProduct / (normA * normB);
    }
}