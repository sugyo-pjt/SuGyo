# 웹캠 실행
import cv2
import mediapipe as mp
import time
import numpy as np
import os
from dotenv import load_dotenv

load_dotenv()

# 모델 학습에 맞는 형태로 변경
from change_to_model_input import change_np_array_from_df
from test_code import pad_or_trim

mp_hands = mp.solutions.hands
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils

ARM_IDS = [11, 13, 15, 12, 14, 16]
HAND_IDS = [17, 19, 21, 18, 20, 22]
FACE_IDS = [n for n in range(0, 11)]

OUTPUT_JSON = "data/mp_annotation.json"

COUNTDOWN_SEC = 3

# 색상
COLOR_L = (255, 128, 0)    # 왼손
COLOR_R = (0, 128, 255)    # 오른손

# 기본 틀 잡기
def build_base():
    json_data = {
        "frames": []
    }
    return json_data


# 좌표 추출
def extract_annotation_to_json(result_pose, result_hand):
    frame_data = {}

    pose_landmarks = []
    left_hand_landmarks = []
    right_hand_landmarks = []

    # pose landmarks
    if result_pose.pose_landmarks:
        for point_idx, point in enumerate(result_pose.pose_landmarks.landmark):
            pose_landmarks.append({"x": point.x, "y": point.y, "z": point.z, "w": float(point.visibility)})
        frame_data["pose"] = pose_landmarks

    # hand landmarks
    if result_hand.multi_hand_landmarks and result_hand.multi_handedness:
        for idx, landmark in enumerate(result_hand.multi_hand_landmarks):
            label = result_hand.multi_handedness[idx].classification[0].label    # Left / Right
            hand_points = []
            for point_idx, point in enumerate(landmark.landmark):
                hand_points.append({"x": point.x, "y": point.y, "z": point.z, "w": 0.0})

            if label == "Left":
                left_hand_landmarks = hand_points
            else:
                right_hand_landmarks = hand_points

    if left_hand_landmarks:
        frame_data["left"] = left_hand_landmarks
    else:
        frame_data["left"] = []
    
    if right_hand_landmarks:
        frame_data["right"] = right_hand_landmarks
    else:
        frame_data["right"] = []
    
    return frame_data


# ----------------------------------------------------   
cap = cv2.VideoCapture(0)

if not cap.isOpened():
    raise RuntimeError("카메라를 열 수 없음.")

# JSON 준비
new_json = build_base()

# 시작 시간 기록
started = False
start_time = time.time()
frame_idx = 0

# 모델 준비 : 포즈, 손
with mp_pose.Pose(
    static_image_mode=False,
    model_complexity=1,
    enable_segmentation=False,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
) as pose, mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=2,
    model_complexity=1,
    min_detection_confidence=0.6,
    min_tracking_confidence=0.6
) as hands:

    while True:
        ok, frame = cap.read()

        if not ok:
            continue

        # 좌우 반전
        frame = cv2.flip(frame, 1)
        copyed_frame = frame.copy()
        H, W = copyed_frame.shape[:2]

        # 경과 시간 계산
        elapsed = int(time.time() - start_time)

        # 카운트 다운
        if not started:
            if elapsed < COUNTDOWN_SEC:
                text = str(COUNTDOWN_SEC - elapsed)
                cv2.putText(copyed_frame, text, (100, 100), cv2.FONT_HERSHEY_SIMPLEX, 3, (0, 255, 0), 5)

            # 3초 이상 지나면 "START" 표시
            else:
                cv2.putText(copyed_frame, "START", (100, 100), cv2.FONT_HERSHEY_SIMPLEX, 3, (0, 0, 255), 5)
                started = True
                frame_idx = 0

        # START 이후만 추출
        if started:
            # BGR -> RGB
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            result_pose = pose.process(image)
            result_hand = hands.process(image)

            image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

            if result_hand.multi_hand_landmarks:
                for hand_landmarks in result_hand.multi_hand_landmarks:
                    mp_drawing.draw_landmarks(
                        copyed_frame, hand_landmarks, mp_hands.HAND_CONNECTIONS
                    )

            if result_pose.pose_landmarks:
                mp_drawing.draw_landmarks(
                    copyed_frame, result_pose.pose_landmarks, mp_pose.POSE_CONNECTIONS
                )

                pose_lm = result_pose.pose_landmarks.landmark
                for point_idx in ARM_IDS:
                    x, y = int(pose_lm[point_idx].x * W), int(pose_lm[point_idx].y * H)
                    cv2.putText(copyed_frame, str(point_idx), (x+8, y-8), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
                
            # 좌표 추출하기
            frame_info = extract_annotation_to_json(result_pose, result_hand)
            new_json["frames"].append(frame_info)
            frame_idx += 1

        cv2.imshow("SELF CAMERA", copyed_frame)

        if cv2.waitKey(1) == ord("q"):
            break

cap.release()

import json

# 파일 저장
with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
    json.dump(new_json, f, ensure_ascii=False, indent=4)

print(f"[저장 완료] {OUTPUT_JSON} | frames: {len(new_json['frames'])}")

# 저장된 JSON 불러오기
with open(OUTPUT_JSON, "r", encoding="utf-8") as f:
    annotation_data = json.load(f)

# 모델 불러오기
MODEL_PATH = os.getenv("MODEL_PATH")
LABEL_ENCODER_PATH = os.getenv("LABEL_ENCODER_PATH")

import pickle
from tensorflow import keras

model = keras.models.load_model(MODEL_PATH)
with open(LABEL_ENCODER_PATH, "rb") as f:
    label_encoder = pickle.load(f)

# 좌표를 배열로 바꾸기
np_data = change_np_array_from_df(annotation_data, use_buckets=True)

# 프레임 길이를 60으로 정규화
X_fixed = pad_or_trim(np_data, target_len=60)   # (60, 195)

sample = np.expand_dims(X_fixed, axis=0)   # (1, 60, 195)
sample = np.expand_dims(sample, axis=-1)   # (1, 60, 195, 1)

# 예측
probs = model.predict(sample)[0]   # (클래스 수,)

# 확률 상위 3개 인덱스
top3_idx = np.argsort(probs)[-3:][::-1]
top3_words = label_encoder.inverse_transform(top3_idx)
top3_probs = probs[top3_idx]

print('X 형태', np_data.shape)
# print("실제 라벨:", y)
print("Top-3 예측:")
for word, p in zip(top3_words, top3_probs):
    print(f"{word}: {p:.4f}")