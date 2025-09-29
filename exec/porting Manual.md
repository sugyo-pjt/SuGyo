# **프로젝트 빌드 및 배포 가이드**

이 문서는 GitLab 저장소에서 소스 코드를 클론한 후, Jenkins와 Docker를 사용하여 애플리케이션을 빌드하고 Blue/Green 방식으로 배포하는 전체 과정을 안내합니다.

## **1. 개발 및 실행 환경**

프로젝트를 구성하는 주요 기술 스택과 버전 정보는 다음과 같습니다.

### **1.1. 기술 스택 (Technology Stack)**

| 구분                | 기술                                     | 위치 (Directory) | 설명                                    |
| ------------------- | ---------------------------------------- | ---------------- | --------------------------------------- |
| **백엔드 (Backend)**    | Java, Spring Boot 3.5.3, Gradle 8.14.3 | `/BE`              | 메인 비즈니스 로직 및 API 서버          |
| **AI 서버**         | Python, FastAPI                          | `/AI_server`     |  챗봇 API
| **데이터베이스**        | MySQL 8.0                                | `/MYSQL`         | 데이터 영속성 관리                      |
| **캐시/세션 저장소**  | Redis                                    | `docker-compose.yml` | JWT Refresh Token, 캐시 데이터 관리     |
| **리버스 프록시**     | Nginx                                    | `/Nginx`         | Blue/Green 배포, SSL 종료, 로드 밸런싱 |
| **CI/CD**           | Jenkins, Docker Compose                  | `/`              | 자동화된 빌드, 테스트, 배포 파이프라인 |

### **1.2. 개발 환경 (IDE)**

*   **IDE**: IntelliJ IDEA (`2025.1.3`), VS Code (`1.104.2`)
*   **JVM**: `bellsoft/liberica-openjdk-debian:21` (BE Dockerfile 기반)
*   **Python Interpreter**: `python:3.10-slim` (AI_server Dockerfile 기반)
*   **Python Library**: `mediapipe 0.10.21`, `fastapi 0.116.1`, `uvicorn 0.35.0`


## **2. 빌드 및 배포 프로세스 (CI/CD)**

빌드와 배포는 Jenkins 멀티 브랜치 파이프라인을 통해 자동화됩니다. 루트 디렉토리의 `Jenkinsfile`에 정의된 파이프라인은 Git 브랜치에 따라 다른 작업을 수행합니다.

### **2.1. 주요 단계 (Pipeline Stages)**

1.  **Analyze Changes**: Git 변경 사항을 감지하여 다음 중 재빌드/재시작이 필요한 대상을 결정합니다.
    *   **Spring Boot**: `/BE` 디렉토리 변경 시
    *   **FastAPI**: `/AI_server` 디렉토리 변경 시
    *   **인프라**: `/Nginx`, `/MYSQL`, `docker-compose.yml` 파일 변경 시
2.  **Update Green Environment (`develop` 브랜치)**: `develop` 브랜치에 푸시가 발생하면, Blue(운영) 환경에 영향을 주지 않고 Green(테스트) 환경만 최신 버전으로 업데이트합니다.
3.  **Deploy to Production (`master` 브랜치)**:
    *   **Prepare for Deployment**: Green 환경을 먼저 최신 버전으로 업데이트합니다.
    *   **Approval**: 운영 환경에 배포하기 전, Jenkins UI에서 수동 승인을 대기합니다.
    *   **Swap Blue-Green**: 승인 시, `/scripts/swap_blue_green.sh` 스크립트를 실행하여 Nginx 설정을 변경하고 트래픽을 Green 환경으로 전환합니다.
4.  **Post Actions**: 파이프라인 종료 후, 작업 공간(Workspace)을 정리합니다.

### **2.2. 빌드 시 사용되는 환경 변수**

Jenkins 파이프라인은 실행 시 `withCredentials` 플러그인을 통해 민감 정보를 안전하게 주입하고, 이 값들을 `.env` 파일로 생성하여 Docker Compose가 사용하도록 합니다.

