from pydantic import BaseModel

class Motion(BaseModel):
    dot: list
    
class LandmarkInput(BaseModel):
    input: dict

class LandmarkOutput(BaseModel):
    result: dict

class ChatbotInput(BaseModel):
    input: dict
    
class ChatBotOutput(BaseModel):
    result: dict