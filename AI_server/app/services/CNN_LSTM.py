
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np
import pandas as pd
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
from pathlib import Path
import pickle

class SignLanguageCNNLSTM:
    def __init__(self, num_classes, sequence_length=60, feature_dim=225):
        '''
        수어 인식용 CNN+LSTM 모델

        Args:
            num_classes: 수어 단어 클래스 수 (AI-Hub 기준 ~3000개)
            sequence_length: 시퀀스 길이 (프레임 수)
            feature_dim: MediaPipe landmark 특성 차원 (pose:99 + hands:126 = 225)
        '''
        # 이론 상 ai허브에서 제공하는 단어가 3000개
        self.num_classes = num_classes
        # 프레임 수(mediapipe 기준이라면 30으로 낮추면 됨. 이는 우리 컴퓨터 성능으로 타협하면서 해야할 듯)
        self.sequence_length = sequence_length
        # 특성 차원(좌표값들. 손이면 42개였나? 3배수 하고 골격에도 뽑아서 225차원을 맞춰 줌)
        self.feature_dim = feature_dim
        self.model = None
        self.label_encoder = LabelEncoder()

    def build_model(self):
        '''
        CNN+LSTM 하이브리드 모델 구성
        L40S 48GB GPU 최적화
        '''
        # 입력층
        inputs = keras.Input(shape=(self.sequence_length, self.feature_dim))

        # Reshape for CNN (시간축을 배치로 처리)
        x = layers.Reshape((self.sequence_length, self.feature_dim, 1))(inputs)

        # TimeDistributed CNN 층들
        # 각 프레임 단위로 CNN 적용
        x = layers.TimeDistributed(
            layers.Conv1D(64, 3, activation='relu', padding='same')
        )(x)
        x = layers.TimeDistributed(layers.BatchNormalization())(x)
        x = layers.TimeDistributed(layers.MaxPooling1D(2))(x)
        x = layers.TimeDistributed(layers.Dropout(0.3))(x)

        x = layers.TimeDistributed(
            layers.Conv1D(128, 3, activation='relu', padding='same')
        )(x)
        x = layers.TimeDistributed(layers.BatchNormalization())(x)
        x = layers.TimeDistributed(layers.MaxPooling1D(2))(x)
        x = layers.TimeDistributed(layers.Dropout(0.3))(x)

        x = layers.TimeDistributed(
            layers.Conv1D(256, 3, activation='relu', padding='same')
        )(x)
        x = layers.TimeDistributed(layers.BatchNormalization())(x)
        x = layers.TimeDistributed(layers.GlobalMaxPooling1D())(x)
        x = layers.TimeDistributed(layers.Dropout(0.4))(x)

        # LSTM 층들 (시간적 패턴 학습) 3층으로 쌓은 것(512 + 256 + 128)
        x = layers.LSTM(512, return_sequences=True, dropout=0.3, recurrent_dropout=0.3)(x)
        x = layers.LSTM(256, return_sequences=True, dropout=0.3, recurrent_dropout=0.3)(x)
        x = layers.LSTM(128, dropout=0.3, recurrent_dropout=0.3)(x)

        # Dense 층들
        x = layers.Dense(512, activation='relu')(x)
        x = layers.BatchNormalization()(x)
        x = layers.Dropout(0.5)(x)

        x = layers.Dense(256, activation='relu')(x)
        x = layers.Dropout(0.5)(x)

        # 출력층
        outputs = layers.Dense(self.num_classes, activation='softmax')(x)

        # 모델 생성
        self.model = keras.Model(inputs, outputs)

        # L40S GPU 최적화 컴파일
        self.model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=0.001, clipnorm=1.0),
            loss='categorical_crossentropy',
            metrics=['accuracy', 'top_5_accuracy']
        )

        return self.model

    def load_processed_data(self, data_path):
        '''
        데이터 전처리된 MediaPipe 데이터 로드
        추후에 뭔가 추가 처리가 들어가면 바뀔 수 있음. 일단은 이런 형식으로 들어온다 가정
        '''
        data_path = Path(data_path)

        # 메타데이터 로드. data 폴더에 dataset_meta.csv 파일로 저장. 만약 이름이 달라진다면 맨 뒤만 바꾸면 됨.
        # os까지 쓰는건 굳이라 안씀. 나중에 배포 수준까지 간다면 상대 경로로 바꿀 것!!!!!!!!!!!!!!!!!!!!
        metadata = pd.read_csv(data_path / "C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_server\data\dataset_metadata.csv")

        # Landmark 데이터 로드
        X = []
        y = []

        for _, row in metadata.iterrows():
            landmarks_file = data_path / row['landmarks_file']
            if landmarks_file.exists():
                landmarks = np.load(landmarks_file)

                # 시퀀스 길이 정규화
                landmarks = self.normalize_sequence_length(landmarks)

                X.append(landmarks)
                y.append(row['word_gloss'])

        # 라벨 인코딩
        y_encoded = self.label_encoder.fit_transform(y)
        y_categorical = keras.utils.to_categorical(y_encoded, self.num_classes)

        return np.array(X), y_categorical

    def normalize_sequence_length(self, sequence):
        '''
        시퀀스 길이를 고정 길이로 정규화
        '''
        current_length = sequence.shape[0]

        if current_length > self.sequence_length:
            # 긴 시퀀스는 균등하게 샘플링
            indices = np.linspace(0, current_length-1, self.sequence_length, dtype=int)
            return sequence[indices]
        elif current_length < self.sequence_length:
            # 짧은 시퀀스는 패딩
            padding = np.zeros((self.sequence_length - current_length, sequence.shape[1]))
            return np.vstack([sequence, padding])
        else:
            return sequence

    def train(self, data_path, validation_split=0.2, epochs=100, batch_size=32):
        '''
        모델 학습
        L40S 48GB 최적화 설정
        '''
        # 데이터 로드. data/ 안에 학습용 데이터 넣어서 함.
        X, y = self.load_processed_data(data_path)

        print(f"데이터 형태: X={X.shape}, y={y.shape}")

        # 학습/검증 분할
        X_train, X_val, y_train, y_val = train_test_split(
            X, y, test_size=validation_split, random_state=42, stratify=y.argmax(axis=1)
        )

        # 모델 구성
        if self.model is None:
            self.build_model()

        print("모델 구조:")
        self.model.summary()

        # 콜백 설정. 현황 확인하고 오류 잡기 위해 넣은 것.
        callbacks = [
            keras.callbacks.EarlyStopping(
                monitor='val_accuracy',
                patience=15,
                restore_best_weights=True,
                verbose=1
            ),
            keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=7,
                min_lr=1e-7,
                verbose=1
            ),
            keras.callbacks.ModelCheckpoint(
                'best_ksl_model.h5',
                monitor='val_accuracy',
                save_best_only=True,
                verbose=1
            )
        ]

        # L40S GPU 최적화: Mixed Precision 사용
        policy = keras.mixed_precision.Policy('mixed_float16')
        keras.mixed_precision.set_global_policy(policy)

        # 학습 실행
        # 아마 이 부분에서가 파인튜닝 세부 설정이 될 것. 더 많게 하거나 줄이면서 하나씩 확인 요망.
        history = self.model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=batch_size,  # L40S 48GB에서 더 큰 배치 사용 가능
            callbacks=callbacks,
            verbose=1
        )

        # 라벨 인코더 저장
        with open('label_encoder.pkl', 'wb') as f:
            pickle.dump(self.label_encoder, f)

        return history

    def predict(self, landmarks_sequence):
        '''
        실시간 예측
        '''
        # 모델이 없는 경우 = 학습 된 모델이 없는 경우. 이거 찍히면 개큰일난것.
        if self.model is None:
            raise ValueError("모델이 학습되지 않았습니다.")

        # 시퀀스 정규화
        normalized_seq = self.normalize_sequence_length(landmarks_sequence)
        normalized_seq = np.expand_dims(normalized_seq, axis=0)

        # 예측
        predictions = self.model.predict(normalized_seq)
        predicted_class = np.argmax(predictions[0])
        confidence = predictions[0][predicted_class]

        # 라벨 디코딩
        predicted_word = self.label_encoder.inverse_transform([predicted_class])[0]

        return predicted_word, confidence

    def evaluate_model(self, data_path):
        '''
        모델 성능 평가
        '''
        X, y = self.load_processed_data(data_path)

        # 평가
        results = self.model.evaluate(X, y, verbose=0)
        metrics = dict(zip(self.model.metrics_names, results))

        print("모델 평가 결과:")
        for metric, value in metrics.items():
            print(f"{metric}: {value:.4f}")

        return metrics

