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
 * curl "http://j13a602.p.ssafy.io/api/words/autocomplete?q=안"
 * # 검색
 * curl "http://j13a602.p.ssafy.io/api/words/search?q=안녕하세요"
 * # 상세
 * curl "http://j13a602.p.ssafy.io/api/words/101"
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel

// 키보드 액션/옵션 (IME Search)
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

// 개별 명시 임포트(가독용)
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton




// ───────────────────────────────────────────────────────────────────
// ❻ 검색 화면 UI
// ───────────────────────────────────────────────────────────────────

@Composable
fun SearchScreen(
    onOpenDetail: (Long) -> Unit, // 카드 클릭 시 상세로 이동하는 콜백 (NavGraph에서 전달)
) {
    // Hilt를 사용한 ViewModel 주입
    val vm: SearchViewModel = hiltViewModel()
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

        // 검색 입력창
        SearchTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            onSearch = { } // 디바운스로 자동 검색되므로 빈 함수
        )

        Spacer(Modifier.height(16.dp))

        // 상태에 따라 다른 UI를 보여줌
        when {
            // 아직 아무것도 안쳤거나, 검색하지 않은 상태
            state.query.isBlank() && !state.hasSearched -> EmptyState()

            // 네트워크 로딩 중
            state.isLoading -> LoadingState()

            // 에러가 났을 때
            state.error != null -> ErrorState(state.error!!)

            // 검색 결과가 없을 때
            state.results.isEmpty() && state.hasSearched -> {
                Box(Modifier.fillMaxSize()) { 
                    Text("검색 결과가 없습니다.", modifier = Modifier.align(Alignment.Center)) 
                }
            }

            // 검색 결과 표시
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


// 검색 결과 목록
@Composable
private fun ResultList(
    items: List<StudySearchItemDto>,
    onClick: (Long) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.wordId }) { item ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(item.wordId) },  // 카드 클릭 시 상세로 이동
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.word,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

