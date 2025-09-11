import os
from dotenv import load_dotenv
import chromadb
from chromadb.config import Settings
from openai import OpenAI
import time
import asyncio

load_dotenv()

GMS_KEY = os.getenv("GMS_KEY")
GPT_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1/embeddings"