# 사용 예시
def main():
    # AI-Hub 데이터 기준 설정
    word_count = 3000  # AI-Hub 수어단어 데이터 기준 -> 이건 우리가 학습하려는 단어 개수로 고치면 됨
    # 2초 영상 (30fps 기준) -> 추가 기능을 붙여서 수어인지 아닌지 판단하는게 들어가면 빼거나 거기에서 초를 받아와서 할 것.
    # 2초로 해두면 그 텀에 짤릴 가능성이 좀 있음.
    sequence_length = 60  
    feature_dimension = 225  # MediaPipe landmark 차원

    # 모델 초기화
    model = SignLanguageCNNLSTM(
        num_classes=word_count,
        sequence_length=sequence_length,
        feature_dim=feature_dimension
    )

    # 학습 실행 - 얘는 따로 설정해서 거기서 뽑아와서 할 것. 루트 폴더가 되겠지.
    processed_data_path = "/path/to/processed/mediapipe_data"
    history = model.train(
        data_path=processed_data_path,
        epochs=100,
        batch_size=64  # L40S 48GB에서 큰 배치 사용
    )

    # 모델 평가 - 이건 찍어봐야 알 듯. 확신은 없음.
    metrics = model.evaluate_model(processed_data_path)

    print(f"학습 완료! 예상 정확도: {metrics['accuracy']:.2%}")

if __name__ == "__main__":
    main()
