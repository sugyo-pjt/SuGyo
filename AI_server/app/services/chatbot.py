import os
import asyncio
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

# 시스템 프롬프트
SYSTEM_PROMPT = (
    "한국어로만 대답하세요. 사용자는 기초 수어 학습자입니다.\n"
    "규칙:\n"
    "1) 사용자의 입력은 해당 문장을 수어 문법에 맞는 순서로 바꿔 달라는 것.\n"
    "2) 쉬운 어휘, 최대한 간략하고 쉬운 단어로 구성.\n"
    "3) 설명을 제외한 당신의 처음 답변은 단순한 단어의 나열로 제공해야 합니다. "
    "내일 치킨 먹으러 갈래? -> 우리 먹다 치킨 내일\n"
    "4) 사용자의 문장을 단어의 나열로 표현한 이후에는 답변의 단어들로 문장을 구성한 이유를 "
    "수어 문법 기반으로 매우 간략하게 알려줘야 합니다.\n"
    "5) 당신의 답변에서 비언어적인 표현이 어디 들어가야 하는지도 알려주세요.\n"
    "6) 비언어적 표현은 가장 중요한 단어 하나정도만 살짝 자세하게 알려주세요."
)

# 유저 프롬프트 템플릿
USER_PROMPT_TEMPLATE = (
    "아래 문장은 사용자의 수어 인식 결과를 단순히 이어붙인 것입니다.\n"
    "사용자 문장: {sentence}\n\n"
    "이 문장을 사람이 말하듯 자연스럽게 변환해서 이해하고, "
    "시스템 프롬프트 규칙에 맞춰 아주 간단히 답하세요.\n"
    "예시:\n"
    "사용자: 내일 집에 아빠 온대. 나가서 치킨 먹을까?\n"
    "챗봇: 내일 집 아빠 오다. 우리 치킨 먹다 나가다?\n"
    "의문은 말 끝을 올리고 **의문 표정(눈썹 올리기, 고개 기울이기)**으로 표현합니다."
)


# LLM 호출 (히스토리 없음)
async def chatting(sentence: str) -> str:
    messages = [
        {"role": "system", "content": SYSTEM_PROMPT},
        {"role": "user", "content": USER_PROMPT_TEMPLATE.format(sentence=sentence)},
    ]

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {GMS_KEY}",
    }
    payload = {
        "model": "gpt-4.1-mini",
        "messages": messages,
        "max_tokens": 600,
        "temperature": 0.4,
    }

    async with httpx.AsyncClient(verify=False, timeout=10.0) as client:
        resp = await client.post(CHAT_LLM_URL, headers=headers, json=payload)
        resp.raise_for_status()
        data = resp.json()

    reply = (
        data.get("choices", [{}])[0]
        .get("message", {})
        .get("content", "")
        .strip()
    )

    if not reply:
        reply = "죄송합니다. 응답을 생성하지 못 했습니다. 다시 한 번 질문해주세요."

    return reply


# 테스트 실행 예시 (서버 없이)
# if __name__ == "__main__":
#     print(asyncio.run(chatting("나 치킨 먹다가 이빨 부러졌어")))
