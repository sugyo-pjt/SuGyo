import sys
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
import chromadb
from pathlib import Path

# --- 경로 설정 ---
# 이 스크립트의 위치를 기준으로 상대 경로를 설정합니다.
try:
    SCRIPT_DIR = Path(__file__).resolve().parent
except NameError:
    # 주피터 노트북 등에서 실행할 경우를 대비
    SCRIPT_DIR = Path('.').resolve()

# AI_train/training 폴더의 경로를 sys.path에 추가하여 config를 임포트할 수 있게 합니다.
TRAINING_DIR = SCRIPT_DIR.parent.parent / 'AI_train' / 'training'
sys.path.append(str(TRAINING_DIR))

# --- 주요 경로 및 상수 정의 ---
MODEL_PATH = TRAINING_DIR / 'model' / 'best_model.h5'
METADATA_PATH = TRAINING_DIR / 'data' / 'dataset_metadata.csv'
LANDMARKS_DIR = TRAINING_DIR / 'data'
DB_PERSIST_PATH = str(SCRIPT_DIR / "sign_motion_db") # DB가 저장될 폴더
COLLECTION_NAME = "sign_motions"
FEATURE_EXTRACTOR_LAYER_NAME = 'motion_vector_extractor' # 모델 생성 시 지정한 레이어 이름

def pad_or_trim(seq: np.ndarray, target_len: int) -> np.ndarray:
    """
    시퀀스 길이를 target_len으로 정규화 (패딩 또는 트리밍)
    """
    if seq is None or seq.size == 0:
        raise ValueError("Input sequence is empty.")
    
    if len(seq.shape) != 2:
        raise ValueError(f"Sequence must be a 2D array. Current shape: {seq.shape}")

    t, F = seq.shape
    
    if t == target_len:
        return seq
    
    # 길이가 길면 균등 샘플링
    if t > target_len:
        idx = np.linspace(0, t - 1, target_len).astype(int)
        return seq[idx]
    
    # 길이가 짧으면 0으로 패딩
    pad = np.zeros((target_len - t, F), dtype=seq.dtype)
    return np.vstack([seq, pad])

def create_motion_db():
    """
    학습된 모델과 데이터를 사용하여 동작 벡터 DB를 생성합니다.
    """
    print("--- 동작 벡터 DB 생성을 시작합니다. ---")

    # 1. 모델 및 특징 추출기 로드
    if not MODEL_PATH.exists():
        print(f"오류: 모델 파일을 찾을 수 없습니다. 경로: {MODEL_PATH}")
        return

    print(f"1. 모델 로드 중... 경로: {MODEL_PATH}")
    original_model = keras.models.load_model(MODEL_PATH)
    
    try:
        feature_extractor_model = keras.Model(
            inputs=original_model.input,
            outputs=original_model.get_layer(FEATURE_EXTRACTOR_LAYER_NAME).output
        )
        print("   - 특징 추출 모델을 성공적으로 생성했습니다.")
    except ValueError:
        print(f"오류: 모델에서 '{FEATURE_EXTRACTOR_LAYER_NAME}' 레이어를 찾을 수 없습니다.")
        print("모델 빌드 시 해당 레이어에 name을 지정했는지 확인하세요.")
        return

    # 2. ChromaDB 클라이언트 설정
    print(f"2. ChromaDB 설정 중... DB 저장 경로: {DB_PERSIST_PATH}")
    client = chromadb.PersistentClient(path=DB_PERSIST_PATH)
    collection = client.get_or_create_collection(name=COLLECTION_NAME)
    print(f"   - '{COLLECTION_NAME}' 컬렉션을 준비했습니다.")

    # 3. 메타데이터 로드
    if not METADATA_PATH.exists():
        print(f"오류: 메타데이터 파일을 찾을 수 없습니다. 경로: {METADATA_PATH}")
        return
    
    print(f"3. 메타데이터 로드 중... 경로: {METADATA_PATH}")
    metadata_df = pd.read_csv(METADATA_PATH)
    print(f"   - 총 {len(metadata_df)}개의 데이터 처리를 시작합니다.")

    # 4. 데이터 처리 및 DB 저장
    embeddings = []
    metadatas = []
    ids = []
    
    for i, row in metadata_df.iterrows():
        try:
            landmark_file = LANDMARKS_DIR / row['landmarks_file']
            label = row['word_gloss']
            
            if not landmark_file.exists():
                print(f"   - 경고: 파일을 찾을 수 없음 - {landmark_file}")
                continue

            # 랜드마크 데이터 로드 및 전처리
            landmarks = np.load(landmark_file)
            landmarks = np.nan_to_num(landmarks).astype(np.float32)
            landmarks = pad_or_trim(landmarks, 60)
            
            # 모델 입력 형태로 변환 (batch 차원 추가)
            landmarks_batch = np.expand_dims(landmarks, axis=0)

            # 동작 벡터 추출
            motion_vector = feature_extractor_model.predict(landmarks_batch, verbose=0)
            
            # 저장할 리스트에 추가
            embeddings.append(motion_vector.flatten().tolist())
            metadatas.append({"label": label})
            # id는 파일명에서 확장자를 제거하여 사용 (고유해야 함)
            file_id = landmark_file.stem
            ids.append(file_id)

            if (i + 1) % 100 == 0:
                print(f"   - {i + 1}/{len(metadata_df)} 처리 완료...")

        except Exception as e:
            print(f"   - 오류 발생: 파일 {row['landmarks_file']} 처리 중 문제 발생 - {e}")
            continue
    
    # 5. DB에 일괄 추가
    if embeddings:
        print(f"5. 총 {len(embeddings)}개의 벡터를 DB에 저장합니다.")
        # ChromaDB는 ID가 중복되면 오류를 발생시키므로, 기존 ID는 건너뛰는 로직이 필요할 수 있습니다.
        # 여기서는 간단하게 일괄 추가합니다.
        try:
            collection.add(
                embeddings=embeddings,
                metadatas=metadatas,
                ids=ids
            )
            print("   - DB 저장이 성공적으로 완료되었습니다.")
        except Exception as e:
            print(f"   - DB 저장 중 오류 발생: {e}")
            print("   - ID 중복 등의 문제가 있을 수 있습니다.")

    else:
        print("오류: DB에 저장할 벡터를 하나도 생성하지 못했습니다.")

    print("모든 작업이 완료되었습니다. ---")


if __name__ == "__main__":
    # TensorFlow GPU 메모리 관리 설정 (선택 사항)
    gpus = tf.config.experimental.list_physical_devices('GPU')
    if gpus:
        try:
            for gpu in gpus:
                tf.config.experimental.set_memory_growth(gpu, True)
        except RuntimeError as e:
            print(e)
            
    create_motion_db()