from pydantic import BaseModel, Field

class Motion(BaseModel):
    dot: list
    
class chatInput(BaseModel):
    subject: str = Field(..., description="대화 주제 (예: 날씨, 음식, 취미 등)")
    level: int = Field(..., ge=1, le=3, description="챗봇 난이도 (1: 쉬움, 2: 보통, 3: 어려움)")

class chatOutput(BaseModel):
    result: str = Field(..., description="챗봇의 응답 메시지")