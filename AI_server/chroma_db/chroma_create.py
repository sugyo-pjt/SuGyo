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

# config 모듈 import
try:
    from config import ModelConfig as cf
except ImportError:
    print("경고: config.py를 찾을 수 없습니다. 기본값을 사용합니다.")
    # 기본값 설정
    class DefaultConfig:
        SEQUENCE_LENGTH = 60
        FEATURE_DIM = 234
    cf = DefaultConfig()

# --- 주요 경로 및 상수 정의 ---
MODEL_PATH = TRAINING_DIR / 'model' / 'best_model.h5'
METADATA_PATH = TRAINING_DIR / 'data' / 'dataset_metadata.csv'
LANDMARKS_DIR = TRAINING_DIR / 'data'
DB_PERSIST_PATH = str(SCRIPT_DIR / "sign_motion_db") # DB가 저장될 폴더
COLLECTION_NAME = "sign_motions"
FEATURE_EXTRACTOR_LAYER_NAME = 'global_average_pooling1d' # GlobalAveragePooling1D 레이어에서 특징 추출

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
    except ValueError as e:
        print(f"오류: 모델에서 '{FEATURE_EXTRACTOR_LAYER_NAME}' 레이어를 찾을 수 없습니다.")
        print(f"상세 오류: {e}")
        print("사용 가능한 레이어들:")
        for i, layer in enumerate(original_model.layers):
            print(f"  {i}: {layer.name} ({type(layer).__name__})")
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
            
            # 데이터 형태 검증
            if len(landmarks.shape) != 2:
                print(f"   - 경고: 잘못된 데이터 형태 - {landmarks.shape}, 파일: {landmark_file}")
                continue
                
            if landmarks.shape[1] != cf.FEATURE_DIM:
                print(f"   - 경고: 특성 차원 불일치 - 예상: {cf.FEATURE_DIM}, 실제: {landmarks.shape[1]}, 파일: {landmark_file}")
                continue
            
            landmarks = pad_or_trim(landmarks, cf.SEQUENCE_LENGTH)
            
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
        
        # 기존 데이터가 있는지 확인하고 중복 ID 처리
        try:
            existing_ids = set()
            try:
                existing_data = collection.get()
                if existing_data and existing_data['ids']:
                    existing_ids = set(existing_data['ids'])
                    print(f"   - 기존 DB에 {len(existing_ids)}개의 데이터가 있습니다.")
            except Exception:
                print("   - 기존 DB가 비어있거나 접근할 수 없습니다.")
            
            # 중복되지 않는 데이터만 필터링
            new_embeddings = []
            new_metadatas = []
            new_ids = []
            
            for i, (emb, meta, id_val) in enumerate(zip(embeddings, metadatas, ids)):
                if id_val not in existing_ids:
                    new_embeddings.append(emb)
                    new_metadatas.append(meta)
                    new_ids.append(id_val)
                else:
                    print(f"   - 중복 ID 건너뛰기: {id_val}")
            
            if new_embeddings:
                collection.add(
                    embeddings=new_embeddings,
                    metadatas=new_metadatas,
                    ids=new_ids
                )
                print(f"   - {len(new_embeddings)}개의 새로운 벡터를 DB에 저장했습니다.")
            else:
                print("   - 저장할 새로운 데이터가 없습니다.")
                
        except Exception as e:
            print(f"   - DB 저장 중 오류 발생: {e}")
            print("   - 수동으로 DB를 초기화하거나 다른 방법을 시도해보세요.")

    else:
        print("오류: DB에 저장할 벡터를 하나도 생성하지 못했습니다.")

    print("모든 작업이 완료되었습니다. ---")


def test_feature_extraction():
    """
    특징 추출 기능을 테스트하는 함수
    """
    print("--- 특징 추출 테스트 시작 ---")
    
    # 모델 로드
    if not MODEL_PATH.exists():
        print(f"오류: 모델 파일을 찾을 수 없습니다. 경로: {MODEL_PATH}")
        return False
    
    try:
        original_model = keras.models.load_model(MODEL_PATH)
        print("모델 로드 성공")
        
        # 특징 추출 모델 생성
        feature_extractor_model = keras.Model(
            inputs=original_model.input,
            outputs=original_model.get_layer(FEATURE_EXTRACTOR_LAYER_NAME).output
        )
        print("특징 추출 모델 생성 성공")
        
        # 테스트 데이터 생성
        test_data = np.random.rand(1, cf.SEQUENCE_LENGTH, cf.FEATURE_DIM).astype(np.float32)
        print(f"테스트 데이터 형태: {test_data.shape}")
        
        # 특징 추출 테스트
        features = feature_extractor_model.predict(test_data, verbose=0)
        print(f"추출된 특징 형태: {features.shape}")
        print(f"특징 벡터 차원: {features.flatten().shape}")
        
        print("특징 추출 테스트 성공!")
        return True
        
    except Exception as e:
        print(f"테스트 실패: {e}")
        return False


if __name__ == "__main__":
    import sys
    
    # TensorFlow GPU 메모리 관리 설정 (선택 사항)
    gpus = tf.config.experimental.list_physical_devices('GPU')
    if gpus:
        try:
            for gpu in gpus:
                tf.config.experimental.set_memory_growth(gpu, True)
        except RuntimeError as e:
            print(e)
    
    # 명령행 인수 확인
    if len(sys.argv) > 1 and sys.argv[1] == "test":
        print("테스트 모드로 실행합니다.")
        test_feature_extraction()
    else:
        print("DB 생성 모드로 실행합니다.")
        create_motion_db()