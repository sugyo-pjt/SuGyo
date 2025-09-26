# 웹캠 실행
import cv2
import mediapipe as mp
import time
import numpy as np
import os
import json
import pickle
from dotenv import load_dotenv
from tensorflow import keras

load_dotenv()

# -----------------------------
ARM_IDS = [11, 13, 15, 12, 14, 16]      # 팔 6
FACE_IDS = [n for n in range(0, 11)]    # 얼굴 11

NUM_JOINTS = 65
NUM_COORDS = 3
FEATURE_DIM = NUM_JOINTS * NUM_COORDS   # 195


def change_np_array_from_df(annotation_data, use_buckets=True):
    """
    JSON(annotation_data)을 numpy array로 변환
    출력 shape: (frames, 195)
    """
    frames = annotation_data["frames"]
    rows = []

    for frame in frames:
        coords = []

        # --- Pose landmarks (ARM + FACE) ---
        if "pose" in frame:
            # ARM (6개)
            for j in ARM_IDS:
                if j < len(frame["pose"]):
                    coords.extend([frame["pose"][j]["x"],
                                   frame["pose"][j]["y"],
                                   frame["pose"][j]["z"]])
                else:
                    coords.extend([0.0, 0.0, 0.0])

            # FACE (11개)
            for j in FACE_IDS:
                if j < len(frame["pose"]):
                    coords.extend([frame["pose"][j]["x"],
                                   frame["pose"][j]["y"],
                                   frame["pose"][j]["z"]])
                else:
                    coords.extend([0.0, 0.0, 0.0])
        else:
            coords.extend([0.0, 0.0, 0.0] * (len(ARM_IDS) + len(FACE_IDS)))

        # --- Hand landmarks ---
        left_hand = frame.get("left", [])
        right_hand = frame.get("right", [])

        # 대표 landmark 3개씩 (좌우 합 6개)
        for idx in [17, 19, 21]:  # 왼손 대표
            if idx < len(left_hand):
                coords.extend([left_hand[idx]["x"], left_hand[idx]["y"], left_hand[idx]["z"]])
            else:
                coords.extend([0.0, 0.0, 0.0])
        for idx in [18, 20, 22]:  # 오른손 대표
            if idx < len(right_hand):
                coords.extend([right_hand[idx]["x"], right_hand[idx]["y"], right_hand[idx]["z"]])
            else:
                coords.extend([0.0, 0.0, 0.0])

        # 세부 landmark 21개씩 (좌우 합 42개)
        for idx in range(21):
            if idx < len(left_hand):
                coords.extend([left_hand[idx]["x"], left_hand[idx]["y"], left_hand[idx]["z"]])
            else:
                coords.extend([0.0, 0.0, 0.0])

            if idx < len(right_hand):
                coords.extend([right_hand[idx]["x"], right_hand[idx]["y"], right_hand[idx]["z"]])
            else:
                coords.extend([0.0, 0.0, 0.0])

        # --- 길이 보정 ---
        if len(coords) != FEATURE_DIM:
            print(f"[WARN] frame 길이 불일치: {len(coords)} / {FEATURE_DIM}")
            if len(coords) < FEATURE_DIM:
                coords.extend([0.0] * (FEATURE_DIM - len(coords)))
            else:
                coords = coords[:FEATURE_DIM]

        rows.append(coords)

    return np.array(rows, dtype=np.float32)  # (frames, 195)






def pad_or_trim(np_data, target_len=90):
    """
    시퀀스를 target_len으로 맞춤
    - 짧으면 padding, 길면 trimming
    """
    num_frames, feat_dim = np_data.shape

    if num_frames == target_len:
        return np_data
    if num_frames < target_len:
        pad_len = target_len - num_frames
        pad = np.zeros((pad_len, feat_dim), dtype=np.float32)
        return np.concatenate([np_data, pad], axis=0)
    if num_frames > target_len:
        return np_data[:target_len]


