import sys
import os
from dotenv import load_dotenv
from typing import List
import httpx

load_dotenv()

GMS_KEY = os.getenv("GMS_KEY")
LLM_MODEL = os.getenv("LLM_MODEL")
LLM_URL = os.getenv("LLM_URL")

async def start_chat(subject: str, level: int):
    # 환경 변수가 설정되지 않은 경우 대체 응답
    if GMS_KEY == "dummy_key" or not GMS_KEY:
        level_responses = {
            1: f"안녕하세요! {subject}에 대해 쉽게 이야기해봐요!",
            2: f"안녕하세요! {subject}에 대해 자세히 이야기해보겠습니다.",
            3: f"안녕하세요! {subject}에 대해 전문적으로 논의해보겠습니다."
        }
        return level_responses.get(level, f"안녕하세요! {subject}에 대해 이야기해봐요!")
    
    prompt = f"""
    당신은 1, 2, 3 단계 난이도의 챗봇 중 {level} 단계의 챗봇입니다.
    해당 레벨은 숫자가 커질수록 어려운 어휘를 구사하는 챗봇입니다.
    
    현재 당신이 수행해야 할 대화는 {subject}에 관한 대화입니다.
    
    아래에 주어지는 예시와 비슷하게 대화하되 난이도를 고려하여주세요.
    
    만약 주제가 날씨라면 : "안녕하세요 사용자님. 오늘은 날씨가 어떤가요?"
    
    """
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {GMS_KEY}"
    }
    payload = {
        "model": LLM_MODEL,
        "messages": [
            {"role": "system", "content": "당신은 수어 학습을 하려는 사람을 위한 친절한 AI 챗봇입니다. 입력에 대한 답변을 하세요."},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.7,
        "max_tokens": int(level * 50)  # 토큰 수 강제로 작게해서 짧은 답변 위주로 나오게.
    }

    try:
        async with httpx.AsyncClient(verify=False, timeout=15.0) as client:
            response = await client.post(LLM_URL, headers=headers, json=payload)
            response.raise_for_status()
            result = response.json()

        reason = result["choices"][0]["message"]["content"].strip()
        return str(reason)
    except Exception as e:
        # API 호출 실패 시 대체 응답
        level_responses = {
            1: f"안녕하세요! {subject}에 대해 쉽게 이야기해봐요!",
            2: f"안녕하세요! {subject}에 대해 자세히 이야기해보겠습니다.",
            3: f"안녕하세요! {subject}에 대해 전문적으로 논의해보겠습니다."
        }
        return level_responses.get(level, f"안녕하세요! {subject}에 대해 이야기해봐요!")

async def text_to_motion_language(text: str):
    """
    텍스트를 수어 동작으로 변환하는 함수 (구현 예정)
    """
    # TODO: 텍스트를 수어 동작으로 변환하는 로직 구현
    return f"수어 동작: {text}"