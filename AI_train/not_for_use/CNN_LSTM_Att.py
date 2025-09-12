import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from pathlib import Path
import pickle

from AI_train.training.config import ModelConfig as cf


class NormalizeSequenceLength:
    def pad_or_trim(self, seq: np.ndarray, target_len: int) -> np.ndarray:
        """
        길이가 target_len보다 길면 균등 샘플링, 짧으면 0패딩
        영상 길이가 가변적이니까 일관적인 데이터 처리를 위해 사용.
        2~3초라고 가정하고 60, 75, 90단위로 패딩 처리.
        만약 2초 미만의 영상이 많을 경우 30, 45까지 넣어도 됨.
        """
        try:
            if seq is None or seq.size == 0:
                raise ValueError("입력 시퀀스가 비어있습니다.")
            
            if len(seq.shape) != 2:
                raise ValueError(f"시퀀스는 2차원 배열이어야 합니다. 현재 shape: {seq.shape}")
            
            t, F = seq.shape
            
            if target_len <= 0:
                raise ValueError(f"target_len은 양수여야 합니다. 현재 값: {target_len}")
            
            if t == target_len:
                return seq
            if t > target_len:
                idx = np.linspace(0, t - 1, target_len).astype(int)
                return seq[idx]
            
            pad = np.zeros((target_len - t, F), dtype=seq.dtype)
            return np.vstack([seq, pad])
            
        except Exception as e:
            print(f"pad_or_trim 에러: {e}")
            raise


    def nearest_bucket_len(self, t: int, buckets=cf.BUCKETS) -> int:
        """현재 길이 t와 가장 가까운 버킷 길이를 반환"""
        try:
            if t <= 0:
                raise ValueError(f"시퀀스 길이는 양수여야 합니다. 현재 값: {t}")
            
            if not buckets or len(buckets) == 0:
                raise ValueError("버킷 리스트가 비어있습니다.")
            
            if any(b <= 0 for b in buckets):
                raise ValueError("모든 버킷 값은 양수여야 합니다.")
            
            return min(buckets, key=lambda b: abs(b - t))
            
        except Exception as e:
            print(f"nearest_bucket_len 에러: {e}")
            raise


