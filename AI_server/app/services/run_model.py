import sys
import os
import numpy as np
from pathlib import Path
from dotenv import load_dotenv
from typing import List

# .env 파일에서 환경 변수 로드
load_dotenv()

def classification(data):
    """
    수어 동작을 분류하는 함수 (임시 구현)
    
    Args:
        data: 랜드마크 데이터
        
    Returns:
        분류된 단어
    """
    # TODO: 실제 모델을 사용한 분류 로직 구현
    # 현재는 임시로 랜덤 단어를 반환
    import random
    sample_words = ["안녕", "고마워", "미안해", "사랑해", "날씨", "음식", "물", "집", "학교", "친구"]
    return random.choice(sample_words)