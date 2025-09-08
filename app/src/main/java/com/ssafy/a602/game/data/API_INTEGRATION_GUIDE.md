# API 연동 가이드

## 개요
현재 게임 시스템은 더미 데이터를 사용하고 있으며, 실제 API 연동을 위해 최적화된 구조로 설계되었습니다.

## 주요 변경사항

### 1. 중앙화된 데이터 관리
- `GameDataManager`: 모든 게임 데이터를 중앙에서 관리
- `GameApiService`: API 호출을 위한 인터페이스 정의
- `DummyGameApiService`: 현재 사용 중인 더미 구현체

### 2. 화면 간 데이터 공유
- 모든 화면이 `GameDataManager`를 통해 동일한 데이터 사용
- 곡 선택, 게임 진행 상태 등이 자동으로 동기화

### 3. API 연동 준비
- 비동기 처리 지원 (suspend 함수)
- 에러 처리 구조 준비
- 로딩 상태 관리

## API 연동 방법

### 1. 실제 API 서비스 구현
```kotlin
class RealGameApiService : GameApiService {
    private val apiClient = RetrofitClient.create()
    
    override suspend fun getSongs(): List<Song> {
        return apiClient.getSongs().map { it.toSong() }
    }
    
    override suspend fun searchSongs(query: String): List<Song> {
        return apiClient.searchSongs(query).map { it.toSong() }
    }
    
    override suspend fun getSongSections(songId: String): List<SongSection> {
        return apiClient.getSongSections(songId).map { it.toSongSection() }
    }
    
    override suspend fun saveGameResult(songId: String, score: Int, accuracy: Float) {
        apiClient.saveGameResult(GameResultRequest(songId, score, accuracy))
    }
    
    override suspend fun getUserBestScore(songId: String): Int? {
        return apiClient.getUserBestScore(songId).score
    }
}
```

### 2. GameDataManager 수정
```kotlin
object GameDataManager {
    // 더미 서비스를 실제 서비스로 교체
    private val apiService: GameApiService = RealGameApiService()
    
    // 나머지 코드는 동일
}
```

### 3. 필요한 API 엔드포인트
- `GET /songs` - 곡 목록 조회
- `GET /songs/search?q={query}` - 곡 검색
- `GET /songs/{songId}/sections` - 곡의 소절 정보 조회
- `POST /game/result` - 게임 결과 저장
- `GET /user/best-score/{songId}` - 사용자 최고 점수 조회

## 데이터 구조

### Song
```kotlin
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationText: String,
    val bpm: Int,
    val rating: Double,
    val bestScore: Int?,
    val thumbnailRes: Int?
)
```

### SongSection
```kotlin
data class SongSection(
    val startTime: Float,    // 소절 시작 시간 (초)
    val duration: Float,     // 소절 길이 (초)
    val lyrics: String,      // 해당 소절의 가사
    val highlightRange: IntRange? // 하이라이트할 단어 범위
)
```

## 현재 더미 데이터
- `FakeSongs.items`: 10개의 더미 곡 데이터
- 각 곡별로 소절 정보가 하드코딩되어 있음
- 실제 API 연동 시 이 부분만 교체하면 됨

## 주의사항
1. 모든 API 호출은 suspend 함수로 구현되어 있음
2. 에러 처리는 각 ViewModel에서 try-catch로 처리
3. 로딩 상태는 UI에서 자동으로 표시됨
4. 게임 진행 상태는 실시간으로 업데이트됨

## 테스트
- 현재 더미 데이터로 모든 기능이 정상 작동함
- API 연동 후에도 동일한 UI/UX 유지
- Preview에서도 더미 데이터 사용 가능
