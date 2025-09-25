from pydantic import BaseModel, Field
from typing import List, Any

class SignInput(BaseModel):
    user_id: str = Field(..., description="사용자 ID")
    sequences: List[List[Any]] = Field(..., description="수어 동작 시퀀스 데이터")

class TextInput(BaseModel):
    sentence: str = Field(..., description="번역할 텍스트 문장")

class ChatbotOutput(BaseModel):
    result: str = Field(..., description="챗봇의 응답 메시지")