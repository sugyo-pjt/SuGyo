import os
import asyncio
from typing import Optional
import httpx
from dotenv import load_dotenv
load_dotenv()

GMS_KEY = os.getenv("GMS_KEY")
CHAT_LLM_URL = os.getenv("CHAT_LLM_URL")

def _require_env(name: str) -> str:
    v = os.getenv(name)
    if not v:
        raise RuntimeError(f"환경변수 {name}가 설정되지 않았습니다.")
    return v

SYSTEM_PROMPT = (
    "한국어로만 대답하세요. 사용자는 기초 수어 학습자입니다.\n"
    "규칙:\n"
    "1) 사용자의 텔레그래픽 문장을 자연스러운 구어체로 바꿔 이해한다.\n"
    "2) 쉬운 어휘, 짧은 문장(최대 2~3문장)으로 답한다.\n"
    "3) 마지막에 짧은 질문 1개만 덧붙여 대화를 이어간다. 해당 질문은 사용자가 수어 초보자임을 고려하여 수어로 답하기 쉬운 문장이어야 한다.\n"
    "4) 과한 이모지 금지. 존댓말 유지. 전문용어/긴 설명 금지."
)

USER_PROMPT_TEMPLATE = (
    "아래 문장은 사용자의 수어 인식 결과를 단순히 이어붙인 것입니다.\n"
    "사용자 문장: {sentence}\n\n"
    "이 문장을 사람이 말하듯 자연스럽게 변환해서 이해하고, 시스템 프롬프트 규칙에 맞춰 아주 간단히 답하세요.\n"
    "아래에 주어지는 예시를 참고해서 대화를 진행해줘.\n"
    "예시:\n"
    "사용자: 안녕.\n"
    "챗봇: 안녕하세요. 오늘 하루는 어떠셨어요?\n"
    "사용자: 오늘 비 슬프다.\n"
    "챗봇: 오늘 비가 와서 마음이 가라앉았군요. 지금은 조금 괜찮으세요?\n"
    "사용자: 저녁 된장찌개\n"
    "챗봇: 저녁에 된장찌개 드셨군요. 맛있었나요?\n"
)

async def chatting(sentence: str) -> str:
    # 환경변수 확인. 싸피에서 이거 자꾸 바꿈
    print(CHAT_LLM_URL)
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {GMS_KEY}",
    }

    payload = {
        "model": "gpt-4.1-mini",
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": USER_PROMPT_TEMPLATE.format(sentence=sentence.strip())},
        ],
        "max_tokens": 400,     
        "temperature": 0.5,    # 무작위성 보정정
        "top_p": 0.9,
    }

    async with httpx.AsyncClient(verify=False, timeout=10.0) as client:
        resp = await client.post(CHAT_LLM_URL, headers=headers, json=payload)
        resp.raise_for_status()
        data = resp.json()

    content = (
        data.get("choices", [{}])[0]
            .get("message", {})
            .get("content")
    ) or data.get("output") or data.get("text") or ""

    content = content.strip()
    if not content:
        raise ValueError("LLM 응답이 비어 있습니다.")

    return content  # 문자열

# --- 사용 예시 ---
# import asyncio
# print(asyncio.run(chatting("오늘 비")))
