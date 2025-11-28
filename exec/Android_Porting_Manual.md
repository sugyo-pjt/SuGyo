# 수어 학습 플랫폼 Android 앱 - 빌드 및 배포 가이드

## 개요
이 문서는 수어 학습 플랫폼 Android 애플리케이션의 GitLab 소스 클론 이후 빌드 및 배포를 위한 상세 가이드입니다.

---

## 1. 사용 기술 및 버전 정보

### 1.1 개발 환경 및 언어

- **Android API**: 최소 SDK 26 (Android 8.0), 타겟 SDK 36
- **Kotlin**: 2.0.21
- **Java 버전**: OpenJDK 11
- **Android Gradle Plugin**: 8.11.1
- **Gradle**: 8.14.3+ (Wrapper 사용)

### 1.2 주요 기술 스택

#### UI 프레임워크
- **Jetpack Compose**: 2024.09.00 (Compose BOM)
- **Material Design 3**: 1.3.0
- **Compose Navigation**: 2.8.0

#### 백엔드 통신
- **Retrofit**: 2.9.0 (REST API 통신)
- **OkHttp**: 4.12.0 (HTTP 클라이언트)
- **Gson & Moshi**: JSON 파싱 (AUTO 모드 지원)

#### 의존성 주입
- **Hilt**: 2.48 (Dagger 기반)
- **Hilt Navigation Compose**: 1.1.0

#### 미디어 및 카메라
- **Media3 (ExoPlayer)**: 1.8.0 (동영상 재생)
- **CameraX**: 1.4.0 (카메라 기능)
- **MediaPipe**: 0.10.11 (AI 비전 처리)

#### 기타 라이브러리
- **Coil**: 2.7.0 (이미지 로딩)
- **Kotlinx Serialization**: 1.7.3
- **JWT Decode**: 2.0.2 (토큰 디코딩)
- **Coroutines**: 1.8.1

---

## 2. 빌드 시 사용되는 환경 변수

### 2.1 네트워크 설정

```kotlin
// RetrofitClient.kt에 정의된 기본 API 서버
private const val BASE_URL = "https://j13a602.p.ssafy.io/"
```

### 2.2 네트워크 보안 설정

**network_security_config.xml**에 정의된 허용 도메인:
- `j13a602.p.ssafy.io` (API 서버 - HTTP/HTTPS 모두 허용)
- `korean.go.kr` (외부 사전 API - HTTP 허용)

### 2.3 앱 권한 설정

**AndroidManifest.xml**에 정의된 권한:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-feature android:name="android.hardware.camera.any" android:required="false" />
```

### 2.4 NDK 설정 (16KB 페이지 크기 지원)

```kotlin
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
}
```

---

## 3. 빌드 및 배포 명령어

### 3.1 사전 준비사항

#### 1. 개발 환경 설정
```bash
# Android SDK 경로 설정 (local.properties에 자동 생성됨)
sdk.dir=C:\\Users\\{사용자명}\\AppData\\Local\\Android\\Sdk

# JVM 메모리 설정 (gradle.properties)
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

#### 2. 필수 도구 설치
- **Android Studio**: Arctic Fox 이상 권장
- **Android SDK**: API Level 26-36
- **Android Build Tools**: 최신 버전
- **Android Emulator** 또는 **실제 디바이스**

### 3.2 프로젝트 클론 및 설정

```bash
# 1. 저장소 클론
git clone https://lab.ssafy.com/s13-webmobile1-sub1/S13P21A602.git
cd S13P21A602

# 2. 권한 설정 (Linux/Mac)
chmod +x gradlew

# 3. Gradle Wrapper 검증
./gradlew --version
```

### 3.3 빌드 명령어

#### 디버그 빌드
```bash
# Windows
.\gradlew assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

#### 릴리즈 빌드
```bash
# Windows
.\gradlew assembleRelease

# Linux/Mac
./gradlew assembleRelease
```

#### 클린 빌드
```bash
# Windows
.\gradlew clean assembleDebug

