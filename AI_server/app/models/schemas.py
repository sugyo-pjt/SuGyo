from pydantic import BaseModel
from typing import List

class ChatRequest(BaseModel):
    user_id: str
    sentence: str

class ChatMessage(BaseModel):
    role: str
    content: str

class ChatResponse(BaseModel):
    reply: str
    history: List[ChatMessage]
    
class ChatbotOutput(BaseModel):
    result: str
    history: List[ChatMessage]