# TIL – Kotlin 기본 문법 정리 (for Android)

## 1. Kotlin의 기본 개념

* Kotlin은 **JVM 기반 언어**로, Android 개발의 공식 언어로 채택됨.
* **정적 타입 언어**이며, **타입 추론**, **null-safety**, **람다**, **함수형 프로그래밍** 지원.
* Python과 달리 변수의 **자료형이 명시되거나 컴파일러에 의해 추론**되어야 하며, **null 허용 여부도 명시적**이다.

---

## 2. 변수와 자료형

### 변수 선언

```kotlin
val name: String = "은수"   // 불변 변수 (const 아님, 초기값 기준으로 고정)
var age: Int = 25          // 가변 변수
```

* `val` = value, 변경 불가 (immutable)
* `var` = variable, 변경 가능 (mutable)

### 자료형 (참조형으로 동작하지만 내부적으로 primitive 최적화)

| 타입                     | 예시           | 설명          |
| ---------------------- | ------------ | ----------- |
| Byte, Short, Int, Long | 1, 10L       | 정수형         |
| Float, Double          | 3.14f, 2.7   | 실수형         |
| Boolean                | true / false | 논리형         |
| Char                   | 'A'          | 문자형 (작은따옴표) |
| String                 | "Hello"      | 문자열 (큰따옴표)  |

### 변수명 규칙

* `numberOfBooks`: 일반 변수, 함수 등은 **카멜 케이스**
* `MainActivity`: 클래스나 인터페이스는 **파스칼 케이스**

---

## 3. 문자열 다루기

```kotlin
val name = "은수"
val age = 20
println("이름: $name, 나이: ${age + 1}")
```

* 문자열 템플릿 지원: `$변수`, `${표현식}`
* 비교 시 `==`는 값 비교, `===`는 참조(주소) 비교

---

## 4. 자료형 변환

Kotlin은 \*\*묵시적 형변환(자동 캐스팅)\*\*을 허용하지 않음 → 명시적으로 변환해야 함

```kotlin
val x: Int = 5
val y: Long = x.toLong() // Int -> Long
val z: Double = y.toDouble()
```

> Python에서는 `int + float`이 자동으로 float으로 변환되지만, Kotlin은 **toFloat()**, **toInt()** 등 사용 필요

---

## 5. 스마트 캐스트와 타입 검사

```kotlin
fun getLength(obj: Any): Int? {
    if (obj is String) {
        // obj는 자동으로 String으로 스마트 캐스팅됨
        return obj.length
    }
    return null
}
```

* `is`는 Python의 `isinstance()`와 비슷
* `is`가 true이면 명시적인 캐스팅 없이 내부에서 형 변환됨 (스마트 캐스트)

---

## 6. Null 안전성과 처리 방식

### 기본적으로 null을 허용하지 않음

```kotlin
var a: String = "hello"
a = null // 오류 발생
```

### nullable 변수 선언

```kotlin
var b: String? = null
```

### 안전한 접근 방법

```kotlin
val len = b?.length         // null이면 null 반환
val len2 = b?.length ?: 0   // null이면 0 반환 (엘비스 연산자)
val len3 = b!!.length        // null 아님을 보장 (null일 경우 런타임 에러)
```

---

## 7. 조건문과 반복문

### if 표현식

```kotlin
val max = if (a > b) a else b
```

* Kotlin에서는 **if도 값**을 반환하는 표현식

### when (Python의 match/case와 유사)

```kotlin
val result = when (score) {
    in 90..100 -> "A"
    in 80..89 -> "B"
    else -> "F"
}
```

### for 문

```kotlin
for (i in 0..5) { }            // 0부터 5까지
for (i in 10 downTo 1 step 2) // 10, 8, 6, ..., 2
for (i in 0 until 5) { }       // 0부터 4까지
```

### while & do-while

```kotlin
while (조건) { }
do { } while (조건)
```

### break, continue, label

```kotlin
outer@ for (i in 1..5) {
    for (j in 1..5) {
        if (j == 3) break@outer
    }
}
```

---

## 8. 함수 정의 및 람다 표현식

### 일반 함수

```kotlin
fun add(a: Int, b: Int): Int {
    return a + b
}
```

### 축약형 (단일 표현식)

```kotlin
fun add(a: Int, b: Int) = a + b
```

### 람다식

```kotlin
val sum = { x: Int, y: Int -> x + y }
val greet: (String) -> String = { name -> "Hello, $name" }
```

* 람다는 변수에 저장하거나 다른 함수에 넘길 수 있는 **일급 객체**
* 마지막 줄의 결과가 반환값
* 매개변수가 1개일 경우, `it` 키워드로 생략 가능

---

## 9. 배열과 컬렉션

```kotlin
val numbers = arrayOf(1, 2, 3)
val list = listOf("a", "b", "c")       // 불변 리스트
val mutable = mutableListOf("x", "y")  // 가변 리스트
```

* listOf → 변경 불가 (immutable)
* mutableListOf → 추가, 삭제 가능

---

## 요약 정리

| 주제       | 요점                                  |
| -------- | ----------------------------------- |
| 변수       | `val` (불변), `var` (가변)              |
| 자료형      | 정적 타입 + 명시적 변환 필요                   |
| Null 안전성 | `?`, `!!`, `?.`, `?:` 사용            |
| 조건/반복    | if/when/for/while 지원, 삼항 연산자 없음     |
| 함수형 지원   | 람다, 고차함수, 컬렉션 API                   |
| 스마트 캐스트  | `is` 검사 후 자동 캐스팅                    |
| 자료구조     | listOf, mutableListOf, arrayOf 등 다양 |

---

필요하면 이 내용을 기반으로 Android에서 자주 쓰이는 코틀린 문법만 따로 정리해줄 수도 있어.
또, Python과 Kotlin 문법 비교표가 필요하면 그것도 만들어줄게.
계속 이어서 정리할 주제가 있다면 알려줘!
