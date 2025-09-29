import os
import cv2
import time
import numpy as np
import mediapipe as mp

from extract_annotation import extract_annotation_to_json
from perpare_models import MediapipeDetector


from dotenv import load_dotenv

load_dotenv()

detector = MediapipeDetector()

VIDEO_PATH = os.getenv("VIDEO_PATH")

target_video = VIDEO_PATH + "/snow_ksl.mp4"


# 타겟 비디오 열기
cap_video = cv2.VideoCapture(target_video)
cv2.namedWindow("TARGET VIDEO", cv2.WINDOW_NORMAL)
cv2.resizeWindow("TARGET VIDEO", 1280, 720)


if not cap_video.isOpened():
    raise RuntimeError("타겟 비디오를 열 수 없음.")

# 프레임 저장할 딕셔너리
frames = {"frames":[]}

hands_video, pose_video = detector.ready_to_detect_target()

while True:
    ok_video, frame_video = cap_video.read()

    if not ok_video:
        break
        

    # 타겟 비디오
    left_video = right_video = None
    if ok_video:
        H_video, W_video = frame_video.shape[:2]
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
                detector.mp_drawing.draw_landmarks(frame_video, landmark, detector.mp_hands.HAND_CONNECTIONS)
    
    # 팔
    pose_left_video = pose_right_video = None

    if ok_video:
        rgb_video_pose = cv2.cvtColor(frame_video, cv2.COLOR_BGR2RGB)
        result_pose_video = pose_video.process(rgb_video_pose)
        if result_pose_video.pose_landmarks:
            pose_left_video  = result_pose_video.pose_landmarks.landmark
            pose_right_video  = result_pose_video.pose_landmarks.landmark
            # 그려보기
            detector.mp_drawing.draw_landmarks(frame_video, result_pose_video.pose_landmarks, detector.mp_pose.POSE_CONNECTIONS)

    frame_data = extract_annotation_to_json(result_pose_video, result_hand_video)
    frames["frames"].append(frame_data)

    if ok_video:
        cv2.imshow("TARGET VIDEO", frame_video)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap_video.release()
cv2.destroyAllWindows()

import json

SAVE_PATH = target_video.strip(".mp4") + ".json"

with open(SAVE_PATH, "w", encoding="utf-8") as data:
    json.dump(frames, data, ensure_ascii=False, indent=4)



