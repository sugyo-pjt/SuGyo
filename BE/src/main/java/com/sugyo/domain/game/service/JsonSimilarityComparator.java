package com.sugyo.domain.game.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sugyo.domain.game.dto.Coordinate;
import com.sugyo.domain.game.dto.Pose;
import com.sugyo.domain.game.dto.MotionFrame;
import com.sugyo.domain.game.domain.BodyPart;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 동작 과정:
 * JSON → MotionFrame → HandNormalization → 특징벡터 → 코사인유사도
 */
public class JsonSimilarityComparator {

    /**
     * 0.3초 프레임 전부에 대한 최고 유사도 계산
     */
    public static double calculateMotionSimilarity(List<MotionFrame> againstChart, List<MotionFrame> correctChart, int width, int height) {
        int minFrames = Math.min(againstChart.size(), correctChart.size());
        if (minFrames == 0) {
            return 0.0;
        }

        double maxSimilarity = 0.0;
        for (int i = 0; i < againstChart.size(); i++) {
            for(int j = 0; j < correctChart.size(); j++) {
                double frameSimilarity = calculateFrameSimilarity(againstChart.get(i), correctChart.get(i), width, height);
                maxSimilarity =Math.max(frameSimilarity, maxSimilarity);
            }
        }

        return maxSimilarity;
    }

    /**
     * 각 프레임 별 유사도 계산
     */
    public static double calculateFrameSimilarity(MotionFrame frame1, MotionFrame frame2, int width, int height) {
        // 각 신체 부위별 유사도 계산 (왼팔,오른팔,손(왼손,오른손))
        double leftArmSimilarity = calculateArmSimilarity(frame1, frame2, "Left", width, height);
        double rightArmSimilarity = calculateArmSimilarity(frame1, frame2, "Right", width, height);
        double handSimilarity = calculateHandSimilarity(frame1, frame2, width, height);

        // 가중 평균: 팔(왼40% + 오른40%) + 손(20%)
        double totalSimilarity = (leftArmSimilarity * 0.2 + rightArmSimilarity * 0.2 + handSimilarity * 0.6);

        return totalSimilarity;
    }


    /**
     * 한 프레임에 대한 팔(관절) 유사도 계산
     */
    public static double calculateArmSimilarity(MotionFrame frame1, MotionFrame frame2, String side, int width, int height) {
        List<Pose> poses1 = frame1.poses();
        List<Pose> poses2 = frame2.poses();

        double[] armFeature1 = HandNormalization.armFeatureVector(poses1, width, height, side);
        double[] armFeature2 = HandNormalization.armFeatureVector(poses2, width, height, side);

        if (armFeature1 == null || armFeature2 == null) {
            return 0.0;
        }

        return HandNormalization.cosineSimilarity(armFeature1, armFeature2);
    }

    /**
     * MotionFrame에서 특정 신체 부위의 좌표를 추출하는 헬퍼 메서드
     */
    private static List<Coordinate> getHandCoordinates(MotionFrame frame, BodyPart bodyPart) {
        for (Pose pose : frame.poses()) {
            if (pose.part() == bodyPart) {
                return pose.coordinates();
            }
        }
        return new ArrayList<>();
    }

    /**
     * 한 프레임에 대한 손(왼손,오른손) 유사도 계산
     */
    public static double calculateHandSimilarity(MotionFrame frame1, MotionFrame frame2, int width, int height) {
        // 각 프레임에서 손 좌표 추출
        List<Coordinate> leftHand1 = getHandCoordinates(frame1, BodyPart.LEFT_HAND);
        List<Coordinate> rightHand1 = getHandCoordinates(frame1, BodyPart.RIGHT_HAND);
        List<Coordinate> leftHand2 = getHandCoordinates(frame2, BodyPart.LEFT_HAND);
        List<Coordinate> rightHand2 = getHandCoordinates(frame2, BodyPart.RIGHT_HAND);

        // 각 손의 유사도 계산
        double leftSimilarity = calculateSingleHandSimilarity(leftHand1, leftHand2, width, height);
        double rightSimilarity = calculateSingleHandSimilarity(rightHand1, rightHand2, width, height);

        return (leftSimilarity + rightSimilarity) / 2.0;
    }

    /**
     * 한 손에 대한 유사도 계산
     */
    private static double calculateSingleHandSimilarity(List<Coordinate> hand1, List<Coordinate> hand2, int width, int height) {
        if (hand1 == null || hand2 == null || hand1.isEmpty() || hand2.isEmpty()) {
            return 0.0;
        }

        if (hand1.size() != hand2.size()) {
            return 0.0;
        }

        double[] handFeature1 = HandNormalization.handFeatureVector(hand1, width, height);
        double[] handFeature2 = HandNormalization.handFeatureVector(hand2, width, height);

        return HandNormalization.cosineSimilarity(handFeature1, handFeature2);
    }

