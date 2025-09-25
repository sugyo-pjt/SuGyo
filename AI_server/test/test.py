import cv2
import numpy as np
import mediapipe as mp
import tensorflow as tf
import pickle
import pandas as pd

# 1. 설정
MODEL_PATH       = r"C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_server\test\best_model.h5"
LABELENCODER_PKL = r"C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_server\test\label_encoder.pkl"
METADATA_CSV     = r"C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_server\test\poseformer_metadata_total.csv"
SEQUENCE_LENGTH  = 90  # config.py의 SEQUENCELENGTH과 동일
FEATURE_DIM      = 195 # joints(65) * coords(3)
CHANNELS         = 1

# 2. LabelEncoder 로드
with open(LABELENCODER_PKL, "rb") as f:
    label_encoder = pickle.load(f)
class_names = label_encoder.classes_

# 3. 모델 로드
model = tf.keras.models.load_model(MODEL_PATH)
model.compile()  # compile 옵션은 이미 저장된 상태이므로 최소화

# 4. MediaPipe 초기화
mp_pose = mp.solutions.pose
pose = mp_pose.Pose(static_image_mode=False,
                    model_complexity=1,
                    enable_segmentation=False,
                    min_detection_confidence=0.5,
                    min_tracking_confidence=0.5)

# 5. 특징 버퍼
buffer = []

def extract_landmarks(results):
    if not results.pose_landmarks:
        return [0.0] * (33 * 4)
    lm = results.pose_landmarks.landmark
    return [coord for p in lm for coord in (p.x, p.y, p.z, p.visibility)]

# 6. 카메라 실행
cap = cv2.VideoCapture(0)
if not cap.isOpened():
    raise RuntimeError("카메라를 열 수 없습니다.")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    frame = cv2.flip(frame, 1)
    rgb   = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    res   = pose.process(rgb)
    lm    = extract_landmarks(res)

    buffer.append(lm)
    if len(buffer) > SEQUENCE_LENGTH:
        buffer.pop(0)

    if len(buffer) == SEQUENCE_LENGTH:
        x = np.array(buffer, dtype=np.float32)
        x = x.reshape(SEQUENCE_LENGTH, FEATURE_DIM, CHANNELS, 1)
        preds = model.predict(x, verbose=0)[0]
        idx   = np.argmax(preds)
        label = label_encoder.inverse_transform([idx])[0]
        conf  = preds[idx] * 100

        cv2.putText(frame, f"{label} ({conf:.1f}%)",
                    (10,30), cv2.FONT_HERSHEY_SIMPLEX,
                    1, (0,255,0), 2)

    cv2.imshow("SLR with Encoder", frame)
    if cv2.waitKey(1) & 0xFF == 27:  # ESC
        break

cap.release()
cv2.destroyAllWindows()
pose.close()
