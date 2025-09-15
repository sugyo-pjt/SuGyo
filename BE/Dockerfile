FROM bellsoft/liberica-openjdk-debian:21 AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper 스크립트 복사
COPY gradlew .
COPY gradle ./gradle

# build.gradle 파일 복사 (의존성 분석을 위함)
COPY build.gradle .
COPY settings.gradle .

# 먼저 의존성만 다운로드하여 별도의 레이어에 캐싱합니다.
# build.gradle 파일이 변경되지 않으면 이 단계는 캐시를 사용해 즉시 완료됩니다.
RUN ./gradlew dependencies

# 전체 소스 코드 복사
COPY src ./src

# 애플리케이션을 빌드하여 jar 파일을 생성합니다.
# 이 때, build.gradle에 설정된 'bootJar'가 계층형 JAR를 생성합니다.
RUN ./gradlew bootJar

# =================
# 2. 최종 이미지 스테이지 (Final Stage)
# =================
# 실제 실행 환경은 JRE(Java Runtime Environment)만 포함된 경량 이미지로 구성합니다.
FROM bellsoft/liberica-runtime-container:jre-21-slim-glibc

WORKDIR /app

# 빌드 스테이지에서 생성된 계층형 JAR의 각 레이어를 순서대로 복사합니다.
# 의존성 -> 스프링 부트 로더 -> 애플리케이션 순으로 복사하여 변경이 잦은 'application' 레이어만 새로 빌드되도록 합니다.
COPY --from=builder /app/build/libs/*.jar app.jar



# 컨테이너 실행 명령어
# java -jar app.jar 대신 JarLauncher를 직접 실행하여 부팅 속도를 약간 개선합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]
