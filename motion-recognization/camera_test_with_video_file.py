import os
import cv2
import time
import numpy as np
import mediapipe as mp

from perpare_models import MediapipeDetector
from utils import hand_feature_vector, arm_feature_vector, cosine_similarity

from dotenv import load_dotenv

load_dotenv()

detector = MediapipeDetector()

COUNTDOWN_SEC = int(os.getenv("COUNTDOWN_SEC"))
VIDEO_PATH = os.getenv("VIDEO_PATH")

target_video = VIDEO_PATH + "/jinglebell_ksl.mp4"


# 3초 카운트 다운 먼저 하기
start_time = time.time()
while True:
    elapsed = time.time() - start_time
    if elapsed >= COUNTDOWN_SEC:
        break

    # 카운트 다운 화면 보여주기
    canvas = 255 * np.ones((480, 640, 3), dtype="uint8")    # 흰 배경
    text = str(COUNTDOWN_SEC - int(elapsed))
    cv2.putText(canvas, text, (270, 250), cv2.FONT_HERSHEY_SIMPLEX, 4, (0, 0, 255), 8, cv2.LINE_AA)
    cv2.imshow("COUNTDOWN", canvas)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        cv2.destroyAllWindows()
        raise SystemExit
    
cv2.destroyWindow("COUNTDOWN")

# 타겟 비디오 열기
cap_video = cv2.VideoCapture(target_video)
cv2.namedWindow("TARGET VIDEO", cv2.WINDOW_NORMAL)
cv2.resizeWindow("TARGET VIDEO", 800, 450)

# 셀프 카메라 열기
cap_self = cv2.VideoCapture(0)
cv2.namedWindow("SELF CAMERA", cv2.WINDOW_NORMAL)
cv2.resizeWindow("SELF CAMERA", 800, 450)

if not cap_video.isOpened():
    raise RuntimeError("타겟 비디오를 열 수 없음.")
if not cap_self.isOpened():
    raise RuntimeError("셀프 카메라를 열 수 없음.")

# 유사도 계산
similarities = {"left": [],
                "right": []}
labels = []

hands_self, pose_self = detector.ready_to_detect_self()
hands_video, pose_video = detector.ready_to_detect_target()