| 환경 변수 이름 (in `.env`)          | Jenkins Credential ID     | 설명                                                       |
| ----------------------------------- | ------------------------- | ---------------------------------------------------------- |
| `MYSQL_ROOT_PASSWORD`               | `db-root-password`        | MySQL 데이터베이스의 root 계정 비밀번호                    |
| `MYSQL_DATABASE`                    | `db-name`                 | 애플리케이션이 사용할 데이터베이스(스키마) 이름            |
| `MYSQL_USER`                        | `db-user`                 | 애플리케이션이 DB 접속에 사용할 사용자 계정                |
| `MYSQL_PASSWORD`                    | `db-password`             | 위 사용자의 비밀번호                                       |
| `AWS_ACCESS_KEY_ID`                 | `aws-access-key-id`       | AWS 서비스 접근을 위한 Access Key ID                       |
| `AWS_SECRET_ACCESS_KEY`             | `aws-secret-access-key`   | AWS 서비스 접근을 위한 Secret Access Key                   |
| `AWS_REGION`                        | `aws-region`              | 사용할 AWS 리전 (예: `ap-northeast-2`)                     |
| `SPRING_CLOUD_AWS_S3_BUCKET`        | `aws-s3-bucket`           | 파일 업로드를 위한 S3 버킷 이름                            |
| `SPRING_CLOUD_AWS_S3_CDN_URL`       | `aws-s3-cdn-url`          | S3 버킷과 연결된 CDN(CloudFront 등) URL                    |
| `SPRING_JWT_SECRET`                 | `jwt-secret`              | JWT 서명에 사용되는 시크릿 키 (256비트 이상 권장)        |
| `GMS_KEY`               | `gms-key`        | GMS API를 사용하기 위한 API_Key
| `CHAT_LLM_URL`               | `chat-llm-url`        | GMS API 호출 URL
| `CHAT_MODEL`               | `chat-model`        | GMS API에서 사용할 모델명

`docker-compose.yml` 파일은 이 `.env` 파일을 참조하여 각 컨테이너에 필요한 환경 변수를 주입합니다.

## **3. 배포 시 특이사항**

*   **Blue/Green 배포**: Nginx를 리버스 프록시로 사용하여 무중단 배포를 구현합니다. `spring-app-blue` (포트 8081)와 `spring-app-green` (포트 8082) 두 개의 Spring Boot 컨테이너를 운영하며, Nginx가 트래픽을 둘 중 하나로 보냅니다.
*   **데이터베이스 스키마**: `/MYSQL/DUMP/1_DDL.sql` 파일에 테이블 생성 스키마(DDL)가 정의되어 있습니다. MySQL 컨테이너 빌드 시 이 스크립트가 실행되어 테이블 구조를 초기화합니다.
*   **초기 데이터**: `/MYSQL/DUMP/` 디렉토리의 `2_INSERT_VOCA.sql`, `3_INSERT_DAILY_VOCA.sql` 스크립트를 통해 어휘 및 학습 데이터가 사전에 주입됩니다.
*   **HTTPS 및 SSL**: Let's Encrypt를 사용하여 SSL 인증서를 발급받고, Nginx에서 80번 포트로 들어오는 모든 HTTP 요청을 443번 포트의 HTTPS로 리디렉션합니다.
*   **WebSocket 지원**: `/play/hard/` 경로에 대해서는 WebSocket 연결을 지원하기 위해 Nginx의 `Upgrade` 및 `Connection` 헤더가 설정되어 있습니다.

## **4. 주요 설정 파일 목록**

프로젝트의 핵심 설정 및 계정 정보는 다음 파일들에서 환경 변수를 통해 주입됩니다. **(실제 값은 Jenkins Credentials에 저장되어 있습니다.)**

| 파일 경로                                | 설명                                                                                                |
| ---------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `/.env.template`                         | 프로젝트 실행에 필요한 모든 환경 변수의 목록을 제공하는 템플릿 파일입니다.                             |
| `/BE/src/main/resources/application.yml` | **Spring Boot 애플리케이션의 핵심 설정 파일**입니다.<br/>DB, AWS S3, JWT 관련 설정을 환경 변수로 참조합니다. |
| `/AI_server/main.py` | **FastAPI 서버 핵심 설정 파일**입니다.<br/> prefix, API 미들웨어 등을 정의합니다.|
| `/docker-compose.yml`                    | 프로젝트 전체 인프라와 서비스 간의 관계를 정의합니다.<br/>`.env` 파일을 참조하여 민감 정보를 전달합니다. |
| `/Nginx/nginx.conf`                      | Nginx의 리버스 프록시, Blue/Green 라우팅, SSL 규칙을 정의합니다.                                        |
| `/MYSQL/cnf/my.cnf`                      | MySQL 서버의 세부 설정을 정의합니다.                                                                |
| `/Jenkinsfile`                           | CI/CD 파이프라인의 모든 단계를 정의하는 스크립트입니다.                                               |


***

