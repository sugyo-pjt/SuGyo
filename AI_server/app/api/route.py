from fastapi import APIRouter, UploadFile, File
from fastapi.responses import JSONResponse, PlainTextResponse
from AI_server.app.models.schemas import LandmarkInput, LandmarkOutput, ChatBotOutput, ChatbotInput, Motion


router = APIRouter()

@router.get("/health", response_class=PlainTextResponse)
async def health():
    return "ok"

@router.get("/sing")
async def translate(input_data = LandmarkInput):
    pass

@router.get("/chatbot")
async def translate(input_data = ChatbotInput):
    pass