# Linux/Mac
./gradlew clean assembleDebug
```

### 3.4 설치 및 실행

#### 디바이스에 직접 설치
```bash
# 디버그 버전 설치
./gradlew installDebug

# 앱 실행 (adb 필요)
adb shell am start -n com.ssafy.a602/.MainActivity
```

#### APK 파일 위치
- **디버그**: `app/build/outputs/apk/debug/app-debug.apk`
- **릴리즈**: `app/build/outputs/apk/release/app-release.apk`

---

## 4. 배포 프로세스

### 4.1 디버그 배포 (개발용)

```bash
# 1. 의존성 다운로드 및 검증
./gradlew dependencies

# 2. 린트 검사
./gradlew lint

# 3. 단위 테스트 실행
./gradlew test

# 4. 디버그 빌드 및 설치
./gradlew clean assembleDebug installDebug
```

### 4.2 릴리즈 배포 (프로덕션)

```bash
# 1. 코드 검증
./gradlew clean lint test

# 2. 릴리즈 빌드
./gradlew assembleRelease

# 3. APK 서명 확인 (서명이 설정된 경우)
./gradlew signingReport
```

### 4.3 Play Console 배포

```bash
# AAB(Android App Bundle) 생성 (권장)
./gradlew bundleRelease

# 생성된 파일: app/build/outputs/bundle/release/app-release.aab
```

---

## 5. 배포 시 특이사항

### 5.1 MediaPipe 모델 파일

**assets/models/** 디렉토리에 포함된 AI 모델:
- `hand_landmarker.task` (손 동작 인식)
- `pose_landmarker_lite.task` (자세 인식)

**주의**: 이 파일들은 앱 크기에 영향을 미치므로 필요시 최적화 고려

### 5.2 게임 차트 JSON 파일

**assets/charts/** 디렉토리에 포함된 게임 데이터:
- `music_1.json` (리듬 게임 정답 데이터)
- **AnswerLoader**를 통해 로드되는 정답 타임라인
- **AnswerTimeline**으로 변환되어 게임 판정에 사용

**특징**:
- 대용량 JSON 파일 (약 9MB)
- 프레임별 수어 동작 좌표 데이터
- 게임 시작 시 메모리에 로드

### 5.3 16KB 페이지 크기 지원

Android 15+ 디바이스 호환성을 위한 설정:
```kotlin
packaging {
    jniLibs {
        useLegacyPackaging = false
    }
}
```

### 5.4 ProGuard/R8 최적화

**릴리즈 빌드 시 코드 난독화 및 최적화**:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = false  // 현재 비활성화
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 5.5 네트워크 보안

- **HTTPS 우선**: 프로덕션에서는 HTTPS 사용 권장
- **Certificate Pinning**: 보안 강화시 고려
- **API 키 보안**: 민감한 키는 네이티브 코드나 서버에서 관리

---

## 6. API 설정 및 주요 엔드포인트

### 6.1 기본 API 서버

```kotlin
BASE_URL = "https://j13a602.p.ssafy.io/"
```

### 6.2 주요 API 서비스

#### 인증 관련
- **AuthApiService**: 로그인, 회원가입, 토큰 관리
- **JWT 토큰 관리**: AccessToken + RefreshToken

#### 게임 관련
- **RhythmApi**: 리듬 게임 관련 API
- **음악 데이터**: 차트, 랭킹, 점수 전송
- **정답 데이터**: 로컬 JSON 파일과 서버 API 병행 사용

#### 학습 관련
- **StudyApiService**: 학습 진도, 퀴즈 데이터
- **용어 검색**: 수어 용어 및 사전 API

### 6.3 실시간 데이터 전송

**HTTP 기반 실시간 데이터 전송**:
- **WordWindowUploader**: 수어 타이밍 데이터 업로드
- **CoordinatesRecorder**: 좌표 데이터 수집 및 전송
- **OkHttp 기반**: REST API를 통한 실시간 점수 전송

---

## 7. 빌드 실행 순서

### 7.1 첫 번째 빌드 (초기 설정)

```bash
# 1. 프로젝트 클론
git clone https://lab.ssafy.com/s13-webmobile1-sub1/S13P21A602.git
cd S13P21A602