def preprocess_for_3dcnn(annotation_data, target_len=90):
    """
    최종적으로 3D CNN 입력 형태로 변환
    출력: (1, target_len, 23, 3, 1)
    """
    np_data = change_np_array_from_df(annotation_data)  # (frames, 69)
    X_fixed = pad_or_trim(np_data, target_len=target_len)  # (90, 69)

    # (90, 69) → (90, 23, 3)
    X_fixed = X_fixed.reshape((target_len, NUM_JOINTS, NUM_COORDS))

    # (1, 90, 23, 3, 1)
    sample = np.expand_dims(X_fixed, axis=0)
    sample = np.expand_dims(sample, axis=-1)
    return sample

# -----------------------------
# Mediapipe 설정
mp_hands = mp.solutions.hands
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils

OUTPUT_JSON = "data/record_test.json"
OUTPUT_MP4 = "data/video/record_test.mp4"

COUNTDOWN_SEC = 3
RECORD_SEC = 4  # 촬영 시간 (초)

FRAME_W = 720
FRAME_H = 1080

def build_base():
    return {"frames": []}


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
            label = result_hand.multi_handedness[idx].classification[0].label
            hand_points = []
            for point_idx, point in enumerate(landmark.landmark):
                hand_points.append({"x": point.x, "y": point.y, "z": point.z, "w": 0.0})
            if label == "Left":
                left_hand_landmarks = hand_points
            else:
                right_hand_landmarks = hand_points

    frame_data["left"] = left_hand_landmarks if left_hand_landmarks else []
    frame_data["right"] = right_hand_landmarks if right_hand_landmarks else []

    return frame_data


# -----------------------------
# 웹캠 실행
cap = cv2.VideoCapture(0)
cv2.namedWindow("SELF CAMERA", cv2.WINDOW_NORMAL)
cv2.resizeWindow("SELF CAMERA", FRAME_W, FRAME_H)

if not cap.isOpened():
    raise RuntimeError("카메라를 열 수 없음.")

new_json = build_base()
start_time = time.time()
started = False

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

        elapsed = int(time.time() - start_time)

        if not started:
            # 카운트다운
            if elapsed < COUNTDOWN_SEC:
                text = str(COUNTDOWN_SEC - elapsed)
                cv2.putText(frame, text, (100, 100),
                            cv2.FONT_HERSHEY_SIMPLEX, 3, (0, 255, 0), 5)
            else:
                cv2.putText(frame, "START", (100, 100),
                            cv2.FONT_HERSHEY_SIMPLEX, 3, (0, 0, 255), 5)
                started = True
                record_start = time.time()

        elif started:
            # 4초 후 종료
            if time.time() - record_start > RECORD_SEC:
                break

            # BGR -> RGB
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            result_pose = pose.process(image)
            result_hand = hands.process(image)

            # 좌표 추출
            frame_info = extract_annotation_to_json(result_pose, result_hand)
            new_json["frames"].append(frame_info)

        display = cv2.flip(frame, 1)
        cv2.imshow("SELF CAMERA", display)

        if cv2.waitKey(1) == ord("q"):
            break

cap.release()
cv2.destroyAllWindows()

# -----------------------------
# JSON 저장
with open(OUTPUT_JSON, "w", encoding="utf-8") as f:
    json.dump(new_json, f, ensure_ascii=False, indent=4)

print(f"[저장 완료] {OUTPUT_JSON} | frames: {len(new_json['frames'])}")

# -----------------------------
# 모델 불러오기 및 예측
MODEL_PATH = os.getenv("MODEL_PATH")
LABEL_ENCODER_PATH = os.getenv("LABEL_ENCODER_PATH")

model = keras.models.load_model(MODEL_PATH)

with open(LABEL_ENCODER_PATH, "rb") as f:
    label_encoder = pickle.load(f)

# 입력 변환
sample = preprocess_for_3dcnn(new_json, target_len=90)
print("입력 shape:", sample.shape)  # (1, 90, 65, 3, 1)

# 예측
probs = model.predict(sample)[0]
top3_idx = np.argsort(probs)[-5:][::-1]
top3_words = label_encoder.inverse_transform(top3_idx)
top3_probs = probs[top3_idx]

print("Top-3 예측:")
for word, p in zip(top3_words, top3_probs):
    print(f"{word}: {p:.4f}")
