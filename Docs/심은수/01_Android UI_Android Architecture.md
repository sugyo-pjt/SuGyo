# TIL – Android UI 및 Architecture 기초 정리

## 1. 안드로이드란?

* Google이 주도하고 Open Handset Alliance(OHA)가 개발한 **오픈소스 모바일 운영체제(OS)**.
* 리눅스 커널을 기반으로 하며, 다양한 디바이스(스마트폰, 태블릿, 웨어러블, TV 등)에서 동작.
* 앱은 주로 **Kotlin** 또는 **Java** 언어로 작성되며, Android Studio를 이용해 개발.

### Android vs iOS

| 항목            | Android         | iOS                        |
| ------------- | --------------- | -------------------------- |
| OS            | 오픈소스(Linux 기반)  | 폐쇄형(UNIX 기반)               |
| App Store     | Google Play     | Apple App Store            |
| 언어            | Kotlin, Java    | Swift, Objective-C         |
| Design System | Material Design | Human Interface Guidelines |

* **디자인 시스템**이 서로 다르기 때문에 같은 기능의 앱이라도 플랫폼에 따라 UI/UX는 달라야 함.

---

## 2. Android 플랫폼 구조

Android는 다음과 같은 **4계층**으로 구성되어 있다:

1. **Linux Kernel**: 하드웨어와 직접 연결되는 계층. 메모리 관리, 디바이스 드라이버, 보안 등의 기능 제공.
2. **Native Libraries & Android Runtime (ART)**:

   * SQLite, OpenGL, WebKit 등 네이티브 C/C++ 라이브러리 포함.
   * Android 앱은 ART(Android Runtime) 위에서 동작하며, Java 바이트코드를 DEX로 변환하여 실행.
3. **Application Framework**:

   * 앱 개발에 필요한 다양한 서비스와 API 제공 (Activity Manager, Notification Manager 등).
   * 앱은 이 계층의 API를 사용해 기능을 구현.
4. **Applications**:

   * 사용자가 직접 사용하는 앱들 (SMS, 전화, 카메라, 타사 앱 등).

> 참고: [Android 개발자 문서](https://developer.android.com)

---

## 3. 앱 구성의 기본 단위 – App Components

### 앱 컴포넌트란?

* 안드로이드 앱은 **여러 개의 컴포넌트**로 구성되며, 각 컴포넌트는 **독립적인 실행 단위**이다.
* Java의 `main()` 함수와 같은 진입점이 없고, **시스템이 컴포넌트를 실행**하고 생명주기를 관리.

### 주요 컴포넌트 종류

| 컴포넌트                  | 설명                                                  |
| --------------------- | --------------------------------------------------- |
| **Activity**          | 하나의 화면(UI)을 담당. 사용자와 상호작용하는 주요 컴포넌트.                |
| **Service**           | UI 없이 백그라운드 작업 수행. 음악 재생, GPS, 알림 등.                |
| **BroadcastReceiver** | 시스템 또는 앱에서 발생한 **이벤트** 수신 및 처리. 부팅 완료, 배터리 상태 변화 등. |
| **ContentProvider**   | 앱 간 데이터 공유를 위해 사용. 예: 연락처 앱의 데이터 다른 앱에서 사용 가능.      |

### Intent

* 컴포넌트 간 연결은 `Intent` 객체를 통해 이루어짐.
* 명시적(Explicit) 인텐트: 특정 컴포넌트를 지정하여 실행.
* 암시적(Implicit) 인텐트: 수행할 작업의 타입을 지정, 시스템이 적절한 컴포넌트를 찾아 실행.

---

## 4. 앱 폴더 및 파일 구조

### 프로젝트 구조 핵심

* **AndroidManifest.xml**

  * 앱의 모든 컴포넌트를 정의하는 필수 파일.
  * 권한(permission), 앱 이름, 아이콘, 테마, 진입 액티비티 등을 설정.
  * `<application>` 태그 안에 앱의 주요 정보 포함.

* **Java/Kotlin 소스 디렉터리**

  * `MainActivity.kt` 또는 `MainActivity.java`: 앱 시작 시 실행되는 메인 액티비티 클래스.

* **`res` 폴더 (리소스 폴더)**

  * 앱에서 사용하는 리소스를 저장. XML 파일, 이미지, 문자열 등 포함.

    * `drawable/`: 이미지, 벡터, 배경 등 시각적 요소.
    * `layout/`: 화면 배치를 정의한 XML 파일.
    * `values/`: 문자열(`strings.xml`), 색상(`colors.xml`), 스타일(`styles.xml`) 등 상수값 저장.
    * `mipmap/`: 앱 아이콘 이미지 저장.

---

## 5. 생명주기(Lifecycle) – Activity 생명주기 예시

* Activity는 시스템에 의해 다양한 상태로 전이됨.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) { ... }   // 최초 생성 시
override fun onStart() { ... }                               // 사용자에게 보이기 시작
override fun onResume() { ... }                              // 사용자와 상호작용 가능
override fun onPause() { ... }                               // 포커스를 잃음 (예: 전화 수신)
override fun onStop() { ... }                                // 화면에서 완전히 사라짐
override fun onDestroy() { ... }                             // 소멸
```

> 생명주기를 적절히 관리하지 않으면 메모리 누수, 비정상 종료, 성능 저하 문제가 발생할 수 있음.

---

## 핵심 정리

* 안드로이드는 리눅스 기반 오픈소스 플랫폼이며 Kotlin이 주요 언어로 사용됨.
* 앱은 **컴포넌트 중심 구조**로 구성되며, Activity/Service/BroadcastReceiver/ContentProvider로 나뉨.
* `Intent`로 컴포넌트 간 통신이 이루어지며, `Manifest` 파일은 앱의 전반적 정의를 포함.
* Android Studio의 프로젝트 구조는 파일별 책임이 명확하게 나뉘어 있음.
* 생명주기 메서드를 이해하고 적절하게 활용하는 것이 안정적인 앱 개발의 핵심이다.

---