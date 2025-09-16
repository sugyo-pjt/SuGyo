import os
from dotenv import load_dotenv
import chromadb
from chromadb.config import Settings
from openai import OpenAI

load_dotenv()

# Solar API
SOLAR_KEY = os.getenv("SOLAR_KEY")
solar_embedding_model = "solar-embedding-1-large-query"
solar_client = OpenAI(api_key=SOLAR_KEY, base_url="https://api.upstage.ai/v1")

# Solar API
SOLAR_KEY = os.getenv("SOLAR_KEY")
solar_embedding_model = "solar-embedding-1-large-query"

chroma_client = chromadb.PersistentClient(path="./chroma_db")
collection = chroma_client.get_or_create_collection(
    name="sign-language-db",
    metadata={"hnsw:space": "cosine"}  # 벡터 유사도 metric
)

# 🔹 3. 검색 예시
query_text = "본인"
query_vector = solar_client.embeddings.create(
    model=solar_embedding_model,
    input=query_text
).data[0].embedding

results = collection.query(
    query_embeddings=[query_vector],
    n_results=5
)

print("🔎 검색 결과:")
for doc, id in zip(results["documents"][0], results["ids"][0]):
    print(f"ID: {id}, Word: {doc}")