# 2. Gradle Wrapper 실행 권한 부여 (Linux/Mac)
chmod +x gradlew

# 3. 의존성 다운로드
./gradlew dependencies --refresh-dependencies

# 4. 프로젝트 동기화
./gradlew sync

# 5. 첫 번째 빌드
./gradlew clean assembleDebug
```

### 7.2 개발 중 빌드

```bash
# 빠른 빌드 (증분 빌드)
./gradlew assembleDebug

# 변경사항이 많을 때
./gradlew clean assembleDebug

# 실시간 설치 및 실행
./gradlew installDebug
```

### 7.3 배포 전 검증

```bash
# 1. 전체 테스트 실행
./gradlew clean test lint

# 2. 릴리즈 빌드 검증
./gradlew assembleRelease

# 3. APK 크기 확인
ls -la app/build/outputs/apk/release/

# 4. 설치 테스트
./gradlew installRelease
```

---

## 8. 트러블슈팅

### 8.1 일반적인 빌드 오류

#### Gradle 동기화 실패
```bash
# Gradle 캐시 정리
./gradlew clean --refresh-dependencies

# Gradle Wrapper 재다운로드
./gradlew wrapper --gradle-version 8.14.3
```

#### 의존성 충돌
```bash
# 의존성 트리 확인
./gradlew app:dependencies

# 특정 의존성 강제 버전 지정 (build.gradle.kts)
implementation("com.squareup.okhttp3:okhttp:4.12.0") {
    force = true
}
```

#### 메모리 부족 오류
```properties
# gradle.properties 수정
org.gradle.jvmargs=-Xmx4048m -XX:MaxMetaspaceSize=1024m
org.gradle.parallel=true
org.gradle.caching=true
```

### 8.2 실행 시 오류

#### 네트워크 연결 실패
1. **network_security_config.xml** 확인
2. **인터넷 권한** 확인
3. **API 서버 상태** 확인

#### 카메라 권한 오류
```kotlin
// 런타임 권한 요청 확인
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
    != PackageManager.PERMISSION_GRANTED) {
    // 권한 요청
}
```

#### MediaPipe 모델 로드 실패
1. **assets/models/** 폴더 확인
2. **모델 파일 무결성** 확인
3. **디바이스 호환성** 확인

#### JSON 차트 데이터 로드 실패
1. **assets/charts/** 폴더 확인
2. **music_1.json 파일 존재** 확인
3. **메모리 부족** 확인 (대용량 JSON 파일)

### 8.3 성능 최적화

#### APK 크기 최적화
```kotlin
// build.gradle.kts
android {
    bundle {
        abi { enableSplit = true }
        density { enableSplit = true }
        language { enableSplit = false }
    }
}
```

#### 메모리 사용량 최적화
```kotlin
// 이미지 로딩 최적화 (Coil)
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
)
```

#### JSON 파일 최적화
```kotlin
// AnswerLoader에서 지연 로딩 사용
private fun loadJsonFromAssets(context: Context, musicId: Long): String? {
    return try {
        val fileName = "charts/music_${musicId}.json"
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        Log.e(TAG, "assets 파일 로드 실패: charts/music_${musicId}.json", e)
        null
    }
}
```

---

## 9. 모니터링 및 디버깅

### 9.1 로그 확인

#### 빌드 로그
```bash
# 상세 빌드 로그
./gradlew assembleDebug --info --stacktrace

# 성능 분석
./gradlew assembleDebug --profile
```

#### 런타임 로그
```bash
# 앱 로그 필터링
adb logcat | grep "com.ssafy.a602"

