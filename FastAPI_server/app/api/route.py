
from fastapi import APIRouter, HTTPException
from app.models.schemas import SentenceInput, SentenceResponse, WordInput, WordResponse
from app.services.media import sign_recognizer
from app.services.run_model import classification
import numpy as np

router = APIRouter()

@router.get("/health", response_model=str)
async def health():
    """서버의 상태를 확인합니다."""
    return "OK"

@router.get("/word-classification")
async def word_classification(input_data = WordInput):
    data = input_data
    user_id = data["user_id"]
    landmarks = data["landmarks"]
    result = {
        "user_id": user_id,
        "word": classification(landmarks)
    }
    
    return result
    
@router.post("/recognize-sentence", response_model=SentenceResponse)
async def recognize_sentence_endpoint(sentence_input: SentenceInput):
    """
    분절된 수어 동작들의 리스트를 받아 전체 문장을 인식하고, 문맥에 맞는 챗봇 응답을 반환합니다.
    """
    try:
        # 1. 입력 데이터를 Numpy 배열의 리스트로 변환
        segmented_signs_np = [
            np.array(sign, dtype=np.float32) for sign in sentence_input.segmented_signs
        ]
        
        if not segmented_signs_np:
            raise ValueError("입력된 수어 동작 데이터가 없습니다.")

        # 2. 서비스 호출: 문장 인식 및 챗봇 응답 생성
        recognized_sentence, chatbot_response = sign_recognizer.recognize_sentence(segmented_signs_np)
        
        if recognized_sentence is None:
            raise ValueError(chatbot_response) # 서비스에서 전달된 오류 메시지

        # 3. 최종 결과 반환
        return SentenceResponse(
            recognized_sentence=recognized_sentence,
            chatbot_response=chatbot_response
        )
    
    except FileNotFoundError as e:
        raise HTTPException(status_code=500, detail=f"서버 설정 오류: {e}")
    except ValueError as e:
        raise HTTPException(status_code=400, detail=f"입력 데이터 오류: {e}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"서버 내부 오류: {e}")
