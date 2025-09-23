import sys
import os
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
import chromadb
import openai
import pickle
from pathlib import Path
from dotenv import load_dotenv
from typing import List

# .env 파일에서 환경 변수 로드
load_dotenv()

# --- 경로 설정 ---
try:
    SCRIPT_DIR = Path(__file__).resolve().parent
except NameError:
    SCRIPT_DIR = Path('.').resolve()

AI_TRAIN_DIR = SCRIPT_DIR.parent.parent / 'AI_train'
TRAINING_DIR = AI_TRAIN_DIR / 'training'
sys.path.append(str(TRAINING_DIR))

from config import ModelConfig as cf

# --- 주요 경로 및 상수 정의 ---
MODEL_PATH = TRAINING_DIR / 'model' / 'best_model.h5'
LABEL_ENCODER_PATH = TRAINING_DIR / 'model' / 'label_encoder.pkl'
DB_PERSIST_PATH = SCRIPT_DIR.parent / 'chroma_db' / "sign_motion_db"
COLLECTION_NAME = "sign_motions"
FEATURE_EXTRACTOR_LAYER_NAME = 'motion_vector_extractor'

class SignRecognizer:
    _instance = None

    def __new__(cls, *args, **kwargs):
        if not cls._instance:
            cls._instance = super(SignRecognizer, cls).__new__(cls, *args, **kwargs)
        return cls._instance

    def __init__(self):
        if hasattr(self, 'initialized'):
            return
        print("--- SignRecognizer 초기화를 시작합니다. ---")
        self.original_model = self._load_model()
        self.feature_extractor_model = self._create_feature_extractor()
        self.label_encoder = self._load_label_encoder()
        self.chroma_collection = self._connect_chromadb()
        self.openai_client = openai.OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        self.initialized = True
        print("--- SignRecognizer 초기화 완료. ---")

    def _load_model(self):
        if not MODEL_PATH.exists(): raise FileNotFoundError(f"모델 파일을 찾을 수 없습니다: {MODEL_PATH}")
        print(f"1. Keras 모델 로드 중...: {MODEL_PATH}")
        return keras.models.load_model(MODEL_PATH)

    def _create_feature_extractor(self):
        print("2. 특징 추출 모델 생성 중...")
        try:
            return keras.Model(inputs=self.original_model.input, outputs=self.original_model.get_layer(FEATURE_EXTRACTOR_LAYER_NAME).output)
        except ValueError as e:
            raise ValueError(f"'{FEATURE_EXTRACTOR_LAYER_NAME}' 레이어를 찾을 수 없습니다. {e}")

    def _load_label_encoder(self):
        if not LABEL_ENCODER_PATH.exists(): raise FileNotFoundError(f"레이블 인코더 파일을 찾을 수 없습니다: {LABEL_ENCODER_PATH}")
        print(f"3. 레이블 인코더 로드 중...: {LABEL_ENCODER_PATH}")
        with open(LABEL_ENCODER_PATH, 'rb') as f: return pickle.load(f)

    def _connect_chromadb(self):
        if not DB_PERSIST_PATH.exists(): raise FileNotFoundError(f"ChromaDB 데이터베이스 폴더을 찾을 수 없습니다: {DB_PERSIST_PATH}")
        print(f"4. ChromaDB 연결 중...: {DB_PERSIST_PATH}")
        client = chromadb.PersistentClient(path=str(DB_PERSIST_PATH))
        return client.get_collection(name=COLLECTION_NAME)

    def _preprocess_input(self, landmark_data: np.ndarray) -> np.ndarray:
        processed = np.nan_to_num(landmark_data).astype(np.float32)
        t, F = processed.shape
        if t != cf.SEQUENCE_LENGTH:
            if t > cf.SEQUENCE_LENGTH:
                idx = np.linspace(0, t - 1, cf.SEQUENCE_LENGTH).astype(int)
                processed = processed[idx]
            else:
                pad = np.zeros((cf.SEQUENCE_LENGTH - t, F), dtype=processed.dtype)
                processed = np.vstack([processed, pad])
        return np.expand_dims(processed, axis=0)

    def get_top_predictions(self, processed_data: np.ndarray, top_k: int = 5):
        predictions = self.original_model.predict(processed_data, verbose=0)[0]
        top_indices = predictions.argsort()[-top_k:][::-1]
        return [(self.label_encoder.inverse_transform([i])[0], predictions[i]) for i in top_indices]

    def search_similar_motions(self, processed_data: np.ndarray, n_results: int = 5):
        motion_vector = self.feature_extractor_model.predict(processed_data, verbose=0)
        return self.chroma_collection.query(query_embeddings=[motion_vector.flatten().tolist()], n_results=n_results)

    def _build_llm_verification_prompt(self, top_predictions, similar_motions):
        prompt = "당신은 수어 인식 전문가입니다. 머신러닝 모델의 분석 결과를 바탕으로 가장 정확한 수어 단어 하나를 최종 결정해야 합니다.\n\n"
        prompt += "--- 1차 분석: 분류 모델 예측 결과 ---\n"
        for word, prob in top_predictions: prompt += f"- {word}: {prob:.2%}\n"
        prompt += "\n--- 2차 분석: 유사 동작 검색 결과 ---\n"
        if similar_motions and similar_motions['metadatas'] and len(similar_motions['metadatas'][0]) > 0:
            for metadata in similar_motions['metadatas'][0]: prompt += f"- 유사 동작: {metadata.get('label', 'N/A')}\n"
        else: prompt += "- 유사한 동작을 찾지 못했습니다.\n"
        prompt += "\n--- 최종 임무 ---\n"
        prompt += "위의 1차, 2차 분석 결과를 모두 고려하여, 입력된 동작이 어떤 단어일지 최종 판단을 내려주세요. 답변은 다른 설명 없이, 오직 최종 결정된 단어 하나만 말해야 합니다."
        return prompt

    def recognize_sign_with_llm(self, landmark_data: np.ndarray):
        processed_data = self._preprocess_input(landmark_data)
        top_predictions = self.get_top_predictions(processed_data, top_k=3)
        similar_motions = self.search_similar_motions(processed_data, n_results=3)
        prompt = self._build_llm_verification_prompt(top_predictions, similar_motions)
        response = self.openai_client.chat.completions.create(
            model="gpt-4o", messages=[{"role": "system", "content": "당신은 수어 인식 전문가입니다."}, {"role": "user", "content": prompt}], temperature=0.0
        )
        return response.choices[0].message.content.strip()

    def get_chatbot_response(self, text: str):
        prompt = f"당신은 친절한 AI 비서입니다. 사용자가 수어로 말을 걸었습니다. 사용자의 말: '{text}'. 이에 대해 자연스럽게 대답해주세요."
        response = self.openai_client.chat.completions.create(
            model="gpt-4o", messages=[{"role": "system", "content": "당신은 친절한 AI 비서입니다."}, {"role": "user", "content": prompt}], temperature=0.7
        )
        return response.choices[0].message.content

    def recognize_sentence(self, segmented_signs: List[np.ndarray]):
        """
        분절된 여러 개의 수어 동작을 받아 전체 문장을 인식하고,
        그 문맥에 맞는 챗봇 응답을 생성합니다.
        """
        recognized_words = []
        for sign_landmarks in segmented_signs:
            word = self.recognize_sign_with_llm(sign_landmarks)
            if word:
                recognized_words.append(word)
        
        if not recognized_words:
            return None, "단어를 하나도 인식하지 못했습니다."

        full_sentence = " ".join(recognized_words)
        chatbot_response = self.get_chatbot_response(full_sentence)
        return full_sentence, chatbot_response

# FastAPI에서 사용할 싱글톤 인스턴스 생성
sign_recognizer = SignRecognizer()