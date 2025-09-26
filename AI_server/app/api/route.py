
from fastapi import APIRouter, HTTPException
from app.models.schemas import ChatMessage, ChatRequest, ChatResponse, ChatbotOutput
from app.services.chatbot import chatting, chat_histories
from app.services.Dense import classification
import numpy as np

router = APIRouter()

@router.get("/health", response_model=str)
async def health():
    """서버의 상태를 확인합니다."""
    return "OK"

# 사용자 채팅을 받아 답변 생성
@router.post("/chat", response_model=ChatbotOutput)
async def text_chat(input_data: ChatRequest):
    try:
        reply, history = await chatting(input_data.user_id, input_data.sentence)
        return ChatbotOutput(
            result=reply,
            history=[ChatMessage(**h) for h in history[-5:]]  # 최근 5개만 반환. 사실 이거 5개까지도 애매하긴 한데 그래도 문맥 파악하려면..
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"오류 코드는 {e}")


#유저별 전체 대화 히스토리 조회. 메모리에 저장해서 받아올거임. 확장하면 db에 저장하거나 json으로 따로 저장할건데 일단은 이렇게 간단하게 해둠
@router.get("/history/{user_id}", response_model=ChatResponse)
async def get_history(user_id: str):
    history = chat_histories.get(user_id, [])
    return ChatResponse(
        reply="",
        history=[ChatMessage(**h) for h in history]
    )