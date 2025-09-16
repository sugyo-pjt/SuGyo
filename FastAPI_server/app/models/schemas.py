from pydantic import BaseModel, Field
from typing import List

class SentenceInput(BaseModel):
    """한 문장을 구성하는, 분절된 여러 수어 동작 리스트를 입력받기 위한 모델"""
    segmented_signs: List[List[List[float]]] = Field(
        ..., 
        example=[
            [[0.1, 0.2, ...], [0.11, 0.22, ...]], # 첫 번째 단어의 랜드마크 리스트
            [[0.3, 0.4, ...], [0.31, 0.41, ...]]  # 두 번째 단어의 랜드마크 리스트
        ],
        description="분절된 각 수어 단어의 (프레임 수, 랜드마크 차원) 랜드마크 리스트들의 리스트"
    )

class SentenceResponse(BaseModel):
    """인식된 문장 전체와 그에 대한 챗봇 응답 결과를 반환하기 위한 모델"""
    recognized_sentence: str = Field(..., example="오늘 날씨 어때", description="인식된 수어 단어들을 조합한 전체 문장")
    chatbot_response: str = Field(..., example="오늘 서울의 날씨는 맑고 화창합니다!", description="인식된 문장 전체에 대한 챗봇의 응답")

class WordInput(BaseModel):
    input_data: dict = Field(..., description="사용자 ID와 랜드마크 데이터를 포함한 딕셔너리")

class WordResponse(BaseModel):
    result: dict = Field(..., description="분류 결과를 포함한 딕셔너리")