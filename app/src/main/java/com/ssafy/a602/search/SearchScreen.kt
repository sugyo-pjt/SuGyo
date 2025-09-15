/**
 * 이 파일은 "검색" 기능 전체(목록 + 자동완성 + 상세)를 한 파일에 모아둔 구현입니다.
 *
 * 주요 포인트
 *  - 1초 디바운스 자동완성: 사용자가 타이핑을 멈춘 뒤 1초 후에 마지막 입력값으로 자동완성 API 호출
 *  - 검색 버튼(IME Search)으로 확정 검색
 *  - Retrofit(Gson) + Repository + ViewModel + Compose UI
 *  - Material3 컴포넌트 사용. TextField/TopAppBar가 현재 버전에서 Experimental 표시가 있어 파일 레벨 Opt-in
 *
 *
 *
 *  5) 실제 요청 예시(백엔드와 테스트할 때)
 * 브라우저/포스트맨/curl로 바로 확인 가능:
 * # 자동완성
 * curl "http://i13a106.p.ssafy.io:8000/api/words/autocomplete?q=안"
 * # 검색
 * curl "http://i13a106.p.ssafy.io:8000/api/words/search?q=안녕하세요"
 * # 상세
 * curl "http://i13a106.p.ssafy.io:8000/api/words/101"
 *
 *
 *자동완성 /api/words/autocomplete?q=안
 * [
 *   { "id": 101, "text": "안녕하세요" },
 *   { "id": 102, "text": "안녕히 가세요" }
 * ]
 *
 * 검색 /api/words/search?q=안녕하세요
 * [
 *   { "id": 101, "title": "안녕하세요" },
 *   { "id": 205, "title": "안녕히 계세요" }
 * ]
 *
 * 상세 /api/words/101
 * {
 *   "id": 101,
 *   "title": "안녕하세요",
 *   "videoUrl": "https://cdn.example.com/videos/hello.mp4",
 *   "description": "처음 만났을 때 인사로 사용합니다..."
 * }
 *
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ssafy.a602.search

// ───────────────────────────────────────────────────────────────────
// Imports
// ───────────────────────────────────────────────────────────────────

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// 아이콘들 (Search, ArrowBack, ArrowForward, Play)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search

// Material3 컴포넌트들
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

// ViewModel/Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

// Flow/코루틴
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// 네트워킹 (Retrofit + Gson)
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.converter.gson.GsonConverterFactory

// 키보드 액션/옵션 (IME Search)
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

// 개별 명시 임포트(가독용)
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton



// ───────────────────────────────────────────────────────────────────
// ❶ 데이터 모델
// ───────────────────────────────────────────────────────────────────
// 서버 응답(JSON)을 파싱할 그릇들. 프로퍼티 이름은 서버 JSON 키와 일치해야 함.
data class WordSuggestion(val id: Long, val text: String)      // 자동완성 항목
data class WordSummary(val id: Long, val title: String)        // 검색 결과 요약 카드
data class WordDetail(val id: Long, val title: String, val videoUrl: String?, val description: String) // 상세

// ───────────────────────────────────────────────────────────────────
// ❷ Retrofit API 인터페이스 (엔드포인트는 서버 스펙에 맞게)
// ───────────────────────────────────────────────────────────────────
interface SearchApi {
    // 자동완성: /api/words/autocomplete?q=입력중_문자열
    @GET("/api/words/autocomplete")
    suspend fun autocomplete(@Query("q") prefix: String): List<WordSuggestion>

    // 확정 검색: /api/words/search?q=검색어
    @GET("/api/words/search")
    suspend fun search(@Query("q") query: String): List<WordSummary>

    // 상세: /api/words/{id}
    @GET("/api/words/{id}")
    suspend fun detail(@Path("id") id: Long): WordDetail
}

// ───────────────────────────────────────────────────────────────────
// ❸ Retrofit Provider (싱글톤)  ─ Gson 사용 / baseUrl 마지막은 반드시 '/'
// ───────────────────────────────────────────────────────────────────
private object ApiProvider {
    val api: SearchApi by lazy {
        Retrofit.Builder()
            // ⚠️ http 사용이므로 AndroidManifest.xml에 android:usesCleartextTraffic="true" 필요
            //    가능하면 https 로 바꾸는 것을 추천
            .baseUrl("http://i13a106.p.ssafy.io:8000/") // ← 끝에 '/' 중요!
            .addConverterFactory(GsonConverterFactory.create()) // moshi 대신 gson
            .build()
            .create(SearchApi::class.java)
    }
}

// ───────────────────────────────────────────────────────────────────
// ❹ Repository (API 호출을 한곳에서 관리; 뷰모델과 네트워크 사이의 얇은 레이어)
// ───────────────────────────────────────────────────────────────────
private class SearchRepository(private val api: SearchApi) {
    suspend fun autocomplete(prefix: String) = api.autocomplete(prefix)
    suspend fun search(query: String)        = api.search(query)
    suspend fun detail(id: Long)             = api.detail(id)
}

// ───────────────────────────────────────────────────────────────────
// ❺ ViewModel 상태 + 로직 (1초 디바운스 자동완성 + 확정 검색)
// ───────────────────────────────────────────────────────────────────

// 화면 상태를 한 객체로 관리 (Compose에서 관찰)
data class SearchUiState(
    val query: String = "",                                // 현재 입력 값
    val suggestions: List<WordSuggestion> = emptyList(),   // 자동완성 목록
    val results: List<WordSummary> = emptyList(),          // 확정 검색 결과 목록
    val isLoading: Boolean = false,                        // 로딩 스피너 표시 여부
    val error: String? = null,                             // 에러 메시지 (null이면 에러 없음)
    val hasSearched: Boolean = false                       // 한번이라도 "확정 검색"을 누른 상태인지
)

@OptIn(FlowPreview::class) // debounce/flatMapLatest 조합에서 필요한 표시
private class SearchViewModel(
    private val repo: SearchRepository
) : ViewModel() {

    // 사용자의 입력을 Flow로 받음. TextField onValueChange에서 값을 밀어 넣는다.
    private val queryFlow = MutableStateFlow("")

    // UI 상태(State)를 Flow로 보관
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow() // 외부에 read-only로 공개

    init {
        // ─ 자동완성 파이프라인 ─
        // 1) 사용자가 타이핑하면 queryFlow에 값이 들어옴
        // 2) 1초(debounce 1000ms) 동안 더 입력 없을 때 마지막 값만 통과
        // 3) 앞뒤 공백 제거 + 이전 값과 동일하면 무시(distinctUntilChanged)
        // 4) flatMapLatest: 이전 네트워크 요청이 진행 중이어도 새 입력이 오면 이전 요청은 취소되고 최신 요청만 유지
        viewModelScope.launch {
            queryFlow
                .debounce(1000)              // 1초 디바운스
                .map { it.trim() }           // 공백 제거
                .distinctUntilChanged()      // 같은 내용이면 다시 호출하지 않음
                .flatMapLatest { prefix ->   // 최신 입력만 유지
                    flow {
                        if (prefix.isEmpty()) {
                            // 입력이 비면 자동완성 목록도 비워준다
                            emit(Result.success(emptyList()))
                        } else {
                            // 네트워크 예외를 Result로 감싸서 위로 전달
                            try   { emit(Result.success(repo.autocomplete(prefix))) }
                            catch (t: Throwable) { emit(Result.failure(t)) }
                        }
                    }
                }
                .collect { result ->
                    // UI 상태 갱신 (자동완성 목록 또는 에러)
                    result
                        .onSuccess { list -> _state.update { it.copy(suggestions = list, error = null) } }
                        .onFailure { e    -> _state.update { it.copy(suggestions = emptyList(), error = e.message) } }
                }
        }
    }

    // TextField 입력 이벤트 처리
    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery, error = null) } // 화면의 query 반영 + 에러 제거
        queryFlow.value = newQuery                                // 자동완성 파이프라인에 새 값 푸시
    }

    // IME Search(돋보기)나 검색 버튼 눌렀을 때 호출
    fun onSubmitSearch() {
        val q = state.value.query.trim()
        if (q.isEmpty()) {
            // 빈 검색어로는 결과 없음. "검색한 적 있음"만 표시해서 Empty UI를 보여주자.
            _state.update { it.copy(results = emptyList(), hasSearched = true) }
            return
        }

        // 네트워크 호출은 코루틴에서 수행
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val list = repo.search(q)
                _state.update { it.copy(results = list, hasSearched = true, isLoading = false) }
            } catch (t: Throwable) {
                _state.update { it.copy(isLoading = false, error = t.message) }
            }
        }
    }
}

// ViewModel 생성 팩토리 (한 파일 안에서만 쓰는 간단 DI)
private class SearchVmFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SearchRepository(ApiProvider.api)
        return SearchViewModel(repo) as T
    }
}

// ───────────────────────────────────────────────────────────────────
// ❻ 검색 화면 UI
// ───────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    onOpenDetail: (Long) -> Unit, // 카드 클릭 시 상세로 이동하는 콜백 (NavGraph에서 전달)
) {
    // Composable 내부에서 ViewModel을 생성 (기본값 파라미터에서 viewModel() 호출 금지)
    val vm: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = SearchVmFactory()
    )
    // state StateFlow를 Compose 상태로 구독
    val state by vm.state.collectAsState()

    // 레이아웃 루트
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // 화면 제목
        Text(
            "단어 검색",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(Modifier.height(12.dp))

        // 검색 입력창 (IME Search를 누르면 vm.onSubmitSearch())
        SearchTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            onSearch = vm::onSubmitSearch
        )

        Spacer(Modifier.height(16.dp))

        // 상태에 따라 다른 UI를 보여줌
        when {
            // 아직 아무것도 안쳤거나, 치기만 하고 확정검색은 안한 상태
            state.query.isBlank() && !state.hasSearched -> EmptyState()

            // 네트워크 로딩 중
            state.isLoading -> LoadingState()

            // 에러가 났을 때
            state.error != null -> ErrorState(state.error!!)

            // 입력은 있고 확정검색은 안한 상태 → 자동완성 목록 보여주기
            state.query.isNotBlank() && !state.hasSearched -> {
                SuggestionList(items = state.suggestions, onClick = onOpenDetail)
            }

            // 확정검색 결과
            else -> {
                ResultList(items = state.results, onClick = onOpenDetail)
            }
        }
    }
}

// 검색 입력창
@Composable
private fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, // 돋보기 아이콘
        placeholder = { Text("단어를 검색하세요") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),         // 키보드에 Search 버튼 노출
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),            // Search 누르면 콜백
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    )
}

// 초기 빈 화면(힌트)
@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFFBFC6D1),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "단어를 검색해보세요",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "궁금한 단어를 입력하면 해당 단어의 수어 표현을 확인할 수 있어요.",
                color = Color.Gray
            )
        }
    }
}

// 로딩 스피너
@Composable private fun LoadingState() {
    Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
}

// 에러 메시지
@Composable private fun ErrorState(msg: String) {
    Box(Modifier.fillMaxSize()) {
        Text(
            "오류가 발생했습니다: $msg",
            color = Color.Red,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

// 자동완성 목록
@Composable
private fun SuggestionList(
    items: List<WordSuggestion>,
    onClick: (Long) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { s ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(s.id) },          // 카드 클릭 시 상세로 이동
                shape = MaterialTheme.shapes.large
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        s.text,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("수어 영상 보기", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// 확정 검색 결과 목록
@Composable
private fun ResultList(
    items: List<WordSummary>,
    onClick: (Long) -> Unit
) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize()) { Text("검색 결과가 없습니다.", modifier = Modifier.align(Alignment.Center)) }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { r ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(r.id) },  // 카드 클릭 시 상세로 이동
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            r.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────
// ❼ 상세 화면 (간단 버전; 영상은 videoUrl로 ExoPlayer를 붙이면 됨)
// ───────────────────────────────────────────────────────────────────

// 상세 화면 전용 ViewModel
private class WordDetailViewModel(
    private val repo: SearchRepository,
    private val wordId: Long
) : ViewModel() {
    var ui     by mutableStateOf<WordDetail?>(null); private set
    var loading by mutableStateOf(true);             private set
    var error   by mutableStateOf<String?>(null);    private set

    init {
        viewModelScope.launch {
            loading = true
            try { ui = repo.detail(wordId) } // 상세 API 호출
            catch (t: Throwable) { error = t.message }
            loading = false
        }
    }
}

// 상세 화면용 ViewModel 팩토리
private class WordDetailVmFactory(private val id: Long) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = SearchRepository(ApiProvider.api)
        return WordDetailViewModel(repo, id) as T
    }
}

// 상세 화면 UI
@Composable
fun WordDetailScreen(wordId: Long, onBack: () -> Unit) {
    // NavGraph에서 넘어온 wordId로 ViewModel 생성
    val vm: WordDetailViewModel = viewModel(factory = WordDetailVmFactory(wordId))

    Scaffold(
        topBar = {
            // 최신 material3에서 TopAppBar는 Experimental 표기가 남아있어
            // 파일 상단에 @file:OptIn(ExperimentalMaterial3Api::class) 추가해두었음.
            TopAppBar(
                title = { Text("단어 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "뒤로"
                        )
                    }
                }
            )
        }
    ) { inner ->
        when {
            vm.loading -> Box(Modifier.fillMaxSize().padding(inner)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            vm.error != null -> Box(Modifier.fillMaxSize().padding(inner)) {
                Text("오류: ${vm.error}")
            }
            vm.ui != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 단어 제목 카드
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Text(
                        vm.ui!!.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(20.dp)
                    )
                }
                // 영상 카드 (videoUrl 로 ExoPlayer 붙이면 실제 재생 가능)
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp)) {
                        Text("수어 영상", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(12.dp))
                        Icon(
                            Icons.Default.PlayCircleFilled,
                            contentDescription = "재생",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                // 설명 카드
                ElevatedCard(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp)) {
                        Text("상세 설명", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(8.dp))
                        Text(vm.ui!!.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