# ==========================
# 모델 클래스
# ==========================
class SignLanguageModel:
    def __init__(self):
        self.num_classes = cf.NUM_CLASSES
        self.sequence_length = cf.SEQUENCE_LENGTH
        self.feature_dim = cf.FEATURE_DIM
        self.model = None
        self.label_encoder = LabelEncoder()
        self.normalizer = NormalizeSequenceLength()

    def build_model(self):
        """CNN + LSTM 하이브리드 모델"""
        inputs = keras.Input(shape=(self.sequence_length, self.feature_dim))

        # 프레임별 CNN 처리
        x = layers.Reshape((self.sequence_length, self.feature_dim, 1))(inputs)

        x = layers.TimeDistributed(layers.Conv1D(64, 3, activation='relu', padding='same'))(x)
        x = layers.TimeDistributed(layers.BatchNormalization())(x)
        x = layers.TimeDistributed(layers.MaxPooling1D(2))(x)
        x = layers.TimeDistributed(layers.Dropout(cf.DROPOUT_RATE))(x)

        x = layers.TimeDistributed(layers.Conv1D(128, 3, activation='relu', padding='same'))(x)
        x = layers.TimeDistributed(layers.BatchNormalization())(x)
        x = layers.TimeDistributed(layers.MaxPooling1D(2))(x)
        x = layers.TimeDistributed(layers.Dropout(cf.DROPOUT_RATE))(x)

        x = layers.TimeDistributed(layers.Conv1D(256, 3, activation='relu', padding='same'))(x)
        x = layers.TimeDistributed(layers.BatchNormalization())(x)
        x = layers.TimeDistributed(layers.GlobalMaxPooling1D())(x)
        x = layers.TimeDistributed(layers.Dropout(cf.DROPOUT_RATE))(x)

        # LSTM 블록
        x = layers.LSTM(512, return_sequences=True, dropout=cf.DROPOUT_RATE)(x)
        x = layers.LSTM(256, return_sequences=True, dropout=cf.DROPOUT_RATE)(x)
        x = layers.LSTM(128, dropout=cf.DROPOUT_RATE)(x)

        # 분류기
        x = layers.Dense(512, activation='relu')(x)
        x = layers.BatchNormalization()(x)
        x = layers.Dropout(0.5)(x)

        x = layers.Dense(256, activation='relu')(x)
        x = layers.Dropout(0.5)(x)

        outputs = layers.Dense(self.num_classes, activation='softmax')(x)

        self.model = keras.Model(inputs, outputs)
        self.model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=cf.LEARNING_RATE, clipnorm=1.0),
            loss='categorical_crossentropy',
            metrics=['accuracy', 'top_5_accuracy']
        )
        return self.model

    def load_processed_data(self, data_path):
        """전처리된 landmark 데이터 로드"""
        try:
            data_path = Path(data_path)
            
            if not data_path.exists():
                raise FileNotFoundError(f"데이터 경로가 존재하지 않습니다: {data_path}")
            
            metadata_path = cf.DATA_ROOT / "dataset_metadata.csv"
            if not metadata_path.exists():
                raise FileNotFoundError(f"메타데이터 파일이 존재하지 않습니다: {metadata_path}")
            
            metadata = pd.read_csv(metadata_path)
            
            if metadata.empty:
                raise ValueError("메타데이터가 비어있습니다.")
            
            if 'landmarks_file' not in metadata.columns or 'word_gloss' not in metadata.columns:
                raise ValueError("메타데이터에 필요한 컬럼이 없습니다. 'landmarks_file'과 'word_gloss' 컬럼이 필요합니다.")

            X, y = [], []
            failed_files = []
            
            for idx, row in metadata.iterrows():
                try:
                    landmarks_file = data_path / row['landmarks_file']
                    if landmarks_file.exists():
                        landmarks = np.load(landmarks_file)
                        landmarks = self.normalize_sequence_length(landmarks)
                        X.append(landmarks)
                        y.append(row['word_gloss'])
                    else:
                        failed_files.append(str(landmarks_file))
                        print(f"경고: 파일이 존재하지 않습니다: {landmarks_file}")
                except Exception as e:
                    failed_files.append(str(landmarks_file))
                    print(f"경고: 파일 로딩 실패 {landmarks_file}: {e}")
                    continue
            
            if len(X) == 0:
                raise ValueError("로드된 데이터가 없습니다. 모든 파일 로딩에 실패했습니다.")
            
            if len(failed_files) > 0:
                print(f"총 {len(failed_files)}개 파일 로딩에 실패했습니다.")
            
            print(f"성공적으로 로드된 데이터: {len(X)}개")
            
            y_encoded = self.label_encoder.fit_transform(y)
            y_categorical = keras.utils.to_categorical(y_encoded, self.num_classes)
            
            return np.array(X), y_categorical
            
        except Exception as e:
            print(f"load_processed_data 에러: {e}")
            raise

    def normalize_sequence_length(self, sequence: np.ndarray) -> np.ndarray:
        """버킷 기반 길이 정규화 → 최종 SEQUENCE_LENGTH 보장"""
        try:
            if sequence is None or sequence.size == 0:
                raise ValueError("입력 시퀀스가 비어있습니다.")
            if len(sequence.shape) != 2:
                raise ValueError(f"시퀀스는 2차원 배열이어야 합니다. 현재 shape: {sequence.shape}")

            # 안전장치
            sequence = np.nan_to_num(sequence, copy=False).astype(np.float32)

            # 1) 가까운 버킷으로 1차 정규화 (시간 왜곡 최소화)
            t = sequence.shape[0]
            bucket = self.normalizer.nearest_bucket_len(t, buckets=cf.BUCKETS)
            seq = self.normalizer.pad_or_trim(sequence, bucket)

            # 2) 최종적으로 모델 입력(len=cf.SEQUENCE_LENGTH)과 일치시킴
            if bucket != cf.SEQUENCE_LENGTH:
                seq = self.normalizer.pad_or_trim(seq, cf.SEQUENCE_LENGTH)

            return seq

        except Exception as e:
            print(f"normalize_sequence_length 에러: {e}")
            raise

    def train(self, data_path, validation_split=0.2):
        """모델 학습"""
        try:
            if not (0 < validation_split < 1):
                raise ValueError(f"validation_split은 0과 1 사이의 값이어야 합니다. 현재 값: {validation_split}")
            
            print("데이터 로딩 중...")
            X, y = self.load_processed_data(data_path)
            
            if len(X) < 10:
                raise ValueError(f"학습 데이터가 너무 적습니다. 최소 10개 이상 필요합니다. 현재: {len(X)}개")
            
            print("데이터 분할 중...")
            # stratify를 안전하게 처리
            try:
                X_train, X_val, y_train, y_val = train_test_split(
                    X, y, test_size=validation_split, random_state=42, stratify=y.argmax(axis=1)
                )
            except ValueError as e:
                print(f"stratify 실패, stratify 없이 분할: {e}")
                X_train, X_val, y_train, y_val = train_test_split(
                    X, y, test_size=validation_split, random_state=42
                )
            
            print(f"학습 데이터: {len(X_train)}개, 검증 데이터: {len(X_val)}개")

            if self.model is None:
                print("모델 구성 중...")
                self.build_model()
            else:
                print("기존 모델 사용")

            # 모델 저장 경로 확인
            model_save_path = cf.MODEL_SAVA_PATH
            if not model_save_path.exists():
                model_save_path.mkdir(parents=True, exist_ok=True)
                print(f"모델 저장 경로 생성: {model_save_path}")

            callbacks = [
                keras.callbacks.EarlyStopping(monitor='val_accuracy', patience=15, restore_best_weights=True, verbose=1),
                keras.callbacks.ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=7, min_lr=1e-7, verbose=1),
                keras.callbacks.ModelCheckpoint(
                    model_save_path / "best_model.h5", monitor='val_accuracy', save_best_only=True, verbose=1
                )
            ]

            print("모델 학습 시작...")
            history = self.model.fit(
                X_train, y_train,
                validation_data=(X_val, y_val),
                epochs=cf.EPOCHS,
                batch_size=cf.BATCH_SIZE,
                callbacks=callbacks,
                verbose=1
            )

            print("라벨 인코더 저장 중...")
            with open(model_save_path / 'label_encoder.pkl', 'wb') as f:
                pickle.dump(self.label_encoder, f)
            
            print("학습 완료!")
            return history
            
        except Exception as e:
            print(f"train 에러: {e}")
            raise

    def predict(self, landmarks_sequence):
        """실시간 예측"""
        try:
            if self.model is None:
                raise ValueError("모델이 학습되지 않았습니다.")
            
            if landmarks_sequence is None or landmarks_sequence.size == 0:
                raise ValueError("입력 시퀀스가 비어있습니다.")
            
            if len(landmarks_sequence.shape) != 2:
                raise ValueError(f"입력 시퀀스는 2차원 배열이어야 합니다. 현재 shape: {landmarks_sequence.shape}")
            
            if landmarks_sequence.shape[1] != self.feature_dim:
                raise ValueError(f"입력 시퀀스의 특성 차원이 맞지 않습니다. 예상: {self.feature_dim}, 실제: {landmarks_sequence.shape[1]}")

            seq = self.normalize_sequence_length(landmarks_sequence)
            seq = np.expand_dims(seq, axis=0)

            predictions = self.model.predict(seq, verbose=0)
            
            if predictions is None or len(predictions) == 0:
                raise ValueError("모델 예측 결과가 비어있습니다.")
            
            pred_class = np.argmax(predictions[0])
            confidence = predictions[0][pred_class]
            
            if confidence < 0 or confidence > 1:
                print(f"경고: 신뢰도 값이 비정상적입니다: {confidence}")
            
            word = self.label_encoder.inverse_transform([pred_class])[0]
            return word, confidence
            
        except Exception as e:
            print(f"predict 에러: {e}")
            raise

    def evaluate_model(self, data_path):
        """모델 평가"""
        try:
            if self.model is None:
                raise ValueError("모델이 학습되지 않았습니다.")
            
            print("평가용 데이터 로딩 중...")
            X, y = self.load_processed_data(data_path)
            
            if len(X) == 0:
                raise ValueError("평가할 데이터가 없습니다.")
            
            print(f"평가 데이터: {len(X)}개")
            print("모델 평가 중...")
            
            results = self.model.evaluate(X, y, verbose=0)
            
            if results is None or len(results) == 0:
                raise ValueError("모델 평가 결과가 비어있습니다.")
            
            metrics = dict(zip(self.model.metrics_names, results))
            
            print("모델 평가 결과:")
            for metric, value in metrics.items():
                print(f"  {metric}: {value:.4f}")
            
            return metrics
            
        except Exception as e:
            print(f"evaluate_model 에러: {e}")
            raise


# 실행 예시
def main():
    try:
        print("=== 수어 인식 모델 학습 시작 ===")
        
        # 모델 초기화
        print("모델 초기화 중...")
        model = SignLanguageModel()
        
        # 학습 실행
        print("모델 학습 시작...")
        history = model.train(cf.DATA_ROOT, validation_split=0.2)
        
        # 모델 평가
        print("모델 평가 시작...")
        metrics = model.evaluate_model(cf.DATA_ROOT)
        
        print(f"\n=== 학습 완료 ===")
        print(f"최종 정확도: {metrics['accuracy']:.2%}")
        
        # top_5_accuracy가 있는 경우에만 출력
        if 'top_5_accuracy' in metrics:
            print(f"Top-5 정확도: {metrics['top_5_accuracy']:.2%}")
        else:
            print("Top-5 정확도: 측정되지 않음")
        
    except Exception as e:
        print(f"main 함수 에러: {e}")
        print("프로그램을 종료합니다.")
        raise


if __name__ == "__main__":
    main()
