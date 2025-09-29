# fastapi/Dockerfile

# 1. Base Image: 파이썬 3.11 슬림 버전을 사용하여 이미지 경량화
FROM python:3.10-slim

# 2. Working Directory: 컨테이너 내 작업 디렉토리 설정
WORKDIR /app

# 3. Copy & Install Dependencies: 의존성을 먼저 설치하여 Docker 빌드 캐시를 활용
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# 4. Copy Source Code: 애플리케이션 소스 코드 복사
COPY . .

# 6. Run Command: 컨테이너 시작 시 uvicorn 서버 실행
#    --host 0.0.0.0: 컨테이너 외부에서의 접속을 허용
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]