## 프로젝트 폴더 구조

```
S13P21A602:.
├─.idea
├─.vscode
├─AI_server
│  └─app
│      ├─api
│      │  └─route
│      ├─core
│      ├─models
│      │  └─schemas
│      └─services
│         └─chat

├─BE
│  ├─.gradle
│  │  ├─8.14.3
│  │  │  ├─checksums
│  │  │  ├─executionHistory
│  │  │  ├─expanded
│  │  │  ├─fileChanges
│  │  │  ├─fileHashes
│  │  │  └─vcsMetadata
│  │  ├─buildOutputCleanup
│  │  └─vcs-1
│  ├─.idea
│  │  └─modules
│  ├─build
│  │  ├─classes
│  │  │  └─java
│  │  │      ├─main
│  │  │      │  └─com
│  │  │      │      └─sugyo
│  │  │      │          ├─auth
│  │  │      │          ├─common
│  │  │      │          ├─config
│  │  │      │          └─domain
│  │  │      │              ├─game
│  │  │      │              ├─music
│  │  │      │              ├─study
│  │  │      │              ├─term
│  │  │      │              └─user
│  │  │      └─test
│  │  ├─generated
│  │  ├─reports
│  │  ├─resources
│  │  ├─test-results
│  │  └─tmp
│  ├─gradle
│  │  └─wrapper
│  └─src
│      ├─main
│      │  ├─java
│      │  │  └─com
│      │  │      └─sugyo
│      │  │          ├─auth
│      │  │          ├─common
│      │  │          ├─config
│      │  │          └─domain
│      │  │              ├─game
│      │  │              ├─music
│      │  │              ├─study
│      │  │              ├─term
│      │  │              └─user
│      │  └─resources
│      └─test
│          ├─java
│          └─resources
├─jenkins
├─motion-recognization
├─MYSQL
│  ├─cnf
│  └─DUMP
├─Nginx
└─scripts
```

## 시연 시나리오
1. apk 파일 다운로드 후 어플 설치
2. 회원가입(아이디는 @email.com 형식으로 입력, 비밀번호도 가이드에 따라 입력)
3. 회원가입한 정보로 로그인
4. 학습 / 게임 / 챗봇 / 마이페이지에서 원하는 기능 선택
5. **하단의 네브바에서 원하는 기능 선택**

### 학습
1. 노래학습 / 로드맵 중 선택
2. 로드맵의 로드맵 시작하기 버튼 클릭
3. 원하는 Day 클릭
4. 영상의 플레이 버튼을 클릭하여 수어 학습
5. 영상 하단의 다음 혹은 학습 목록의 단어를 클릭하여 다른 단어로 넘어감
6. 단어 학습 종료 시 우측 상단의 퀴즈 모드 클릭
7. 나오는 영상을 클릭하여 시청 후 주어지는 4개의 단어 중 정답을 선택 후 제출하기 버튼 클릭
8. 정답/오답 결과가 나오면 다음 버튼을 눌러 문제를 넘어감
9. 퀴즈가 끝나면 퀴즈 다시하기 버튼을 눌러 재도전 하거나 로드맵으로 돌아가기 버튼을 클릭해 로드맵으로 이동

### 챗봇
1. 하단의 챗봇 버튼 클릭
2. 원하는 문장을 입력 후 우측의 초록색 버튼 클릭
3. AI의 답변 생성

### 게임
1. 하단의 게임 버튼 클릭
2. 상단의 곡 검색 혹은 제공되는 노래 클릭
3. Easy / Hard 중 원하는 난이도 선택 혹은 취소 버튼 클릭
4. 클릭 후 시작되면 카메라에 본인의 손과 얼굴이 나온 상태에서 수어로 게임 진행
5. 게임 종료 시 화면 하단의 노래학습 시작하기 / 다시하기 / 명예의 전당 / 목록으로 중 선택 가능
    - 명예의 전당 : 다른 사람들의 해당 노래의 점수 현황을 확인 가능
    - 다시하기 : 노래 게임 재시작
    - 노래학습 시작하기 : 학습과 같은 UI를 통해 해당 노래에 등장하는 단어들 학습 가능
    - 목록으로 : 2번으로 돌아감

### 검색
1. 화면 하단의 검색 클릭
2. 상단에 단어를 입력 후 검색
3. 원하는 단어 버튼을 클릭하여 단어 상세 확인

### 마이페이지
1. 화면 하단의 마이 클릭
2. 비밀번호 변경 버튼 클릭 시 비밀번호 변경 가능