while True:
    ok_video, frame_video = cap_video.read()
    ok_self, frame_self = cap_self.read()

    if not ok_video and not ok_self:
        cap_video.set(cv2.CAP_PROP_POS_FRAMES, 0)
        continue
    
    # flip 없이 처리한다.
    copyed_frame_video = frame_video.copy()
    copyed_frame_self = frame_self.copy()

    # 타겟 비디오
    left_video = right_video = None
    if ok_video:
        H_video, W_video = copyed_frame_video.shape[:2]
        rgb_video = cv2.cvtColor(frame_video, cv2.COLOR_BGR2RGB)
        result_hand_video = hands_video.process(rgb_video)
        if result_hand_video.multi_hand_landmarks and result_hand_video.multi_handedness:
            for landmark, hand in zip(result_hand_video.multi_hand_landmarks, result_hand_video.multi_handedness):
                label = hand.classification[0].label    # 왼/오
                if label == "Left":
                    left_video = (landmark.landmark, W_video, H_video)
                else:
                    right_video = (landmark.landmark, W_video, H_video)

            for landmark in result_hand_video.multi_hand_landmarks:
                detector.mp_drawing.draw_landmarks(copyed_frame_video, landmark, detector.mp_hands.HAND_CONNECTIONS)
    
    # 셀프 카메라
    left_self = right_self = None
    if ok_self:
        H_self, W_self = copyed_frame_self.shape[:2]
        rgb_self = cv2.cvtColor(copyed_frame_self, cv2.COLOR_BGR2RGB)
        result_hand_self = hands_self.process(rgb_self)
        if result_hand_self.multi_hand_landmarks and result_hand_self.multi_handedness:
            for landmark, hand in zip(result_hand_self.multi_hand_landmarks, result_hand_self.multi_handedness):
                label = hand.classification[0].label    # 왼/오
                if label == "Left":
                    left_self = (landmark.landmark, W_self, H_self)
                else:
                    right_self = (landmark.landmark, W_self, H_self)
            for landmark in result_hand_self.multi_hand_landmarks:
                detector.mp_drawing.draw_landmarks(copyed_frame_self, landmark, detector.mp_hands.HAND_CONNECTIONS)

    # 팔
    pose_left_video = pose_right_video = None
    pose_left_self = pose_right_self = None

    if ok_video:
        rgb_video_pose = cv2.cvtColor(copyed_frame_video, cv2.COLOR_BGR2RGB)
        result_pose_video = pose_video.process(rgb_video_pose)
        if result_pose_video.pose_landmarks:
            pose_left_video  = result_pose_video.pose_landmarks.landmark
            pose_right_video  = result_pose_video.pose_landmarks.landmark
            # 그려보기
            detector.mp_drawing.draw_landmarks(copyed_frame_video, result_pose_video.pose_landmarks, detector.mp_pose.POSE_CONNECTIONS)

    if ok_self:
        rgb_self_pose = cv2.cvtColor(copyed_frame_self, cv2.COLOR_BGR2RGB)
        result_pose_self = pose_self.process(rgb_self_pose)
        if result_pose_self.pose_landmarks:
            pose_left_self  = result_pose_self.pose_landmarks.landmark
            pose_right_self = result_pose_self.pose_landmarks.landmark

            detector.mp_drawing.draw_landmarks(copyed_frame_self, result_pose_self.pose_landmarks, detector.mp_pose.POSE_CONNECTIONS)

    # 유사도 계산
    WEIGHT_HAND = 0.7
    WEIGHT_ARM = 0.3

    # 왼손 매칭
    if left_video and left_self and pose_left_video and pose_left_self:
        # 손
        hand_vector_video = hand_feature_vector(left_video[0], left_video[1], left_video[2])
        hand_vector_self = hand_feature_vector(left_self[0], left_self[1], left_self[2])
        similarity_hand = cosine_similarity(hand_vector_video, hand_vector_self)
        score_hand = (similarity_hand + 1) / 2.0

        # 팔
        arm_vector_video = arm_feature_vector(pose_left_video, W_video, H_video, side="Left")
        arm_vector_self = arm_feature_vector(pose_left_self, W_self, H_self, side="Left")
        if arm_vector_video is not None and arm_vector_self is not None:
            similarity_arm = cosine_similarity(arm_vector_video, arm_vector_self)
            score_arm = (similarity_arm + 1) / 2.0
            left_score = WEIGHT_HAND * score_hand + WEIGHT_ARM * score_arm
        else:
            # 팔이 없다면 손만
            left_score = score_hand

        similarities["left"].append(left_score)

        cv2.putText(copyed_frame_video, f"Left SIM: {left_score:.3f}", (30, 50),
            cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 255, 0), 2, cv2.LINE_AA)
    

    # 오른손 매칭
    if right_video and right_self and pose_right_video and pose_right_self:
        # 손
        hand_vector_video = hand_feature_vector(right_video[0], right_video[1], right_video[2])
        hand_vector_self = hand_feature_vector(right_self[0], right_self[1], right_self[2])
        similarity_hand = cosine_similarity(hand_vector_video, hand_vector_self)
        score_hand = (similarity_hand + 1) / 2.0

        # 팔
        arm_vector_video = arm_feature_vector(pose_right_video, W_video, H_video, side="Right")
        arm_vector_self = arm_feature_vector(pose_right_self, W_self, H_self, side="Right")
        if arm_vector_video is not None and arm_vector_self is not None:
            similarity_arm = cosine_similarity(arm_vector_video, arm_vector_self)
            score_arm = (similarity_arm + 1) / 2.0
            right_score = WEIGHT_HAND * score_hand + WEIGHT_ARM * score_arm
        else:
            # 팔이 없다면 손만
            right_score = score_hand

        similarities["right"].append(right_score)

        cv2.putText(copyed_frame_video, f"Right SIM: {right_score:.3f}", (400, 50),
            cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 255, 0), 2, cv2.LINE_AA)

    # 표시
    if ok_self:
        display_self = cv2.flip(copyed_frame_self, 1)
        cv2.imshow("SELF CAMERA", display_self)
    
    if ok_video:
        cv2.imshow("TARGET VIDEO", copyed_frame_video)
    
    print(similarities)

    if cv2.waitKey(1) == ord("q"):
        break


cap_video.release()
cap_self.release()
hands_video.close()
hands_self.close()
cv2.destroyAllWindows()

