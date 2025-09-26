
from fastapi import APIRouter, HTTPException
from app.models.schemas import SignInput, TextInput, ChatbotOutput
from app.services.chatbot import chatting
from app.services.Dense import classification
import numpy as np

router = APIRouter()

@router.get("/health", response_model=str)
async def health():
    """서버의 상태를 확인합니다."""
    return "OK"

# 챗봇과 수어로 대화하는 엔드포인트
@router.post("/chatbot", response_model=ChatbotOutput)
async def sign_to_text_chat(input_data: SignInput):
    """수어 동작 시퀀스를 받아 텍스트로 변환하고, 챗봇 응답을 반환합니다."""
    try:
        recognized_words = []
        for sequence in input_data.sequences:
            # sequence가 리스트일 경우 numpy 배열로 변환
            np_sequence = np.array(sequence, dtype=np.float32)
            word = classification(np_sequence)
            recognized_words.append(word)
        
        # 공백을 사이에 두고 단어들을 합쳐 문장 생성
        sentence = " ".join(recognized_words)
        
        if not sentence:
            raise HTTPException(status_code=400, detail="수어 동작을 단어로 변환하지 못했습니다.")

        # 챗봇 서비스 호출
        chatbot_response = await chatting(sentence)
        
        return ChatbotOutput(result=chatbot_response)

    except RuntimeError as e:
        # 모델이 로드되지 않은 경우 등 서비스 내부 오류
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        # 기타 예외 처리
        raise HTTPException(status_code=500, detail=f"처리 중 오류가 발생했습니다: {e}")

# 텍스트로 챗봇과 대화하는 엔드포인트
@router.post("/chat", response_model=ChatbotOutput)
async def text_chat(input_data: TextInput):
    """텍스트 문장을 받아 챗봇 응답을 반환합니다."""
    try:
        sentence = input_data.sentence
        chatbot_response = await chatting(sentence)
        return ChatbotOutput(result=chatbot_response)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"처리 중 오류가 발생했습니다: {e}")