    /**
     * JSON 파일에서 MotionFrame 리스트로 변환하는 유틸리티 메서드 (테스트용)
     */
//    public static List<MotionFrame> loadMotionFramesFromJson(String filePath) throws IOException {
//        JsonNode rootNode = objectMapper.readTree(new File(filePath));
//        List<MotionFrame> motionFrames = new ArrayList<>();
//
//        // 모든 프레임 데이터 추출
//        JsonNode framesNode = rootNode.get("frames");
//        for (JsonNode frameNode : framesNode) {
//            List<Pose> poses = new ArrayList<>();
//
//            // 1. Pose 데이터 추출 (BODY)
//            JsonNode poseNode = frameNode.get("pose");
//            if (poseNode != null) {
//                List<Coordinate> bodyCoordinates = new ArrayList<>();
//                for (JsonNode posePointNode : poseNode) {
//                    double x = posePointNode.get("x").asDouble();
//                    double y = posePointNode.get("y").asDouble();
//                    double z = posePointNode.get("z").asDouble();
//                    double w = posePointNode.get("w").asDouble();
//                    bodyCoordinates.add(new Coordinate(x, y, z, w));
//                }
//                poses.add(new Pose(BodyPart.BODY, bodyCoordinates));
//            }
//
//            // 2. Left 손 데이터 추출
//            JsonNode leftNode = frameNode.get("left");
//            if (leftNode != null) {
//                List<Coordinate> leftCoordinates = new ArrayList<>();
//                for (JsonNode leftPointNode : leftNode) {
//                    double x = leftPointNode.get("x").asDouble();
//                    double y = leftPointNode.get("y").asDouble();
//                    double z = leftPointNode.get("z").asDouble();
//                    double w = leftPointNode.get("w").asDouble();
//                    leftCoordinates.add(new Coordinate(x, y, z, w));
//                }
//                poses.add(new Pose(BodyPart.LEFT_HAND, leftCoordinates));
//            }
//
//            // 3. Right 손 데이터 추출
//            JsonNode rightNode = frameNode.get("right");
//            if (rightNode != null) {
//                List<Coordinate> rightCoordinates = new ArrayList<>();
//                for (JsonNode rightPointNode : rightNode) {
//                    double x = rightPointNode.get("x").asDouble();
//                    double y = rightPointNode.get("y").asDouble();
//                    double z = rightPointNode.get("z").asDouble();
//                    double w = rightPointNode.get("w").asDouble();
//                    rightCoordinates.add(new Coordinate(x, y, z, w));
//                }
//                poses.add(new Pose(BodyPart.RIGHT_HAND, rightCoordinates));
//            }
//
//            // MotionFrame 생성 (frame 번호는 인덱스 기반으로 생성)
//            motionFrames.add(new MotionFrame(motionFrames.size() + 1, poses));
//        }
//
//        return motionFrames;
//    }

    /**
     * JSON 파일들 비교 (새로운 DTO 사용)
     */
//    public static void compareJsonFiles(String file1Path, String file2Path) {
//        try {
//            System.out.println("=== JSON 파일 유사도 비교 ===");
//            System.out.println("파일 1: " + file1Path);
//            System.out.println("파일 2: " + file2Path);
//            System.out.println();
//
//            List<MotionFrame> frames1 = loadMotionFramesFromJson(file1Path);
//            List<MotionFrame> frames2 = loadMotionFramesFromJson(file2Path);
//
//            System.out.println("파일 1 정보:");
//            System.out.println("  프레임 수: " + frames1.size());
//            if (!frames1.isEmpty()) {
//                MotionFrame firstFrame1 = frames1.get(0);
//                System.out.println("  포즈 수: " + firstFrame1.poses().size());
//            }
//
//            System.out.println("\n파일 2 정보:");
//            System.out.println("  프레임 수: " + frames2.size());
//            if (!frames2.isEmpty()) {
//                MotionFrame firstFrame2 = frames2.get(0);
//                System.out.println("  포즈 수: " + firstFrame2.poses().size());
//            }
//
//            int width = 640;
//            int height = 480;
//
//            double overallSimilarity = calculateMotionSimilarity(frames1, frames2, width, height);
//            System.out.println("\n전체 모션 유사도: " + String.format("%.4f", overallSimilarity));
//
//            if (overallSimilarity > 0.95) {
//                System.out.println("매우 유사한 모션입니다!");
//            } else if (overallSimilarity > 0.85) {
//                System.out.println("유사한 모션입니다.");
//            } else if (overallSimilarity > 0.7) {
//                System.out.println("어느 정도 유사한 모션입니다.");
//            } else {
//                System.out.println("다른 모션입니다.");
//            }
//
//            // 첫 10개 프레임의 개별 유사도 출력
//            int framesToShow = Math.min(10, Math.min(frames1.size(), frames2.size()));
//            if (framesToShow > 0) {
//                System.out.println("\n첫 " + framesToShow + "개 프레임 유사도:");
//                for (int i = 0; i < framesToShow; i++) {
//                    double frameSimilarity = calculateFrameSimilarity(frames1.get(i), frames2.get(i), width, height);
//                    System.out.println("  프레임 " + (i + 1) + ": " + String.format("%.4f", frameSimilarity));
//                }
//            }
//
//        } catch (IOException e) {
//            System.err.println("JSON 파일 읽기 오류: " + e.getMessage());
//        }
//    }

}