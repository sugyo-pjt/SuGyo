# 수어 인식 모델 환경 설정 가이드

## 🚨 충돌 해결 단계

### 1단계: 기존 환경 정리
```bash
# 가상환경 생성 (권장)
python -m venv sign_language_env
sign_language_env\Scripts\activate  # Windows
# source sign_language_env/bin/activate  # Linux/Mac

# 기존 패키지 제거
pip uninstall tensorflow keras tensorboard -y
pip uninstall opencv-python opencv-contrib-python opencv-python-headless -y
pip uninstall torch torchaudio torchvision -y
pip uninstall jax jaxlib -y
```

### 2단계: 핵심 패키지 설치
```bash
# TensorFlow 설치
pip install tensorflow==2.20.0

# Keras 설치 (TensorFlow와 함께 설치됨)
pip install keras==3.11.3

# 데이터 처리 라이브러리
pip install numpy==1.26.4 pandas==2.3.2 scipy==1.15.3

# 컴퓨터 비전
pip install opencv-contrib-python==4.10.0.84

# MediaPipe (수어 인식 핵심)
pip install mediapipe==0.10.21

# 머신러닝
pip install scikit-learn==1.5.2

# 시각화
pip install matplotlib==3.10.6

# Jupyter 환경
pip install jupyter ipykernel ipython

# 기타 유틸리티
pip install tqdm pillow h5py
```

### 3단계: 설치 확인
```bash
# Python에서 확인
python -c "import tensorflow as tf; print('TensorFlow:', tf.__version__)"
python -c "import keras; print('Keras:', keras.__version__)"
python -c "import cv2; print('OpenCV:', cv2.__version__)"
python -c "import mediapipe as mp; print('MediaPipe:', mp.__version__)"
```

## 🔧 문제 해결

### CUDA 관련 문제
```bash
# CUDA 버전 확인
nvidia-smi

# TensorFlow GPU 지원 확인
python -c "import tensorflow as tf; print('GPU 사용 가능:', tf.config.list_physical_devices('GPU'))"
```

### 메모리 부족 문제
```bash
# 배치 크기 줄이기 (config.py에서)
BATCH_SIZE = 16  # 32에서 16으로 줄이기
```

### OpenCV 충돌 문제
```bash
# OpenCV 재설치
pip uninstall opencv-python opencv-contrib-python opencv-python-headless -y
pip install opencv-contrib-python==4.10.0.84
```

## 📝 최종 확인

모든 패키지가 정상적으로 설치되었는지 확인:

```python
# test_environment.py
import tensorflow as tf
import keras
import cv2
import mediapipe as mp
import numpy as np
import pandas as pd
import sklearn

print("✅ 모든 패키지가 정상적으로 설치되었습니다!")
print(f"TensorFlow: {tf.__version__}")
print(f"Keras: {keras.__version__}")
print(f"OpenCV: {cv2.__version__}")
print(f"MediaPipe: {mp.__version__}")
print(f"NumPy: {np.__version__}")
print(f"Pandas: {pd.__version__}")
print(f"Scikit-learn: {sklearn.__version__}")
```

