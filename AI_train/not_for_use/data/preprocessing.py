
import cv2
import mediapipe as mp
import json
import os
import numpy as np
import pandas as pd
from pathlib import Path
import logging

class KSLDataProcessor:
    def __init__(self, data_root_path):
        '''
        AI-Hub 수어 데이터셋 처리를 위한 클래스

        Args:
            data_root_path: AI-Hub에서 다운받은 데이터 루트 경로
        '''
        self.data_root = Path(data_root_path)

        # MediaPipe 초기화
        self.mp_holistic = mp.solutions.holistic
        self.mp_drawing = mp.solutions.drawing_utils
        self.holistic = self.mp_holistic.Holistic(
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5,
            model_complexity=2
        )

        # 로깅 설정
        logging.basicConfig(level=logging.INFO)
        self.logger = logging.getLogger(__name__)

    def load_aihub_annotations(self, annotation_path):
        '''
        AI-Hub 어노테이션 파일에서 단어 라벨 추출
        OpenPose 좌표는 무시하고 라벨 정보만 활용
        '''
        with open(annotation_path, 'r', encoding='utf-8') as f:
            data = json.load(f)

        # 단어 라벨만 추출 (OpenPose 좌표 데이터는 건드리지 않음)
        word_labels = []
        for item in data['annotations']:
            word_labels.append({
                'video_id': item['video_id'],
                'word_gloss': item['word_gloss'],  # 수어 단어
                'korean_text': item['korean_text'], # 한국어 텍스트
                'start_frame': item['start_frame'],
                'end_frame': item['end_frame']
            })

        return word_labels

    def extract_mediapipe_landmarks(self, video_path):
        '''
        비디오에서 MediaPipe로 새로운 landmark 좌표 추출
        '''
        cap = cv2.VideoCapture(str(video_path))

        if not cap.isOpened():
            self.logger.error(f"비디오 파일을 열 수 없습니다: {video_path}")
            return None

        landmarks_sequence = []
        frame_count = 0

        while True:
            ret, frame = cap.read()
            if not ret:
                break

            # RGB 변환 (MediaPipe는 RGB 입력 필요)
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # MediaPipe 처리
            results = self.holistic.process(rgb_frame)

            # landmark 좌표 추출
            frame_landmarks = self.extract_frame_landmarks(results)
            landmarks_sequence.append(frame_landmarks)
            frame_count += 1

        cap.release()

        self.logger.info(f"총 {frame_count}프레임에서 landmark 추출 완료")
        return np.array(landmarks_sequence)

    def extract_frame_landmarks(self, results):
        '''
        한 프레임에서 MediaPipe landmark 좌표 추출
        '''
        frame_data = {
            'pose': [],
            'left_hand': [],
            'right_hand': [],
            'face': []
        }

        # Pose landmarks (33개 포인트)
        if results.pose_landmarks:
            for landmark in results.pose_landmarks.landmark:
                frame_data['pose'].extend([landmark.x, landmark.y, landmark.z])
        else:
            frame_data['pose'] = [0.0] * (33 * 3)  # 빈 값으로 채우기

        # Left hand landmarks (21개 포인트)  
        if results.left_hand_landmarks:
            for landmark in results.left_hand_landmarks.landmark:
                frame_data['left_hand'].extend([landmark.x, landmark.y, landmark.z])
        else:
            frame_data['left_hand'] = [0.0] * (21 * 3)

        # Right hand landmarks (21개 포인트)
        if results.right_hand_landmarks:
            for landmark in results.right_hand_landmarks.landmark:
                frame_data['right_hand'].extend([landmark.x, landmark.y, landmark.z])
        else:
            frame_data['right_hand'] = [0.0] * (21 * 3)

        # Face landmarks (468개 포인트 - 필요시 추가)
        # 수어에서는 얼굴 표정도 중요하므로 포함 가능
        if results.face_landmarks:
            face_points = []
            # 주요 얼굴 포인트만 선택 (예: 입술, 눈썹 등)
            key_face_indices = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]  # 예시
            for i in key_face_indices:
                landmark = results.face_landmarks.landmark[i]
                face_points.extend([landmark.x, landmark.y, landmark.z])
            frame_data['face'] = face_points
        else:
            frame_data['face'] = [0.0] * (10 * 3)  # 주요 포인트만

        # 모든 좌표를 하나의 벡터로 결합
        all_landmarks = (frame_data['pose'] + 
                        frame_data['left_hand'] + 
                        frame_data['right_hand'] + 
                        frame_data['face'])

        return all_landmarks

    def process_dataset(self, output_path):
        '''
        전체 데이터셋 처리
        '''
        processed_data = []

        # AI-Hub 데이터 구조에 맞게 수정 필요
        video_files = list(self.data_root.glob("**/*.mp4"))
        annotation_files = list(self.data_root.glob("**/*.json"))

        for video_file in video_files:
            try:
                # 해당 비디오의 어노테이션 파일 찾기
                annotation_file = self.find_matching_annotation(video_file, annotation_files)

                if annotation_file:
                    # 라벨 정보 로드
                    labels = self.load_aihub_annotations(annotation_file)

                    # MediaPipe로 새로운 landmark 추출
                    landmarks = self.extract_mediapipe_landmarks(video_file)

                    if landmarks is not None:
                        processed_data.append({
                            'video_path': str(video_file),
                            'landmarks': landmarks,
                            'labels': labels
                        })

                        self.logger.info(f"처리 완료: {video_file.name}")

            except Exception as e:
                self.logger.error(f"처리 실패: {video_file.name}, 오류: {str(e)}")
                continue

        # 처리된 데이터 저장
        self.save_processed_data(processed_data, output_path)

    def find_matching_annotation(self, video_file, annotation_files):
        '''
        비디오 파일에 해당하는 어노테이션 파일 찾기
        '''
        video_name = video_file.stem
        for ann_file in annotation_files:
            if video_name in ann_file.stem:
                return ann_file
        return None

    def save_processed_data(self, processed_data, output_path):
        '''
        처리된 데이터를 학습용 포맷으로 저장
        '''
        output_dir = Path(output_path)
        output_dir.mkdir(exist_ok=True)

        # CSV 형태로 저장 (메타데이터)
        metadata = []
        for i, item in enumerate(processed_data):
            for label in item['labels']:
                metadata.append({
                    'sample_id': i,
                    'video_path': item['video_path'],
                    'word_gloss': label['word_gloss'],
                    'korean_text': label['korean_text'],
                    'landmarks_file': f"landmarks_{i}.npy"
                })

        df = pd.DataFrame(metadata)
        df.to_csv(output_dir / "dataset_metadata.csv", index=False)

        # Landmark 데이터는 NumPy 파일로 저장
        for i, item in enumerate(processed_data):
            np.save(output_dir / f"landmarks_{i}.npy", item['landmarks'])

        self.logger.info(f"처리 완료: {len(processed_data)}개 샘플이 {output_path}에 저장됨")

# 사용 예시
def main():
    # AI-Hub에서 다운받은 데이터 경로
    data_root = "C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_server\hand_lang"
    output_path = "C:\Users\SSAFY\Desktop\WANG\S13P21A602\AI_server\data"

    # 데이터 처리기 초기화
    processor = KSLDataProcessor(data_root)

    # 전체 데이터셋 처리
    processor.process_dataset(output_path)

    print("데이터 처리 완료!")
    print("이제 CNN+LSTM 모델 학습에 사용할 수 있습니다.")

if __name__ == "__main__":
    main()
