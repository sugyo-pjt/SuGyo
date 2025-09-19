# TIL – Android UI 기초 구조 및 컴포넌트

## 1. 안드로이드 UI 구조

### 1-1. Activity

* 사용자와 상호작용하는 **단일 화면 단위의 컴포넌트**.
* 앱의 실행 단위를 구성하며, `onCreate()` 메서드를 통해 레이아웃을 설정한다.
* 기본적으로 UI를 포함하지 않기 때문에, 반드시 **View를 통해 화면 요소를 구성**해야 한다.

```kotlin
setContentView(R.layout.activity_main)
```

---

### 1-2. View

* 화면에 표시되는 **모든 UI 요소의 기본 클래스**.
* 버튼, 텍스트, 이미지 등은 모두 View의 하위 클래스이다.
* View는 이벤트를 수신하고, 사용자 입력에 반응할 수 있는 **인터랙티브 요소**이다.

---

### 1-3. ViewGroup

* **View를 포함하고 배치하는 컨테이너 역할**을 하는 클래스.
* View와 ViewGroup을 조합하여 UI 계층을 구성한다.

#### 주요 ViewGroup 종류

| ViewGroup          | 설명                                       |
| ------------------ | ---------------------------------------- |
| `LinearLayout`     | 자식 View들을 수직 혹은 수평 방향으로 나열               |
| `FrameLayout`      | 하나의 View만 보이도록 배치하며, 겹치는 구조를 구현 가능       |
| `GridLayout`       | 표 형식으로 행과 열에 맞춰 View 배치                  |
| `ConstraintLayout` | View 간의 제약조건을 기반으로 배치하는 현대적 레이아웃         |
| `Toolbar`          | 액티비티의 상단에 위치하여 제목, 아이콘, 메뉴 등을 표시         |
| `ViewPager2`       | 좌우로 슬라이딩 가능한 페이지 구성 가능 (RecyclerView 기반) |

> `RelativeLayout`은 ConstraintLayout으로 대체되며, 더 복잡한 UI를 효율적으로 표현할 수 있음.

---

## 2. 주요 View 클래스

| View 클래스      | 설명                                           |
| ------------- | -------------------------------------------- |
| `TextView`    | 텍스트를 화면에 표시                                  |
| `EditText`    | 사용자로부터 텍스트를 입력받는 필드                          |
| `Button`      | 클릭 가능한 UI 요소로, 이벤트를 처리함                      |
| `ImageView`   | 이미지 파일을 UI에 표시                               |
| `CheckBox`    | 두 상태(선택/미선택)를 가진 선택 버튼                       |
| `RadioButton` | 여러 옵션 중 하나만 선택 가능. 일반적으로 `RadioGroup`과 함께 사용 |

---

## 3. View의 공통 속성

* `layout_width`, `layout_height`: 크기 지정
* `padding`, `margin`: 내부/외부 여백 설정
* `visibility`: `visible`, `invisible`, `gone` 상태로 UI 요소 노출 여부 설정
* `background`, `textColor`, `src` 등 시각적 속성
* `id`: View를 코드에서 참조하기 위한 고유 식별자
* `focusable`, `clickable`: 사용자와의 상호작용 여부 결정

---

## 4. 이벤트(Event)와 이벤트 리스너(Event Listener)

### 4-1. 이벤트(Event)

* 사용자의 **입력(클릭, 터치, 키보드 입력 등)** 이 발생했을 때 시스템이 발생시키는 객체.

### 4-2. 이벤트 리스너

* View가 특정 이벤트를 감지했을 때 수행할 **동작(콜백 메서드)** 를 정의한 인터페이스.

#### 대표적인 이벤트 리스너 예시:

```kotlin
button.setOnClickListener {
    Toast.makeText(this, "클릭됨", Toast.LENGTH_SHORT).show()
}
```

| 리스너 종류                    | 설명                              |
| ------------------------- | ------------------------------- |
| `OnClickListener`         | 클릭 이벤트 처리                       |
| `OnTouchListener`         | 터치 동작 감지                        |
| `OnLongClickListener`     | 길게 누름 이벤트 처리                    |
| `TextWatcher`             | EditText 입력 변화 감지               |
| `OnCheckedChangeListener` | CheckBox, RadioButton의 상태 변경 감지 |

> 리스너는 **람다식**으로 간결하게 표현할 수 있으며, **ViewBinding** 혹은 **findViewById()** 방식으로 View를 참조해 사용.

---

## 5. AdapterView

### 5-1. AdapterView란?

* 다수의 데이터를 효율적으로 리스트 형식으로 표시하는 **ViewGroup의 하위 클래스**.
* 예: `ListView`, `GridView`, `Spinner` 등

### 5-2. Adapter 구조

* Adapter는 **데이터 → View로 변환**해주는 역할을 함.
* `getView()`를 통해 각 아이템에 대해 View를 생성하거나 재활용.

#### 구성 흐름

```kotlin
ListView → Adapter → 데이터(List 등)
```

#### 대표적인 Adapter 종류

| Adapter                | 설명                                      |
| ---------------------- | --------------------------------------- |
| `ArrayAdapter`         | 간단한 문자열 리스트 등 처리                        |
| `BaseAdapter`          | 커스터마이징 가능한 어댑터 (ViewHolder 패턴과 함께 사용)   |
| `RecyclerView.Adapter` | 성능과 유연성이 뛰어난 어댑터 뷰. 현대 안드로이드 앱의 기본 구성요소 |

> 화면 밖의 View는 생성하지 않고, **스크롤 시 재활용**하여 메모리와 성능을 최적화함.

---

## 핵심 정리

* **Activity는 UI의 틀**, View는 UI의 구성 요소.
* 모든 UI 요소는 View를 상속하며, ViewGroup은 View를 배치하는 컨테이너.
* 사용자와의 **상호작용은 이벤트와 리스너를 통해 처리**.
* **AdapterView는 대량의 데이터를 효율적으로 처리**하는 구조로, 어댑터를 통해 데이터와 뷰를 연결한다.
