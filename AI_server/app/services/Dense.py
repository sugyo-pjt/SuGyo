import numpy as np
import pickle
from tensorflow import keras
from pathlib import Path

# 경로 설정: 이 파일의 위치를 기준으로 프로젝트 루트를 찾습니다.
try:
    BASE_DIR = Path(__file__).resolve().parent.parent.parent
except NameError:
    BASE_DIR = Path('.').resolve()

MODEL_PATH = BASE_DIR / "app" / "models" / "ai_model" / "best_model.h5"
LABEL_ENCODER_PATH = BASE_DIR / "app" / "models" / "ai_model" / "label_encoder.pkl"

# --- 모델 사전 로딩 ---
model = None
label_encoder = None
try:
    print("Dense 모델 로딩 중...")
    if MODEL_PATH.exists() and LABEL_ENCODER_PATH.exists():
        model = keras.models.load_model(MODEL_PATH)
        with open(LABEL_ENCODER_PATH, "rb") as f:
            label_encoder = pickle.load(f)
        print("Dense 모델과 라벨 인코더 로딩 완료 ✅")
    else:
        print(f"경고: 모델 또는 라벨 인코더 파일을 찾을 수 없습니다.")
        print(f"모델 경로: {MODEL_PATH}")
        print(f"인코더 경로: {LABEL_ENCODER_PATH}")
except Exception as e:
    print(f"오류: Dense 모델 로딩 실패: {e}")

def classification(sequence: np.ndarray) -> str:
    """
    입력된 시퀀스 데이터를 분류하여 해당하는 단어를 반환합니다.
    모델과 라벨 인코더는 서버 시작 시 미리 로드됩니다.
    """
    if model is None or label_encoder is None:
        raise RuntimeError("모델이 로드되지 않아 분류를 수행할 수 없습니다.")

    # 입력 데이터는 이미 numpy 배열이어야 합니다.
    # 배치 차원 추가 및 타입 변환
    sequence = np.expand_dims(sequence, axis=0).astype(np.float32)

    # 예측 수행
    predictions = model.predict(sequence)
    class_index = np.argmax(predictions, axis=1)[0]
    word = label_encoder.inverse_transform([class_index])[0]

    return word
