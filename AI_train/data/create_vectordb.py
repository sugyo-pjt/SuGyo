import os
from dotenv import load_dotenv
import chromadb
from chromadb.config import Settings
from openai import OpenAI

load_dotenv()

# Solar API
SOLAR_KEY = os.getenv("SOLAR_KEY")
solar_embedding_model = "solar-embedding-1-large-query"

# ChromaDB 초기화 (로컬 디렉토리에 저장)
chroma_client = chromadb.PersistentClient(path="./chroma_db")

# 콜렉션 생성 (없으면 새로, 있으면 불러오기)
collection = chroma_client.get_or_create_collection(
    name="sign-language-db",
    metadata={"hnsw:space": "cosine"}  # 벡터 유사도 metric
)

# Solar API 클라이언트
solar_client = OpenAI(api_key=SOLAR_KEY, base_url="https://api.upstage.ai/v1")

# 저장할 단어
sample_text = ["안녕하세요", "반갑다", "나", "너"]

# 🔹 1. Solar 임베딩 생성
embeds = []
for t in sample_text:
    response = solar_client.embeddings.create(
        model=solar_embedding_model,
        input=t
    )
    embeds.append(response.data[0].embedding)

# 🔹 2. ChromaDB에 저장
collection.add(
    embeddings=embeds,
    documents=sample_text,
    ids=[f"txt-{i}" for i in range(len(sample_text))]
)

print("✅ Solar 임베딩 ChromaDB 저장 완료!")

# 🔹 3. 검색 예시
query_text = "나"
query_vector = solar_client.embeddings.create(
    model=solar_embedding_model,
    input=query_text
).data[0].embedding

results = collection.query(
    query_embeddings=[query_vector],
    n_results=3
)

print("🔎 검색 결과:")
for doc, id in zip(results["documents"][0], results["ids"][0]):
    print(f"ID: {id}, Word: {doc}")