# 특정 태그 로그
adb logcat -s "AuthManager"
adb logcat -s "AnswerLoader"
adb logcat -s "GamePlayViewModel"
```

### 9.2 네트워크 디버깅

#### HTTP 로그 활성화
```kotlin
// RetrofitClient.kt에서 이미 설정됨
private fun loggingInterceptor(): HttpLoggingInterceptor =
    HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }
```

#### Charles Proxy 연동
1. **네트워크 보안 설정** 수정
2. **프록시 인증서** 설치
3. **HTTPS 트래픽** 모니터링

---

## 10. 배포 체크리스트

### 10.1 배포 전 필수 확인사항

- [ ] **모든 테스트 통과** (`./gradlew test`)
- [ ] **린트 오류 없음** (`./gradlew lint`)
- [ ] **ProGuard 규칙 검증** (릴리즈 빌드)
- [ ] **API 엔드포인트 확인** (프로덕션 URL)
- [ ] **권한 설정 검토** (최소 권한 원칙)
- [ ] **앱 서명 설정** (Play Store 배포시)
- [ ] **JSON 차트 파일 확인** (assets/charts/)

### 10.2 성능 검증

- [ ] **앱 시작 시간** 측정
- [ ] **메모리 사용량** 프로파일링
- [ ] **네트워크 효율성** 검증
- [ ] **배터리 사용량** 최적화
- [ ] **JSON 파일 로딩 시간** 측정

### 10.3 호환성 검증

- [ ] **다양한 디바이스** 테스트
- [ ] **다양한 화면 크기** 검증
- [ ] **Android 버전** 호환성 (API 26-36)
- [ ] **16KB 페이지 크기** 지원

---

## 11. 프로젝트 특화 정보

### 11.1 앱 구조

```
com.ssafy.a602/
├── auth/          # 인증 관련 (로그인, 회원가입)
├── game/          # 리듬 게임 기능
├── learning/      # 학습 기능
├── search/        # 검색 기능
├── home/          # 홈 화면
├── mypage/        # 마이페이지
├── chatbot/       # 챗봇 기능
└── common/        # 공통 컴포넌트
```

### 11.2 주요 기능

- **수어 학습**: MediaPipe 기반 손동작 인식
- **리듬 게임**: ExoPlayer 기반 음악 재생
- **실시간 채점**: HTTP 기반 실시간 데이터 전송
- **진도 관리**: REST API 연동
- **소셜 기능**: 랭킹, 커뮤니티

### 11.3 데이터 흐름

1. **사용자 인증**: JWT 토큰 기반
2. **게임 데이터**: Retrofit으로 REST API 호출
3. **정답 데이터**: 로컬 JSON 파일 + 서버 API 병행
4. **실시간 데이터**: HTTP 기반 실시간 전송
5. **미디어 재생**: ExoPlayer + MediaPipe
6. **로컬 캐싱**: Hilt + Repository 패턴

---

## 12. 추가 개선사항

### 12.1 발견된 개선점

1. **API 서버 통일**: 모든 API가 `https://j13a602.p.ssafy.io/` 사용
2. **MediaPipe 최적화**: Vision 모듈만 사용하여 16KB 페이지 크기 호환성 확보
3. **ExoPlayer 설정**: HTTP 리다이렉트 및 타임아웃 설정 최적화
4. **JSON 차트 데이터**: 대용량 파일의 메모리 최적화 필요

### 12.2 권장사항

1. **ProGuard 활성화**: 릴리즈 빌드에서 코드 난독화 적용 고려
2. **APK 최적화**: Bundle 분할을 통한 다운로드 크기 최적화
3. **보안 강화**: Certificate Pinning 및 API 키 보안 강화
4. **성능 모니터링**: Firebase Performance 또는 유사 도구 도입
5. **JSON 파일 최적화**: 압축 또는 스트리밍 로딩 고려

---

이 문서를 참고하여 수어 학습 플랫폼 Android 앱을 성공적으로 빌드하고 배포할 수 있습니다.

**문의사항**: 빌드 관련 문제 발생시 프로젝트 이슈 트래커나 개발팀에 문의하세요.