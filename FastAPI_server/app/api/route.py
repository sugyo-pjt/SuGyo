
from fastapi import APIRouter, HTTPException
from app.models.schemas import SentenceInput, SentenceResponse, WordInput, WordResponse
from app.models.input_models import chatInput, chatOutput
from app.services.chatbot import start_chat
from app.services.run_model import classification
import numpy as np

router = APIRouter()

@router.get("/health", response_model=str)
async def health():
    """서버의 상태를 확인합니다."""
    return "OK"

# 얘는 일단은 챗봇 시작하는 함수만 간단하게 구현해 둠. 나중에 크게 바뀔 예정.
@router.post("/chat/start", response_model=chatOutput)
async def chat(input_data: chatInput):
    """
    챗봇 대화를 시작합니다.
    
    Args:
        input_data: 대화 주제와 난이도 정보
        
    Returns:
        챗봇의 초기 응답 메시지
    """
    try:
        result = await start_chat(input_data.subject, input_data.level)
        return chatOutput(result=result)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"챗봇 응답 생성 중 오류 발생: {str(e)}")


# 요거는 노래에서 단어 분류하는 것. 일단 만들고 있는데 user_id가 필요없음?
# 준오형이 까서 해준다고 함.
@router.post("/api/v1/game/rhythm/play", response_model=WordResponse)
async def word_classification(input_data: WordInput):
    """
    단일 수어 동작을 분류합니다.
    
    Args:
        input_data: 사용자 ID와 랜드마크 데이터
        
    Returns:
        분류된 단어 결과
    """
    try:
        # WordInput 모델이 dict 타입이므로 직접 접근
        user_id = input_data.input_data.get("user_id")
        landmarks = input_data.input_data.get("frames")
        
        if not user_id or landmarks is None:
            raise ValueError("user_id와 landmarks가 필요합니다.")
        
        classified_word = classification(landmarks)
        
        result = {
            "user_id": user_id,
            "word": classified_word
        }
        
        return WordResponse(result=result)
        
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"단어 분류 중 오류 발생: {str(e)}")