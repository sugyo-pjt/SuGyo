import numpy as np
import pickle
from tensorflow import keras
import os
from dotenv import load_dotenv
load_dotenv()

# --- 경로 설정 ---
MODEL_PATH = os.getenv("MODEL_PATH")
LABEL_ENCODER_PATH = os.getenv("LABEL_ENCODER_PATH")

# --- 모델 & 라벨 로더 ---
print("모델 로딩 중...")
model = keras.models.load_model(MODEL_PATH)
with open(LABEL_ENCODER_PATH, "rb") as f:
    label_encoder = pickle.load(f)
print("모델과 라벨 인코더 로딩 완료 ✅")

def predict_word(sequence: np.ndarray) -> str:
    """
    좌표 시퀀스를 입력받아 단어를 예측
    - sequence shape: (T, F)
      T: 시퀀스 길이 (15~90 프레임 버킷 정규화됨)
      F: 특징 차원 (포즈+손+얼굴 좌표)
    """
    # 배치 차원 추가: (1, T, F)
    sequence = np.expand_dims(sequence, axis=0).astype(np.float32)

    # 예측
    preds = model.predict(sequence)
    class_idx = np.argmax(preds, axis=1)[0]
    word = label_encoder.inverse_transform([class_idx])[0]

    return word

# --- 테스트 ---
if __name__ == "__main__":
    # 예시 좌표 입력 (랜덤, 실제는 Mediapipe 추출 값 사용)
    T, F = 30, 50  # 30프레임, 좌표 50차원 예시
    dummy_sequence = np.random.rand(T, F)

    result = predict_word(dummy_sequence)
    print("예측된 단어:", result)
